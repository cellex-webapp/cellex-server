"""
popularity_service.py
~~~~~~~~~~~~~~~~~~~~~
Handles trending/popular product recommendations.

Uses time-decayed interaction scores to compute product popularity.
Useful as a fallback for cold-start users and for diversification.
"""

import logging
import math
from datetime import datetime, timedelta
from typing import Optional

from pymongo import MongoClient

from app.config import settings

logger = logging.getLogger(__name__)


class PopularityService:
    """
    Service for computing and serving popularity-based recommendations.

    Popularity is computed using:
        - Total interaction score (view * 1 + cart * 3 + purchase * 5 + review * 4)
        - Time decay factor (recent interactions weighted more)
        - Number of unique users who interacted
    """

    def __init__(self):
        self._popularity_cache: dict[str, float] = {}
        self._cache_time: Optional[datetime] = None
        self._cache_ttl = timedelta(hours=1)

    def _get_db(self):
        client = MongoClient(settings.mongo_uri)
        return client[settings.mongo_db]

    def compute_popularity(self, force_refresh: bool = False) -> dict[str, float]:
        """
        Compute popularity scores for all products.

        Returns:
            dict mapping product_id to popularity score
        """
        # Check cache
        if not force_refresh and self._cache_time:
            if datetime.now() - self._cache_time < self._cache_ttl:
                return self._popularity_cache

        logger.info("Computing product popularity scores...")

        db = self._get_db()
        col = db["user_interactions"]

        # Get interactions from the trending window
        cutoff_date = datetime.utcnow() - timedelta(days=settings.trending_window_days)

        # Aggregate by product
        pipeline = [
            {
                "$match": {
                    "updated_at": {"$gte": cutoff_date},
                    "total_score": {"$gt": 0}
                }
            },
            {
                "$group": {
                    "_id": "$product_id",
                    "total_score_sum": {"$sum": "$total_score"},
                    "unique_users": {"$addToSet": "$user_id"},
                    "interaction_count": {"$sum": 1},
                    "last_interaction": {"$max": "$updated_at"},
                    "view_count": {"$sum": "$view_count"},
                    "cart_count": {"$sum": "$cart_count"},
                    "purchase_count": {"$sum": "$purchase_count"},
                    "review_count": {"$sum": "$review_count"}
                }
            },
            {
                "$project": {
                    "_id": 1,
                    "total_score_sum": 1,
                    "user_count": {"$size": "$unique_users"},
                    "interaction_count": 1,
                    "last_interaction": 1,
                    "view_count": 1,
                    "cart_count": 1,
                    "purchase_count": 1,
                    "review_count": 1
                }
            }
        ]

        results = list(col.aggregate(pipeline))

        now = datetime.utcnow()
        popularity_scores = {}

        for doc in results:
            product_id = doc["_id"]

            # Skip if not enough interactions
            if doc["interaction_count"] < settings.min_interactions_for_trending:
                continue

            # Base score from interactions
            base_score = doc["total_score_sum"]

            # User diversity bonus (more unique users = more popular)
            user_bonus = math.log1p(doc["user_count"]) * 2

            # Time decay
            if doc["last_interaction"]:
                days_ago = (now - doc["last_interaction"]).days
                decay = settings.trending_decay_factor ** days_ago
            else:
                decay = 0.5

            # Purchase boost (purchased items are more trustworthy)
            purchase_boost = 1 + (doc.get("purchase_count", 0) * 0.1)

            # Final score
            popularity_scores[product_id] = base_score * decay * purchase_boost + user_bonus

        # Normalize scores to 0-1 range
        if popularity_scores:
            max_score = max(popularity_scores.values())
            if max_score > 0:
                popularity_scores = {
                    pid: score / max_score
                    for pid, score in popularity_scores.items()
                }

        self._popularity_cache = popularity_scores
        self._cache_time = datetime.now()

        logger.info("Computed popularity for %d products", len(popularity_scores))
        return popularity_scores

    def get_trending(
        self,
        n: int = 20,
        category_id: Optional[str] = None,
        exclude_ids: Optional[set[str]] = None
    ) -> list[dict]:
        """
        Get top-N trending/popular products.

        Args:
            n: Number of products to return
            category_id: Optional category filter
            exclude_ids: Product IDs to exclude

        Returns:
            List of dicts with product_id, popularity_score, rank
        """
        if exclude_ids is None:
            exclude_ids = set()

        popularity_scores = self.compute_popularity()

        # Filter by category if specified
        if category_id:
            db = self._get_db()
            products_col = db["products"]
            category_products = set(
                str(doc["_id"])
                for doc in products_col.find(
                    {"categoryId": category_id, "isPublished": True},
                    {"_id": 1}
                )
            )
            popularity_scores = {
                pid: score
                for pid, score in popularity_scores.items()
                if pid in category_products
            }

        # Exclude specified products
        popularity_scores = {
            pid: score
            for pid, score in popularity_scores.items()
            if pid not in exclude_ids
        }

        # Sort by popularity
        sorted_products = sorted(
            popularity_scores.items(),
            key=lambda x: x[1],
            reverse=True
        )[:n]

        return [
            {
                "product_id": pid,
                "popularity_score": round(score, 4),
                "rank": rank,
                "recommendation_reason": "TRENDING"
            }
            for rank, (pid, score) in enumerate(sorted_products, start=1)
        ]

    def get_latest_products(
        self,
        n: int = 20,
        category_id: Optional[str] = None,
        exclude_ids: Optional[set[str]] = None
    ) -> list[dict]:
        """
        Get latest published products as fallback.

        Args:
            n: Number of products to return
            category_id: Optional category filter
            exclude_ids: Product IDs to exclude

        Returns:
            List of dicts with product_id, rank
        """
        if exclude_ids is None:
            exclude_ids = set()

        db = self._get_db()
        products_col = db["products"]

        query = {"isPublished": True}
        if category_id:
            query["categoryId"] = category_id

        cursor = products_col.find(query).sort("createdAt", -1).limit(n * 2)

        results = []
        for doc in cursor:
            pid = str(doc["_id"])
            if pid not in exclude_ids:
                results.append({
                    "product_id": pid,
                    "rank": len(results) + 1,
                    "recommendation_reason": "LATEST"
                })
                if len(results) >= n:
                    break

        return results

    def get_stats(self) -> dict:
        """Return statistics about the popularity cache."""
        popularity_scores = self.compute_popularity()

        if not popularity_scores:
            return {
                "total_products": 0,
                "cache_age_seconds": None,
                "cache_ttl_seconds": self._cache_ttl.total_seconds()
            }

        scores = list(popularity_scores.values())
        cache_age = None
        if self._cache_time:
            cache_age = (datetime.now() - self._cache_time).total_seconds()

        return {
            "total_products": len(popularity_scores),
            "avg_score": round(sum(scores) / len(scores), 4),
            "max_score": round(max(scores), 4),
            "min_score": round(min(scores), 4),
            "cache_age_seconds": cache_age,
            "cache_ttl_seconds": self._cache_ttl.total_seconds()
        }


# Module-level singleton
popularity_service = PopularityService()
