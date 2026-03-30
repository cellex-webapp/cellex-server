"""
test_routes.py
~~~~~~~~~~~~~~
Tests for the FastAPI routes.
"""

import pytest
from unittest.mock import patch, MagicMock
from fastapi.testclient import TestClient


@pytest.fixture
def client():
    """Create FastAPI test client."""
    from app.main import app
    return TestClient(app)


class TestHealthEndpoint:
    """Test /health endpoint."""

    def test_health_no_model(self, client):
        """Test health check when no model loaded."""
        with patch("app.main.recommender") as mock_rec:
            mock_rec.model = None

            response = client.get("/health")

            assert response.status_code == 200
            data = response.json()
            assert data["status"] == "ok"
            assert data["model_loaded"] is False

    def test_health_with_model(self, client):
        """Test health check when model is loaded."""
        with patch("app.main.recommender") as mock_rec:
            mock_rec.model = MagicMock()

            response = client.get("/health")

            assert response.status_code == 200
            data = response.json()
            assert data["status"] == "ok"


class TestTrainEndpoint:
    """Test /api/v1/ml/train endpoint."""

    @patch("app.api.routes.recommender")
    def test_train_success(self, mock_rec, client):
        """Test successful training."""
        mock_rec.train.return_value = {
            "trained_at": "2024-01-01T00:00:00",
            "n_users": 100,
            "n_items": 50
        }

        response = client.post("/api/v1/ml/train")

        assert response.status_code == 200
        data = response.json()
        assert data["message"] == "Training completed successfully."
        assert "meta" in data

    @patch("app.api.routes.recommender")
    def test_train_no_data(self, mock_rec, client):
        """Test training with no data."""
        mock_rec.train.side_effect = ValueError("No interaction data")

        response = client.post("/api/v1/ml/train")

        assert response.status_code == 400
        assert "No interaction data" in response.json()["detail"]


class TestRecommendationsEndpoint:
    """Test /api/v1/ml/recommendations/{user_id} endpoint."""

    @patch("app.api.routes.load_products_map")
    @patch("app.api.routes.recommender")
    def test_recommendations_success(self, mock_rec, mock_products, client, mock_products_map):
        """Test successful recommendations."""
        mock_rec.model = MagicMock()
        mock_rec.recommend.return_value = [
            {"product_id": "prod_1", "score": 4.5, "rank": 1},
            {"product_id": "prod_2", "score": 4.2, "rank": 2}
        ]
        mock_products.return_value = mock_products_map

        response = client.get("/api/v1/ml/recommendations/user_1?limit=10")

        assert response.status_code == 200
        data = response.json()
        assert len(data) == 2
        assert data[0]["product_id"] == "prod_1"
        assert "product_name" in data[0]

    @patch("app.api.routes.recommender")
    def test_recommendations_no_model(self, mock_rec, client):
        """Test recommendations when model not trained."""
        mock_rec.model = None

        response = client.get("/api/v1/ml/recommendations/user_1")

        assert response.status_code == 503
        assert "Model not trained" in response.json()["detail"]


class TestHybridEndpoint:
    """Test /api/v1/ml/hybrid/{user_id} endpoint."""

    @patch("app.api.routes.hybrid_recommender")
    def test_hybrid_recommendations(self, mock_hybrid, client, mock_products_map):
        """Test hybrid recommendations."""
        mock_hybrid.recommend.return_value = [
            {
                "product_id": "prod_1",
                "product_name": "iPhone 15 Pro",
                "score": 0.85,
                "rank": 1,
                "recommendation_reason": "HYBRID_SVD_POPULARITY",
                "explanation": "Test explanation",
                "svd_score": 0.9,
                "popularity_score": 0.7
            }
        ]

        response = client.get("/api/v1/ml/hybrid/user_1?limit=10")

        assert response.status_code == 200
        data = response.json()
        assert len(data) == 1
        assert data[0]["recommendation_reason"] == "HYBRID_SVD_POPULARITY"


class TestTrendingEndpoint:
    """Test /api/v1/ml/trending endpoint."""

    @patch("app.api.routes.load_products_map")
    @patch("app.api.routes.popularity_service")
    def test_trending_products(self, mock_pop, mock_products, client, mock_products_map):
        """Test trending products."""
        mock_pop.get_trending.return_value = [
            {"product_id": "prod_1", "popularity_score": 0.95, "rank": 1}
        ]
        mock_products.return_value = mock_products_map

        response = client.get("/api/v1/ml/trending?limit=10")

        assert response.status_code == 200
        data = response.json()
        assert len(data) == 1
        assert data[0]["product_id"] == "prod_1"


class TestMockDataEndpoint:
    """Test /api/v1/ml/mock-data endpoint."""

    @patch("app.api.routes.generate_mock_data")
    def test_generate_mock_data(self, mock_gen, client):
        """Test mock data generation."""
        mock_gen.return_value = {
            "total_interactions": 1000,
            "unique_users": 100,
            "unique_products": 50
        }

        response = client.post("/api/v1/ml/mock-data?n_users=100&density=0.1")

        assert response.status_code == 200
        data = response.json()
        assert data["message"] == "Mock data generated."
        assert "summary" in data


class TestStatsEndpoint:
    """Test /api/v1/ml/stats endpoint."""

    @patch("app.api.routes.load_interactions_df")
    def test_stats(self, mock_df, client, mock_interactions_df):
        """Test data statistics."""
        mock_df.return_value = mock_interactions_df

        response = client.get("/api/v1/ml/stats")

        assert response.status_code == 200
        data = response.json()
        assert "total_interactions" in data
        assert "unique_users" in data

    @patch("app.api.routes.load_interactions_df")
    def test_stats_empty(self, mock_df, client):
        """Test stats with empty data."""
        import pandas as pd
        mock_df.return_value = pd.DataFrame()

        response = client.get("/api/v1/ml/stats")

        assert response.status_code == 200
        data = response.json()
        assert data["total_interactions"] == 0
