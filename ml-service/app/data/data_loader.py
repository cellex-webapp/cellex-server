"""
data_loader.py
~~~~~~~~~~~~~~
Reads user_interactions from MongoDB and converts to sparse matrices
for the implicit ALS model training.
"""

import logging
from typing import Optional, Tuple

import numpy as np
import pandas as pd
from scipy import sparse
from pymongo import MongoClient

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
    score (view*1 + cart*3 + purchase*5 + review*4).

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


def build_sparse_matrix(
    df: Optional[pd.DataFrame] = None
) -> Tuple[sparse.csr_matrix, dict, dict, dict, dict]:
    """
    Build a sparse user-item interaction matrix for the implicit library.

    Returns:
        user_item_matrix: CSR sparse matrix (users x items)
        user_to_idx: Mapping from user_id to matrix row index
        idx_to_user: Reverse mapping
        item_to_idx: Mapping from product_id to matrix column index
        idx_to_item: Reverse mapping
    """
    if df is None:
        df = load_interactions_df()

    if df.empty:
        raise ValueError("Cannot build sparse matrix: no interaction data.")

    # Create mappings
    unique_users = df["user_id"].unique()
    unique_items = df["product_id"].unique()

    user_to_idx = {user: idx for idx, user in enumerate(unique_users)}
    idx_to_user = {idx: user for user, idx in user_to_idx.items()}
    item_to_idx = {item: idx for idx, item in enumerate(unique_items)}
    idx_to_item = {idx: item for item, idx in item_to_idx.items()}

    # Build sparse matrix
    row_indices = df["user_id"].map(user_to_idx).values
    col_indices = df["product_id"].map(item_to_idx).values
    values = df["rating"].values.astype(np.float32)

    user_item_matrix = sparse.csr_matrix(
        (values, (row_indices, col_indices)),
        shape=(len(unique_users), len(unique_items))
    )

    logger.info(
        "Built sparse matrix: %d users x %d items, %d interactions (density: %.4f%%)",
        user_item_matrix.shape[0],
        user_item_matrix.shape[1],
        user_item_matrix.nnz,
        100 * user_item_matrix.nnz / (user_item_matrix.shape[0] * user_item_matrix.shape[1])
    )

    return user_item_matrix, user_to_idx, idx_to_user, item_to_idx, idx_to_item


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
