"""
test_services.py
~~~~~~~~~~~~~~~~
Tests for service classes.
"""

import pytest
from unittest.mock import patch, MagicMock
from datetime import datetime, timedelta


class TestPopularityService:
    """Test cases for PopularityService."""

    def test_init(self):
        """Test PopularityService initialization."""
        from app.services.popularity_service import PopularityService

        service = PopularityService()

        assert service._popularity_cache == {}
        assert service._cache_time is None

    @patch("app.services.popularity_service.MongoClient")
    def test_compute_popularity(self, mock_client):
        """Test computing popularity scores."""
        from app.services.popularity_service import PopularityService

        # Mock MongoDB aggregation results
        mock_db = MagicMock()
        mock_client.return_value.__getitem__.return_value = mock_db
        mock_db["user_interactions"].aggregate.return_value = [
            {
                "_id": "prod_1",
                "total_score_sum": 100,
                "user_count": 10,
                "interaction_count": 20,
                "last_interaction": datetime.utcnow(),
                "view_count": 50,
                "cart_count": 10,
                "purchase_count": 5,
                "review_count": 2
            },
            {
                "_id": "prod_2",
                "total_score_sum": 50,
                "user_count": 5,
                "interaction_count": 10,
                "last_interaction": datetime.utcnow() - timedelta(days=5),
                "view_count": 30,
                "cart_count": 5,
                "purchase_count": 2,
                "review_count": 1
            }
        ]

        service = PopularityService()
        scores = service.compute_popularity(force_refresh=True)

        assert len(scores) == 2
        assert "prod_1" in scores
        assert "prod_2" in scores
        # First product should have higher score
        assert scores["prod_1"] >= scores["prod_2"]

    @patch("app.services.popularity_service.MongoClient")
    def test_compute_popularity_caching(self, mock_client):
        """Test that popularity scores are cached."""
        from app.services.popularity_service import PopularityService

        mock_db = MagicMock()
        mock_client.return_value.__getitem__.return_value = mock_db
        mock_db["user_interactions"].aggregate.return_value = []

        service = PopularityService()

        # First call
        service.compute_popularity()

        # Second call should use cache
        service.compute_popularity()

        # Aggregate should only be called once
        assert mock_db["user_interactions"].aggregate.call_count == 1

    @patch("app.services.popularity_service.MongoClient")
    def test_get_trending(self, mock_client):
        """Test getting trending products."""
        from app.services.popularity_service import PopularityService

        mock_db = MagicMock()
        mock_client.return_value.__getitem__.return_value = mock_db
        mock_db["user_interactions"].aggregate.return_value = [
            {
                "_id": "prod_1",
                "total_score_sum": 100,
                "user_count": 10,
                "interaction_count": 20,
                "last_interaction": datetime.utcnow(),
                "purchase_count": 5
            }
        ]

        service = PopularityService()
        trending = service.get_trending(n=10)

        assert len(trending) == 1
        assert trending[0]["product_id"] == "prod_1"
        assert trending[0]["recommendation_reason"] == "TRENDING"

    @patch("app.services.popularity_service.MongoClient")
    def test_get_latest_products(self, mock_client):
        """Test getting latest products."""
        from app.services.popularity_service import PopularityService

        mock_db = MagicMock()
        mock_client.return_value.__getitem__.return_value = mock_db
        mock_db["products"].find.return_value.sort.return_value.limit.return_value = [
            {"_id": "prod_1"},
            {"_id": "prod_2"}
        ]

        service = PopularityService()
        latest = service.get_latest_products(n=5)

        assert len(latest) == 2
        assert latest[0]["recommendation_reason"] == "LATEST"


class TestHybridRecommender:
    """Test cases for HybridRecommender."""

    def test_init(self):
        """Test HybridRecommender initialization."""
        from app.services.hybrid_recommender import HybridRecommender

        recommender = HybridRecommender()
        assert recommender._user_interaction_counts == {}

    @patch("app.services.hybrid_recommender.hybrid_recommender")
    def test_is_cold_start_user(self, mock_hybrid):
        """Test cold start user detection."""
        from app.services.hybrid_recommender import HybridRecommender

        with patch("app.data.data_loader._get_collection") as mock_col:
            mock_col.return_value.count_documents.return_value = 1

            recommender = HybridRecommender()
            is_cold = recommender.is_cold_start_user("new_user")

            # With min_interactions=3, user with 1 interaction is cold start
            assert is_cold is True

    @patch("app.services.hybrid_recommender.hybrid_recommender")
    def test_is_not_cold_start_user(self, mock_hybrid):
        """Test non-cold start user detection."""
        from app.services.hybrid_recommender import HybridRecommender

        with patch("app.data.data_loader._get_collection") as mock_col:
            mock_col.return_value.count_documents.return_value = 10

            recommender = HybridRecommender()
            is_cold = recommender.is_cold_start_user("active_user")

            assert is_cold is False

    def test_clear_user_cache(self):
        """Test clearing user cache."""
        from app.services.hybrid_recommender import HybridRecommender

        recommender = HybridRecommender()
        recommender._user_interaction_counts = {"user_1": 5, "user_2": 10}

        # Clear specific user
        recommender.clear_user_cache("user_1")
        assert "user_1" not in recommender._user_interaction_counts
        assert "user_2" in recommender._user_interaction_counts

        # Clear all
        recommender.clear_user_cache()
        assert recommender._user_interaction_counts == {}

    def test_get_explanation(self):
        """Test explanation generation."""
        from app.services.hybrid_recommender import HybridRecommender

        recommender = HybridRecommender()

        assert "SVD++" in recommender._get_explanation("ML_SVD_PLUS_PLUS")
        assert "hot" in recommender._get_explanation("TRENDING")
        assert "moi" in recommender._get_explanation("COLD_START_TRENDING")
