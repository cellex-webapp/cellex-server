"""
data_loader.py
~~~~~~~~~~~~~~
Reads user_interactions from MongoDB and converts to a Surprise-compatible
Dataset for model training.
"""

import logging
from typing import Optional

import pandas as pd
from pymongo import MongoClient
from surprise import Dataset, Reader

from app.config import settings

logger = logging.getLogger(__name__)


def _get_collection(collection_name: str):
    """Return a pymongo collection handle."""
    client = MongoClient(settings.mongo_uri)
    db = client[settings.mongo_db]
    return db[collection_name]


def load_interactions_df() -> pd.DataFrame:
    """
    Load user_interactions from MongoDB and return a long-form DataFrame:

        user_id | product_id | rating

    ``rating`` is the ``total_score`` field which is the pre-computed weighted
    score  (view*1 + cart*3 + purchase*5 + review*4).

    Rows with total_score <= 0 are dropped.
    """
    col = _get_collection("user_interactions")
    cursor = col.find(
        {"total_score": {"$gt": 0}},
        {"_id": 0, "user_id": 1, "product_id": 1, "total_score": 1},
    )
    rows = list(cursor)

    if not rows:
        logger.warning("No interactions found in MongoDB.")
        return pd.DataFrame(columns=["user_id", "product_id", "rating"])

    df = pd.DataFrame(rows)
    df.rename(columns={"total_score": "rating"}, inplace=True)

    logger.info(
        "Loaded %d interactions  |  %d users  |  %d products",
        len(df),
        df["user_id"].nunique(),
        df["product_id"].nunique(),
    )
    return df


def load_surprise_dataset(df: Optional[pd.DataFrame] = None) -> Dataset:
    """
    Convert the interactions DataFrame to a Surprise Dataset.

    ``total_score`` is used as the implicit rating.
    The rating scale is [1, max_score] — Surprise needs explicit bounds.
    """
    if df is None:
        df = load_interactions_df()

    if df.empty:
        raise ValueError("Cannot build Surprise Dataset: no interaction data.")

    min_rating = float(df["rating"].min())
    max_rating = float(df["rating"].max())

    # Avoid collapsed scale if all scores are identical
    if min_rating == max_rating:
        max_rating = min_rating + 1.0

    reader = Reader(rating_scale=(min_rating, max_rating))
    dataset = Dataset.load_from_df(df[["user_id", "product_id", "rating"]], reader)
    return dataset


def load_products_map() -> dict:
    """
    Return {product_id: {name, categoryId, price, ...}} from the products
    collection. Used for enriching API responses.
    """
    col = _get_collection("products")
    products = {}
    for doc in col.find({"isPublished": True}):
        products[str(doc["_id"])] = {
            "name": doc.get("name", ""),
            "categoryId": doc.get("categoryId", ""),
            "price": doc.get("price", 0),
            "finalPrice": doc.get("finalPrice", 0),
            "averageRating": doc.get("averageRating", 0),
            "reviewCount": doc.get("reviewCount", 0),
            "images": doc.get("images", []),
        }
    return products


def get_all_product_ids() -> list[str]:
    """Return a list of all published product IDs."""
    col = _get_collection("products")
    return [str(doc["_id"]) for doc in col.find({"isPublished": True}, {"_id": 1})]


def get_all_user_ids() -> list[str]:
    """Return distinct user IDs that have at least one interaction."""
    col = _get_collection("user_interactions")
    return col.distinct("user_id")
