"""
svd_model.py
~~~~~~~~~~~~
Wraps the custom SVD++ implementation into a simple interface that the API layer can call.

SVD++ (Koren, 2008) extends SVD by incorporating implicit feedback:
    r̂_ui = μ + b_u + b_i + q_i^T(p_u + |N(u)|^{-0.5} * Σ y_j)

Key methods:
    train()                               – fit from MongoDB data
    recommend(user_id, n, exclude_ids)    – top-N for a user
    similar_products(product_id, n)       – item-item KNN from latent factors
"""

import logging
import time
from datetime import datetime, timezone
from typing import Optional

import numpy as np

from app.config import settings
from app.data.data_loader import (
    get_all_product_ids,
    get_all_user_ids,
    load_interactions_df,
)
from app.models.model_store import load_model, save_model
from app.models.svdpp import SVDpp, SVDppConfig

logger = logging.getLogger(__name__)


class SVDRecommender:
    """Wrapper around the custom SVD++ model."""

    def __init__(self):
        self.model: Optional[SVDpp] = None
        self.meta: dict = {}
        self._product_ids: list[str] = []
        self._user_ids: list[str] = []

    # ── Training ───────────────────────────────────────────

    def train(self) -> dict:
        """
        Train SVD++ on current MongoDB interaction data.

        Returns a summary dict with metrics and timing.
        """
        logger.info("=== SVD++ Training started ===")
        t0 = time.time()

        df = load_interactions_df()
        if df.empty:
            raise ValueError("No interaction data available for training.")

        # Prepare training data
        user_ids = df["user_id"].tolist()
        product_ids = df["product_id"].tolist()
        ratings = df["rating"].tolist()

        # Determine rating scale
        min_rating = float(df["rating"].min())
        max_rating = float(df["rating"].max())
        if min_rating == max_rating:
            max_rating = min_rating + 1.0

        # Create and train model
        config = SVDppConfig(
            n_factors=settings.svd_n_factors,
            n_epochs=settings.svd_n_epochs,
            lr=settings.svd_lr_all,
            reg=settings.svd_reg_all,
            min_rating=min_rating,
            max_rating=max_rating,
            verbose=True
        )

        model = SVDpp(config)
        model.fit(user_ids, product_ids, ratings)

        elapsed = time.time() - t0

        # Metadata
        meta = {
            "trained_at": datetime.now(timezone.utc).isoformat(),
            "n_users": model.n_users,
            "n_items": model.n_items,
            "n_interactions": len(ratings),
            "n_factors": settings.svd_n_factors,
            "n_epochs": settings.svd_n_epochs,
            "learning_rate": settings.svd_lr_all,
            "regularization": settings.svd_reg_all,
            "training_seconds": round(elapsed, 2),
            "algorithm": "SVD++",
            "rmse": round(float(model.last_epoch_rmse), 4) if model.last_epoch_rmse is not None else None,
        }

        # Save model
        save_model(model.get_params(), meta)

        # Update in-memory state
        self.model = model
        self.meta = meta
        self._product_ids = get_all_product_ids()
        self._user_ids = get_all_user_ids()

        logger.info("=== SVD++ Training done in %.1fs  |  %s ===", elapsed, meta)
        return meta

    # ── Loading ────────────────────────────────────────────

    def load(self) -> bool:
        """Load a previously trained model from disk. Returns True if loaded."""
        model_params, meta = load_model()
        if model_params is None:
            return False

        self.model = SVDpp()
        self.model.set_params(model_params)
        self.meta = meta
        self._product_ids = get_all_product_ids()
        self._user_ids = get_all_user_ids()

        logger.info("SVD++ model loaded, %d users, %d items",
                   self.model.n_users, self.model.n_items)
        return True

    # ── Recommendation ─────────────────────────────────────

    def recommend(
        self,
        user_id: str,
        n: int = 20,
        exclude_ids: Optional[set[str]] = None,
    ) -> list[dict]:
        """
        Return top-N recommendations for ``user_id``.

        Each entry: {"product_id": str, "score": float, "rank": int}
        """
        if self.model is None:
            raise RuntimeError("Model not trained. Call POST /train first.")

        if exclude_ids is None:
            exclude_ids = set()

        # Check if user exists in training data
        if user_id not in self.model.user_to_idx:
            # Cold-start user - return empty and let hybrid handle fallback
            logger.debug("User %s not in training data, returning empty", user_id)
            return []

        # Get recommendations from model
        recommendations = self.model.recommend(user_id, n=n + len(exclude_ids))

        # Filter excluded items and format results
        results = []
        for product_id, score in recommendations:
            if product_id not in exclude_ids:
                results.append({
                    "product_id": product_id,
                    "score": round(float(score), 4),
                    "rank": len(results) + 1,
                })
                if len(results) >= n:
                    break

        return results

    # ── Item-item similarity ───────────────────────────────

    def similar_products(self, product_id: str, n: int = 10) -> list[dict]:
        """
        Return top-N similar products based on cosine similarity of item
        latent factors learned by SVD++.
        """
        if self.model is None:
            return []

        if product_id not in self.model.item_to_idx:
            return []

        similar = self.model.similar_items(product_id, n=n)

        return [
            {
                "product_id": item_id,
                "similarity": round(float(sim), 4),
                "rank": rank + 1
            }
            for rank, (item_id, sim) in enumerate(similar)
        ]

    # ── Prediction ─────────────────────────────────────────

    def predict(self, user_id: str, product_id: str) -> float:
        """Predict rating for a user-item pair."""
        if self.model is None:
            raise RuntimeError("Model not trained. Call POST /train first.")
        return self.model.predict(user_id, product_id)

    # ── Item factors for external use ──────────────────────

    def get_item_factors(self) -> Optional[np.ndarray]:
        """Get the item factor matrix for similarity calculations."""
        if self.model is None:
            return None
        return self.model.qi

    def get_user_factors(self) -> Optional[np.ndarray]:
        """Get the user factor matrix."""
        if self.model is None:
            return None
        return self.model.pu

    # ── Mappings (for compatibility) ────────────────────────

    @property
    def user_to_idx(self) -> dict:
        """Get user to index mapping."""
        if self.model is None:
            return {}
        return self.model.user_to_idx

    @property
    def item_to_idx(self) -> dict:
        """Get item to index mapping."""
        if self.model is None:
            return {}
        return self.model.item_to_idx


# ── Module-level singleton ─────────────────────────────────
recommender = SVDRecommender()
