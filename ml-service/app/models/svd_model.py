"""
svd_model.py
~~~~~~~~~~~~
Wraps scikit-surprise SVD++ into a simple interface that the API layer can call.

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
from surprise import SVDpp, Dataset

from app.config import settings
from app.data.data_loader import (
    get_all_product_ids,
    get_all_user_ids,
    load_interactions_df,
    load_surprise_dataset,
)
from app.models.model_store import load_model, save_model

logger = logging.getLogger(__name__)


class SVDRecommender:
    """Singleton-style wrapper around a Surprise SVD++ model."""

    def __init__(self):
        self.model: Optional[SVDpp] = None
        self.meta: dict = {}
        self._product_ids: list[str] = []
        self._user_ids: list[str] = []
        # item latent factors cache (for item-item similarity)
        self._item_factors: Optional[np.ndarray] = None
        self._item_id_to_idx: dict[str, int] = {}

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

        dataset = load_surprise_dataset(df)
        trainset = dataset.build_full_trainset()

        algo = SVDpp(
            n_factors=settings.svd_n_factors,
            n_epochs=settings.svd_n_epochs,
            lr_all=settings.svd_lr_all,
            reg_all=settings.svd_reg_all,
            verbose=True,
        )
        algo.fit(trainset)

        elapsed = time.time() - t0

        # Persist
        meta = {
            "trained_at": datetime.now(timezone.utc).isoformat(),
            "n_users": trainset.n_users,
            "n_items": trainset.n_items,
            "n_ratings": trainset.n_ratings,
            "n_factors": settings.svd_n_factors,
            "n_epochs": settings.svd_n_epochs,
            "training_seconds": round(elapsed, 2),
        }
        save_model(algo, meta)

        # Update in-memory state
        self.model = algo
        self.meta = meta
        self._product_ids = get_all_product_ids()
        self._user_ids = get_all_user_ids()
        self._build_item_factors(trainset)

        logger.info("=== SVD++ Training done in %.1fs  |  %s ===", elapsed, meta)
        return meta

    # ── Loading ────────────────────────────────────────────

    def load(self) -> bool:
        """Load a previously trained model from disk. Returns True if loaded."""
        algo, meta = load_model()
        if algo is None:
            return False
        self.model = algo
        self.meta = meta
        self._product_ids = get_all_product_ids()
        self._user_ids = get_all_user_ids()
        # Rebuild item factors from loaded model
        if hasattr(algo, "qi") and algo.qi is not None:
            self._item_factors = algo.qi
            trainset = algo.trainset
            self._item_id_to_idx = {}
            for inner_id in range(trainset.n_items):
                raw_id = trainset.to_raw_iid(inner_id)
                self._item_id_to_idx[raw_id] = inner_id
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

        predictions = []
        for pid in self._product_ids:
            if pid in exclude_ids:
                continue
            pred = self.model.predict(user_id, pid)
            predictions.append((pid, pred.est))

        # Sort by predicted score descending
        predictions.sort(key=lambda x: x[1], reverse=True)

        results = []
        for rank, (pid, score) in enumerate(predictions[:n], start=1):
            results.append(
                {
                    "product_id": pid,
                    "score": round(score, 4),
                    "rank": rank,
                }
            )
        return results

    # ── Item-item similarity ───────────────────────────────

    def similar_products(self, product_id: str, n: int = 10) -> list[dict]:
        """
        Return top-N similar products based on cosine similarity of item
        latent factors learned by SVD++.
        """
        if self._item_factors is None or product_id not in self._item_id_to_idx:
            return []

        idx = self._item_id_to_idx[product_id]
        target_vec = self._item_factors[idx]
        target_norm = np.linalg.norm(target_vec)
        if target_norm == 0:
            return []

        similarities = []
        for raw_id, other_idx in self._item_id_to_idx.items():
            if raw_id == product_id:
                continue
            other_vec = self._item_factors[other_idx]
            other_norm = np.linalg.norm(other_vec)
            if other_norm == 0:
                continue
            cos_sim = float(np.dot(target_vec, other_vec) / (target_norm * other_norm))
            similarities.append((raw_id, cos_sim))

        similarities.sort(key=lambda x: x[1], reverse=True)

        return [
            {"product_id": pid, "similarity": round(sim, 4), "rank": rank}
            for rank, (pid, sim) in enumerate(similarities[:n], start=1)
        ]

    # ── Internal ───────────────────────────────────────────

    def _build_item_factors(self, trainset):
        """Cache the item latent-factor matrix for quick similarity lookups."""
        if hasattr(self.model, "qi") and self.model.qi is not None:
            self._item_factors = self.model.qi
            self._item_id_to_idx = {}
            for inner_id in range(trainset.n_items):
                raw_id = trainset.to_raw_iid(inner_id)
                self._item_id_to_idx[raw_id] = inner_id
            logger.info(
                "Item factor matrix cached: %d items × %d factors",
                self._item_factors.shape[0],
                self._item_factors.shape[1],
            )


# ── Module-level singleton ─────────────────────────────────
recommender = SVDRecommender()
