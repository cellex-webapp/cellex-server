"""
test_svd_model.py
~~~~~~~~~~~~~~~~~
Tests for the SVD++ recommendation model.
"""

import pytest
from unittest.mock import patch, MagicMock
import numpy as np


class TestSVDppModel:
    """Test cases for the core SVDpp class."""

    def test_init(self):
        """Test SVDpp initialization."""
        from app.models.svdpp import SVDpp, SVDppConfig

        config = SVDppConfig(n_factors=20, n_epochs=10)
        model = SVDpp(config)

        assert model.config.n_factors == 20
        assert model.config.n_epochs == 10
        assert not model.is_fitted

    def test_fit(self, sample_training_data):
        """Test SVDpp training."""
        from app.models.svdpp import SVDpp, SVDppConfig

        user_ids, item_ids, ratings = sample_training_data

        config = SVDppConfig(n_factors=10, n_epochs=5, verbose=False)
        model = SVDpp(config)
        model.fit(user_ids, item_ids, ratings)

        assert model.is_fitted
        assert model.n_users == 4
        assert model.n_items == 3
        assert model.pu.shape == (4, 10)
        assert model.qi.shape == (3, 10)
        assert model.yj.shape == (3, 10)

    def test_predict(self, sample_svdpp_model):
        """Test prediction for known user-item pair."""
        pred = sample_svdpp_model.predict("u1", "p1")

        assert isinstance(pred, float)
        assert 1.0 <= pred <= 5.0

    def test_predict_unknown_user(self, sample_svdpp_model):
        """Test prediction for unknown user returns global mean."""
        pred = sample_svdpp_model.predict("unknown_user", "p1")

        assert pred == sample_svdpp_model.global_mean

    def test_recommend(self, sample_svdpp_model):
        """Test top-N recommendations."""
        recommendations = sample_svdpp_model.recommend("u1", n=2)

        assert len(recommendations) <= 2
        assert all(isinstance(item_id, str) for item_id, _ in recommendations)
        assert all(isinstance(score, float) for _, score in recommendations)

    def test_recommend_unknown_user(self, sample_svdpp_model):
        """Test recommendations for unknown user returns empty."""
        recommendations = sample_svdpp_model.recommend("unknown_user", n=5)

        assert recommendations == []

    def test_similar_items(self, sample_svdpp_model):
        """Test similar items."""
        similar = sample_svdpp_model.similar_items("p1", n=2)

        assert len(similar) <= 2
        # Should not include the query item
        assert all(item_id != "p1" for item_id, _ in similar)

    def test_get_set_params(self, sample_svdpp_model):
        """Test parameter serialization."""
        from app.models.svdpp import SVDpp

        params = sample_svdpp_model.get_params()

        new_model = SVDpp()
        new_model.set_params(params)

        assert new_model.is_fitted
        assert new_model.n_users == sample_svdpp_model.n_users
        assert new_model.n_items == sample_svdpp_model.n_items


class TestSVDRecommender:
    """Test cases for SVDRecommender wrapper class."""

    def test_init(self):
        """Test SVDRecommender initialization."""
        from app.models.svd_model import SVDRecommender

        recommender = SVDRecommender()

        assert recommender.model is None
        assert recommender.meta == {}
        assert recommender._product_ids == []
        assert recommender._user_ids == []

    @patch("app.models.svd_model.load_interactions_df")
    @patch("app.models.svd_model.get_all_product_ids")
    @patch("app.models.svd_model.get_all_user_ids")
    @patch("app.models.svd_model.save_model")
    def test_train_success(
        self,
        mock_save,
        mock_user_ids,
        mock_product_ids,
        mock_df,
        mock_interactions_df
    ):
        """Test successful model training."""
        from app.models.svd_model import SVDRecommender

        mock_df.return_value = mock_interactions_df
        mock_product_ids.return_value = ["prod_1", "prod_2", "prod_3"]
        mock_user_ids.return_value = ["user_1", "user_2", "user_3", "user_4"]

        recommender = SVDRecommender()
        meta = recommender.train()

        assert recommender.model is not None
        assert "trained_at" in meta
        assert "n_users" in meta
        assert "n_items" in meta
        assert meta["algorithm"] == "SVD++"
        mock_save.assert_called_once()

    @patch("app.models.svd_model.load_interactions_df")
    def test_train_no_data(self, mock_df):
        """Test training with no data raises error."""
        import pandas as pd
        from app.models.svd_model import SVDRecommender

        mock_df.return_value = pd.DataFrame(columns=["user_id", "product_id", "rating"])

        recommender = SVDRecommender()

        with pytest.raises(ValueError, match="No interaction data"):
            recommender.train()

    def test_recommend_no_model(self):
        """Test recommend without trained model raises error."""
        from app.models.svd_model import SVDRecommender

        recommender = SVDRecommender()

        with pytest.raises(RuntimeError, match="Model not trained"):
            recommender.recommend("user_1")

    @patch("app.models.svd_model.load_interactions_df")
    @patch("app.models.svd_model.get_all_product_ids")
    @patch("app.models.svd_model.get_all_user_ids")
    @patch("app.models.svd_model.save_model")
    def test_recommend_with_model(
        self,
        mock_save,
        mock_user_ids,
        mock_product_ids,
        mock_df,
        mock_interactions_df
    ):
        """Test recommendations with trained model."""
        from app.models.svd_model import SVDRecommender

        mock_df.return_value = mock_interactions_df
        mock_product_ids.return_value = ["prod_1", "prod_2", "prod_3"]
        mock_user_ids.return_value = ["user_1", "user_2", "user_3", "user_4"]

        recommender = SVDRecommender()
        recommender.train()

        results = recommender.recommend("user_1", n=2)

        assert isinstance(results, list)
        if results:
            assert all("product_id" in r for r in results)
            assert all("score" in r for r in results)
            assert all("rank" in r for r in results)

    def test_similar_products_no_model(self):
        """Test similar products without trained model returns empty."""
        from app.models.svd_model import SVDRecommender

        recommender = SVDRecommender()
        results = recommender.similar_products("prod_1")

        assert results == []

    @patch("app.models.svd_model.load_model")
    def test_load_no_model(self, mock_load):
        """Test load when no model exists."""
        from app.models.svd_model import SVDRecommender

        mock_load.return_value = (None, None)

        recommender = SVDRecommender()
        result = recommender.load()

        assert result is False
        assert recommender.model is None

    @patch("app.models.svd_model.load_model")
    @patch("app.models.svd_model.get_all_product_ids")
    @patch("app.models.svd_model.get_all_user_ids")
    def test_load_with_model(
        self,
        mock_user_ids,
        mock_product_ids,
        mock_load,
        sample_svdpp_model
    ):
        """Test load when model exists."""
        from app.models.svd_model import SVDRecommender

        mock_load.return_value = (
            sample_svdpp_model.get_params(),
            {"trained_at": "2024-01-01", "algorithm": "SVD++"}
        )
        mock_product_ids.return_value = ["p1", "p2", "p3"]
        mock_user_ids.return_value = ["u1", "u2", "u3", "u4"]

        recommender = SVDRecommender()
        result = recommender.load()

        assert result is True
        assert recommender.model is not None
        assert recommender.meta["algorithm"] == "SVD++"

    def test_get_item_factors_no_model(self):
        """Test getting item factors without model."""
        from app.models.svd_model import SVDRecommender

        recommender = SVDRecommender()
        factors = recommender.get_item_factors()

        assert factors is None

    @patch("app.models.svd_model.load_interactions_df")
    @patch("app.models.svd_model.get_all_product_ids")
    @patch("app.models.svd_model.get_all_user_ids")
    @patch("app.models.svd_model.save_model")
    def test_get_item_factors(
        self,
        mock_save,
        mock_user_ids,
        mock_product_ids,
        mock_df,
        mock_interactions_df
    ):
        """Test getting item factors with trained model."""
        from app.models.svd_model import SVDRecommender

        mock_df.return_value = mock_interactions_df
        mock_product_ids.return_value = ["prod_1", "prod_2", "prod_3"]
        mock_user_ids.return_value = ["user_1", "user_2", "user_3", "user_4"]

        recommender = SVDRecommender()
        recommender.train()

        factors = recommender.get_item_factors()

        assert factors is not None
        assert isinstance(factors, np.ndarray)
