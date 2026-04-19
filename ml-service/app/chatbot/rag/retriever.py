"""
Retriever
---------
Truy van thong tin tu VectorStore va databases.
"""

from typing import List, Dict, Any, Optional
from loguru import logger
from pymongo import MongoClient

from .vectorstore import VectorStore


class Retriever:
    """
    Retriever de lay thong tin phuc vu RAG.
    """

    def __init__(
        self,
        vector_store: VectorStore,
        mongo_client: MongoClient,
        mongo_db_name: str = "cellex",
    ):
        """
        Khoi tao retriever.

        Args:
            vector_store: VectorStore instance
            mongo_client: MongoDB client
            mongo_db_name: MongoDB database name
        """
        self.vector_store = vector_store
        self.mongo_client = mongo_client
        self.db = mongo_client[mongo_db_name]

    def retrieve_products(
        self, query: str, top_k: int = 5, filters: Optional[Dict] = None
    ) -> List[Dict[str, Any]]:
        """
        Truy van products tu vector store + MongoDB.

        Args:
            query: Search query
            top_k: Number of results
            filters: Metadata filters

        Returns:
            List products with full details
        """
        # Tim kiem semantic trong vector store
        vector_results = self.vector_store.search_products(query, top_k, filters)

        if not vector_results:
            logger.info("No vector results, falling back to keyword search")
            return self._keyword_search_products(query, top_k)

        # Lay full product details tu MongoDB
        product_ids = [r["product_id"] for r in vector_results]
        products = list(self.db.products.find({"_id": {"$in": product_ids}}))

        # Kem score tu vector search
        product_map = {str(p["_id"]): p for p in products}
        enriched = []
        for vr in vector_results:
            pid = vr["product_id"]
            if pid in product_map:
                product = product_map[pid]
                product["relevance_score"] = 1 - vr["distance"]  # Convert distance to similarity
                enriched.append(product)

        return enriched

    def _keyword_search_products(self, query: str, limit: int = 5) -> List[Dict]:
        """
        Fallback: keyword search trong MongoDB.

        Args:
            query: Search query
            limit: Max results

        Returns:
            List products
        """
        try:
            results = self.db.products.find(
                {
                    "$or": [
                        {"title": {"$regex": query, "$options": "i"}},
                        {"description": {"$regex": query, "$options": "i"}},
                        {"brand": {"$regex": query, "$options": "i"}},
                    ]
                }
            ).limit(limit)

            return list(results)

        except Exception as e:
            logger.error(f"Keyword search failed: {e}")
            return []

    def retrieve_product_context(self, product_id: str) -> Dict[str, Any]:
        """
        Lay full context cua product: details + reviews + similar.

        Args:
            product_id: Product ID

        Returns:
            Dict chua product + reviews + similar products
        """
        context = {"product": None, "reviews": [], "similar_products": []}

        # Product details
        product = self.db.products.find_one({"_id": product_id})
        if not product:
            return context

        context["product"] = product

        # Reviews (top 10 most helpful)
        reviews = list(
            self.db.reviews.find({"product_id": product_id})
            .sort([("helpful_count", -1), ("rating", -1)])
            .limit(10)
        )
        context["reviews"] = reviews

        # Similar products (same category)
        similar = list(
            self.db.products.find(
                {"category": product.get("category"), "_id": {"$ne": product_id}}
            ).limit(5)
        )
        context["similar_products"] = similar

        return context

    def retrieve_order_info(
        self, user_id: str, order_id: Optional[str] = None
    ) -> List[Dict[str, Any]]:
        """
        Lay thong tin don hang cua user.

        Args:
            user_id: User ID
            order_id: Optional specific order ID

        Returns:
            List orders
        """
        query = {"user_id": user_id}
        if order_id:
            query["_id"] = order_id

        try:
            orders = list(self.db.orders.find(query).sort("created_at", -1).limit(20))
            return orders
        except Exception as e:
            logger.error(f"Failed to retrieve orders: {e}")
            return []

    def retrieve_shop_metrics(
        self, shop_id: str, days: int = 7
    ) -> Dict[str, Any]:
        """
        Lay metrics cua shop (cho SELLER role).

        Args:
            shop_id: Shop ID
            days: Number of days to look back

        Returns:
            Shop metrics dict
        """
        # Placeholder - se implement day du khi co du lieu
        metrics = {
            "shop_id": shop_id,
            "period_days": days,
            "total_revenue": 0,
            "total_orders": 0,
            "avg_order_value": 0,
            "conversion_rate": 0,
            "top_products": [],
        }

        try:
            # TODO: Implement aggregation pipeline khi co orders data
            pass
        except Exception as e:
            logger.error(f"Failed to retrieve shop metrics: {e}")

        return metrics

    def retrieve_system_metrics(self) -> Dict[str, Any]:
        """
        Lay system-level metrics (cho ADMIN role).

        Returns:
            System metrics dict
        """
        metrics = {
            "total_products": 0,
            "total_orders": 0,
            "total_users": 0,
            "total_reviews": 0,
        }

        try:
            metrics["total_products"] = self.db.products.count_documents({})
            metrics["total_orders"] = self.db.orders.count_documents({})
            metrics["total_reviews"] = self.db.reviews.count_documents({})
            # total_users from PostgreSQL - skip for now
        except Exception as e:
            logger.error(f"Failed to retrieve system metrics: {e}")

        return metrics
