"""
hybrid_recommender.py
~~~~~~~~~~~~~~~~~~~~
Hybrid recommendation engine that combines multiple strategies:

1. SVD++ (primary ML-based recommendations)
2. Popularity/Trending (for diversification and cold-start)
3. Latest products (fallback)

Implements the fallback chain defined in AI_SCOPE:
    SVD++ -> CF (via Java backend) -> Content-based -> Trending -> Latest
"""

import logging
from typing import Optional

from app.config import settings
from app.models.svd_model import recommender
from app.services.popularity_service import popularity_service
from app.data.data_loader import load_products_map, get_all_product_ids

logger = logging.getLogger(__name__)


class HybridRecommender:
    """
    Combines SVD++ with popularity-based recommendations.

    The hybrid score is computed as:
        hybrid_score = ml_weight * svd_score + pop_weight * popularity_score + recency_weight * recency_score

    For cold-start users (not in training data), falls back to:
        1. Popularity-based recommendations
        2. Latest products
    """

    def __init__(self):
        self._user_interaction_counts: dict[str, int] = {}

    def get_user_interaction_count(self, user_id: str) -> int:
        """Get cached interaction count for a user."""
        if user_id not in self._user_interaction_counts:
            from app.data.data_loader import _get_collection
            col = _get_collection("user_interactions")
            count = col.count_documents({"user_id": user_id})
            self._user_interaction_counts[user_id] = count
        return self._user_interaction_counts[user_id]

    def clear_user_cache(self, user_id: Optional[str] = None):
        """Clear user interaction count cache."""
        if user_id:
            self._user_interaction_counts.pop(user_id, None)
        else:
            self._user_interaction_counts.clear()

    def is_cold_start_user(self, user_id: str) -> bool:
        """Check if user is cold-start (not enough interactions)."""
        count = self.get_user_interaction_count(user_id)
        return count < settings.cold_start_min_interactions

    def recommend(
        self,
        user_id: str,
        n: int = 20,
        category_id: Optional[str] = None,
        exclude_ids: Optional[set[str]] = None,
        use_hybrid: bool = True
    ) -> list[dict]:
        """
        Get hybrid recommendations for a user.

        Args:
            user_id: User ID to get recommendations for
            n: Number of recommendations to return
            category_id: Optional category filter
            exclude_ids: Product IDs to exclude
            use_hybrid: Whether to use hybrid scoring (vs pure SVD++)

        Returns:
            List of recommendation dicts with product details
        """
        if exclude_ids is None:
            exclude_ids = set()

        products_map = load_products_map()

        # Check if cold-start user
        if self.is_cold_start_user(user_id):
            logger.info("Cold-start user %s, using fallback recommendations", user_id)
            return self._get_fallback_recommendations(
                n=n,
                category_id=category_id,
                exclude_ids=exclude_ids,
                products_map=products_map,
                reason="COLD_START_TRENDING"
            )

        # Check if model is trained
        if recommender.model is None:
            logger.warning("SVD++ model not trained, using fallback")
            return self._get_fallback_recommendations(
                n=n,
                category_id=category_id,
                exclude_ids=exclude_ids,
                products_map=products_map,
                reason="NO_MODEL_FALLBACK"
            )

        # Get SVD++ recommendations
        try:
            svd_recs = recommender.recommend(
                user_id=user_id,
                n=n * 3,  # Get more for filtering
                exclude_ids=exclude_ids
            )
        except Exception as e:
            logger.error("SVD++ recommendation failed: %s", e)
            return self._get_fallback_recommendations(
                n=n,
                category_id=category_id,
                exclude_ids=exclude_ids,
                products_map=products_map,
                reason="ML_ERROR_FALLBACK"
            )

        if not svd_recs:
            logger.warning("SVD++ returned empty, using fallback")
            return self._get_fallback_recommendations(
                n=n,
                category_id=category_id,
                exclude_ids=exclude_ids,
                products_map=products_map,
                reason="EMPTY_ML_FALLBACK"
            )

        # If not using hybrid, just return SVD++ results
        if not use_hybrid:
            return self._enrich_recommendations(
                recommendations=svd_recs[:n],
                products_map=products_map,
                category_id=category_id,
                reason="ML_SVD_PLUS_PLUS"
            )

        # Compute hybrid scores
        popularity_scores = popularity_service.compute_popularity()
        hybrid_recs = []

        for rec in svd_recs:
            pid = rec["product_id"]

            # Skip if not in products map (deleted product)
            if pid not in products_map:
                continue

            # Apply category filter
            if category_id and products_map[pid].get("categoryId") != category_id:
                continue

            svd_score = rec["score"]
            pop_score = popularity_scores.get(pid, 0.0)

            # Normalize SVD score (assuming it's roughly in 0-5 range)
            normalized_svd = min(svd_score / 5.0, 1.0)

            # Compute hybrid score
            hybrid_score = (
                settings.hybrid_ml_weight * normalized_svd +
                settings.hybrid_popularity_weight * pop_score
            )

            hybrid_recs.append({
                "product_id": pid,
                "score": round(hybrid_score, 4),
                "svd_score": round(svd_score, 4),
                "popularity_score": round(pop_score, 4),
                "recommendation_reason": "HYBRID_SVD_POPULARITY"
            })

        # Sort by hybrid score
        hybrid_recs.sort(key=lambda x: x["score"], reverse=True)

        # Diversify: add some trending products if not enough results
        if len(hybrid_recs) < n:
            seen_pids = {r["product_id"] for r in hybrid_recs}
            trending = popularity_service.get_trending(
                n=n - len(hybrid_recs),
                category_id=category_id,
                exclude_ids=seen_pids | exclude_ids
            )
            for t in trending:
                hybrid_recs.append({
                    "product_id": t["product_id"],
                    "score": t["popularity_score"],
                    "svd_score": 0.0,
                    "popularity_score": t["popularity_score"],
                    "recommendation_reason": "TRENDING_DIVERSITY"
                })

        # Enrich with product details
        return self._enrich_recommendations(
            recommendations=hybrid_recs[:n],
            products_map=products_map,
            reason=None  # Keep original reason
        )

    def _get_fallback_recommendations(
        self,
        n: int,
        category_id: Optional[str],
        exclude_ids: set[str],
        products_map: dict,
        reason: str
    ) -> list[dict]:
        """Get fallback recommendations when ML is unavailable."""
        results = []

        # Try trending first
        if settings.fallback_to_popularity:
            trending = popularity_service.get_trending(
                n=n,
                category_id=category_id,
                exclude_ids=exclude_ids
            )
            for t in trending:
                t["recommendation_reason"] = reason
            results.extend(trending)

        # If still not enough, get latest
        if len(results) < n and settings.fallback_to_latest:
            seen_pids = {r["product_id"] for r in results}
            latest = popularity_service.get_latest_products(
                n=n - len(results),
                category_id=category_id,
                exclude_ids=seen_pids | exclude_ids
            )
            for l in latest:
                l["recommendation_reason"] = f"{reason}_LATEST"
            results.extend(latest)

        return self._enrich_recommendations(
            recommendations=results[:n],
            products_map=products_map,
            reason=None  # Keep original reason
        )

    def _enrich_recommendations(
        self,
        recommendations: list[dict],
        products_map: dict,
        category_id: Optional[str] = None,
        reason: Optional[str] = None
    ) -> list[dict]:
        """Add product details to recommendations."""
        enriched = []

        for rank, rec in enumerate(recommendations, start=1):
            pid = rec["product_id"]
            pinfo = products_map.get(pid, {})

            # Apply category filter if specified and not already filtered
            if category_id and pinfo.get("categoryId") != category_id:
                continue

            images = pinfo.get("images", [])

            enriched.append({
                "product_id": pid,
                "product_name": pinfo.get("name"),
                "product_image": images[0] if images else None,
                "price": pinfo.get("price"),
                "final_price": pinfo.get("finalPrice"),
                "average_rating": pinfo.get("averageRating"),
                "review_count": pinfo.get("reviewCount"),
                "score": rec.get("score", rec.get("popularity_score", 0)),
                "rank": rank,
                "recommendation_reason": reason or rec.get("recommendation_reason", "UNKNOWN"),
                "explanation": self._get_explanation(rec.get("recommendation_reason", reason))
            })

        return enriched

    def _get_explanation(self, reason: str) -> str:
        """Get human-readable explanation for recommendation reason."""
        explanations = {
            "ML_SVD_PLUS_PLUS": "Goi y tu mo hinh AI SVD++ dua tren hanh vi nguoi dung tuong tu",
            "HYBRID_SVD_POPULARITY": "Ket hop AI va xu huong pho bien",
            "TRENDING": "San pham dang hot, duoc nhieu nguoi quan tam",
            "TRENDING_DIVERSITY": "San pham pho bien de da dang hoa goi y",
            "COLD_START_TRENDING": "San pham pho bien cho nguoi dung moi",
            "LATEST": "San pham moi nhat",
            "NO_MODEL_FALLBACK": "San pham pho bien (mo hinh dang duoc train)",
            "ML_ERROR_FALLBACK": "San pham pho bien (loi tam thoi tu AI)",
            "EMPTY_ML_FALLBACK": "San pham pho bien"
        }
        return explanations.get(reason, "Goi y dua tren phan tich du lieu")

    def get_similar_products(
        self,
        product_id: str,
        n: int = 10,
        category_id: Optional[str] = None
    ) -> list[dict]:
        """
        Get products similar to the given product.

        Uses SVD++ latent factors for similarity.
        Falls back to same-category products if model not available.
        """
        products_map = load_products_map()

        # Try SVD++ similarity
        if recommender.model is not None:
            similar = recommender.similar_products(product_id=product_id, n=n * 2)

            if similar:
                results = []
                for item in similar:
                    pid = item["product_id"]
                    pinfo = products_map.get(pid, {})

                    # Apply category filter
                    if category_id and pinfo.get("categoryId") != category_id:
                        continue

                    images = pinfo.get("images", [])
                    results.append({
                        "product_id": pid,
                        "product_name": pinfo.get("name"),
                        "product_image": images[0] if images else None,
                        "price": pinfo.get("price"),
                        "final_price": pinfo.get("finalPrice"),
                        "similarity": item["similarity"],
                        "rank": len(results) + 1,
                        "recommendation_reason": "ML_LATENT_SIMILARITY"
                    })

                    if len(results) >= n:
                        break

                if results:
                    return results

        # Fallback: same category products
        logger.info("Falling back to category-based similarity for %s", product_id)
        return self._get_category_similar(
            product_id=product_id,
            n=n,
            products_map=products_map
        )

    def _get_category_similar(
        self,
        product_id: str,
        n: int,
        products_map: dict
    ) -> list[dict]:
        """Fallback similarity based on category."""
        if product_id not in products_map:
            return []

        target_category = products_map[product_id].get("categoryId")
        if not target_category:
            return []

        # Get all products in same category
        category_products = [
            (pid, pinfo)
            for pid, pinfo in products_map.items()
            if pinfo.get("categoryId") == target_category and pid != product_id
        ]

        # Sort by rating/popularity
        category_products.sort(
            key=lambda x: (x[1].get("averageRating", 0), x[1].get("reviewCount", 0)),
            reverse=True
        )

        results = []
        for pid, pinfo in category_products[:n]:
            images = pinfo.get("images", [])
            results.append({
                "product_id": pid,
                "product_name": pinfo.get("name"),
                "product_image": images[0] if images else None,
                "price": pinfo.get("price"),
                "final_price": pinfo.get("finalPrice"),
                "similarity": 0.5,  # Default similarity for category match
                "rank": len(results) + 1,
                "recommendation_reason": "CATEGORY_SIMILAR"
            })

        return results


# Module-level singleton
hybrid_recommender = HybridRecommender()
