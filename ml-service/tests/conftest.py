"""
conftest.py
~~~~~~~~~~~
Pytest fixtures for ML Service tests.
"""

import os
import sys
import pytest
import numpy as np
import pandas as pd
from unittest.mock import MagicMock, patch

# Add app to path
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))


@pytest.fixture
def mock_interactions_df():
    """Sample interaction DataFrame for testing."""
    return pd.DataFrame({
        "user_id": ["user_1", "user_1", "user_2", "user_2", "user_3", "user_3", "user_4"],
        "product_id": ["prod_1", "prod_2", "prod_1", "prod_3", "prod_2", "prod_3", "prod_1"],
        "rating": [5.0, 3.0, 4.0, 2.0, 5.0, 4.0, 3.0]
    })


@pytest.fixture
def mock_products_map():
    """Sample products map for testing."""
    return {
        "prod_1": {
            "name": "iPhone 15 Pro",
            "categoryId": "cat_phones",
            "price": 25000000,
            "finalPrice": 24000000,
            "averageRating": 4.8,
            "reviewCount": 150,
            "images": ["https://example.com/iphone.jpg"]
        },
        "prod_2": {
            "name": "MacBook Pro M3",
            "categoryId": "cat_laptops",
            "price": 45000000,
            "finalPrice": 43000000,
            "averageRating": 4.9,
            "reviewCount": 80,
            "images": ["https://example.com/macbook.jpg"]
        },
        "prod_3": {
            "name": "AirPods Pro 2",
            "categoryId": "cat_accessories",
            "price": 6500000,
            "finalPrice": 6000000,
            "averageRating": 4.7,
            "reviewCount": 200,
            "images": ["https://example.com/airpods.jpg"]
        },
        "prod_4": {
            "name": "Apple Watch Series 9",
            "categoryId": "cat_watches",
            "price": 12000000,
            "finalPrice": 11500000,
            "averageRating": 4.6,
            "reviewCount": 120,
            "images": []
        }
    }


@pytest.fixture
def mock_mongodb():
    """Mock MongoDB connection."""
    with patch("pymongo.MongoClient") as mock_client:
        mock_db = MagicMock()
        mock_client.return_value.__getitem__.return_value = mock_db
        yield mock_db


@pytest.fixture
def sample_training_data():
    """Sample training data for SVD++ tests."""
    user_ids = ["u1", "u1", "u2", "u2", "u3", "u3", "u4", "u4"]
    item_ids = ["p1", "p2", "p1", "p3", "p2", "p3", "p1", "p2"]
    ratings = [5.0, 3.0, 4.0, 2.0, 5.0, 4.0, 3.0, 4.0]
    return user_ids, item_ids, ratings


@pytest.fixture
def sample_svdpp_model(sample_training_data):
    """Create a simple trained SVD++ model for testing."""
    from app.models.svdpp import SVDpp, SVDppConfig

    user_ids, item_ids, ratings = sample_training_data

    config = SVDppConfig(
        n_factors=10,
        n_epochs=5,
        lr=0.01,
        reg=0.02,
        min_rating=1.0,
        max_rating=5.0,
        verbose=False
    )

    model = SVDpp(config)
    model.fit(user_ids, item_ids, ratings)

    return model
