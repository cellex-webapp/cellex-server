"""
Retriever
---------
Truy van thong tin tu VectorStore va databases.

Field mapping (Spring Boot schemas):
  MongoDB - Product: name, description, images, price, finalPrice,
            averageRating, reviewCount, purchaseCount, shopId, categoryId, isPublished
  MongoDB - UserInteraction: userId, productId, viewCount, cartCount,
            purchaseCount, reviewCount, totalScore
  PostgreSQL - orders table: queried via Spring Boot REST API (spring_boot_base_url)
"""

from datetime import datetime, timedelta
from typing import List, Dict, Any, Optional
from loguru import logger
from pymongo import MongoClient
from bson import ObjectId

from .vectorstore import VectorStore
from ...config import settings


class Retriever:
    """
    Retriever de lay thong tin phuc vu RAG.
    Kết nối trực tiếp MongoDB (products, user_interactions, reviews)
    và query orders qua aggregation MongoDB embed hoac fallback.
    """

    def __init__(
        self,
        vector_store: VectorStore,
        mongo_client: MongoClient,
        mongo_db_name: str = "cellex",
    ):
        self.vector_store = vector_store
        self.mongo_client = mongo_client
        self.db = mongo_client[mongo_db_name]

    # ──────────────────────────────────────────────────────────────────────────
    # PRODUCT RETRIEVAL
    # ──────────────────────────────────────────────────────────────────────────

    def retrieve_products(
        self, query: str, top_k: int = 5, filters: Optional[Dict] = None
    ) -> List[Dict[str, Any]]:
        """
        Truy van products tu vector store + MongoDB.
        Field names khớp với Product.java (MongoDB @Document):
            name, description, images, price, finalPrice, averageRating, reviewCount, shopId, isPublished
        """
        vector_results = self.vector_store.search_products(query, top_k, filters)

        if not vector_results:
            logger.info("No vector results, falling back to keyword search")
            return self._keyword_search_products(query, top_k)

        product_ids = [r["product_id"] for r in vector_results]
        # _id là String trong MongoDB Product
        products = list(self.db.products.find({"_id": {"$in": product_ids}, "isPublished": True}))

        product_map = {str(p["_id"]): p for p in products}
        enriched = []
        for vr in vector_results:
            pid = vr["product_id"]
            if pid in product_map:
                product = product_map[pid]
                product["relevance_score"] = max(0.0, 1 - vr["distance"])
                enriched.append(self._format_product(product))

        return enriched

    def _keyword_search_products(self, query: str, limit: int = 5) -> List[Dict]:
        """Fallback: keyword search trong MongoDB Products."""
        try:
            results = self.db.products.find(
                {
                    "isPublished": True,
                    "$or": [
                        {"name": {"$regex": query, "$options": "i"}},
                        {"description": {"$regex": query, "$options": "i"}},
                    ],
                }
            ).limit(limit)
            return [self._format_product(p) for p in results]
        except Exception as e:
            logger.error(f"Keyword search failed: {e}")
            return []

    def _format_product(self, p: Dict) -> Dict:
        """Chuẩn hóa product document để trả về chatbot tools."""
        images = p.get("images") or []
        return {
            "_id": str(p.get("_id", "")),
            "id": str(p.get("_id", "")),
            # Dùng đúng field names từ Product.java
            "name": p.get("name", ""),
            "description": p.get("description", ""),
            "price": p.get("price"),
            "finalPrice": p.get("finalPrice"),
            "averageRating": p.get("averageRating", 0.0),
            "reviewCount": p.get("reviewCount", 0),
            "purchaseCount": p.get("purchaseCount", 0),
            "images": images,
            "image": images[0] if images else None,
            "shopId": p.get("shopId"),
            "categoryId": p.get("categoryId"),
            "isPublished": p.get("isPublished", False),
            "stockQuantity": p.get("stockQuantity"),
            "relevance_score": p.get("relevance_score", 0.0),
            "attributeValues": p.get("attributeValues") or [],
            # Legacy aliases cho backward-compat với tool code cũ
            "title": p.get("name", ""),
            "rating": p.get("averageRating", 0.0),
            "review_count": p.get("reviewCount", 0),
        }

    def retrieve_product_context(self, product_id: str) -> Dict[str, Any]:
        """
        Lay full context cua product: details + reviews + similar (same category).
        """
        context = {"product": None, "reviews": [], "similar_products": []}

        # Thử tìm bằng _id (String trong MongoDB)
        product = self.db.products.find_one({"_id": product_id})
        if not product:
            return context

        context["product"] = self._format_product(product)

        # Reviews (MongoDB collection)
        try:
            reviews = list(
                self.db.reviews.find({"productId": product_id})
                .sort([("helpful_count", -1), ("rating", -1)])
                .limit(10)
            )
            context["reviews"] = reviews
        except Exception:
            pass

        # Similar products trong cùng category
        try:
            similar = list(
                self.db.products.find(
                    {
                        "categoryId": product.get("categoryId"),
                        "_id": {"$ne": product_id},
                        "isPublished": True,
                    }
                )
                .sort([("averageRating", -1), ("reviewCount", -1)])
                .limit(5)
            )
            context["similar_products"] = [self._format_product(s) for s in similar]
        except Exception:
            pass

        return context

    # ──────────────────────────────────────────────────────────────────────────
    # ORDER RETRIEVAL  (Orders nằm trong PostgreSQL, không trực tiếp trong MongoDB)
    # ml-service chỉ biết MongoDB → ta embed thông tin order vào MongoDB
    # hoặc fallback: đọc qua Spring Boot API nếu cần.
    # Hiện tại: query user_interactions để lấy thông tin hành vi thay thế.
    # ──────────────────────────────────────────────────────────────────────────

    def retrieve_order_info(
        self, user_id: str, order_id: Optional[str] = None
    ) -> List[Dict[str, Any]]:
        """
        Lay thong tin don hang cua user.
        Note: Orders lưu ở PostgreSQL (Spring Boot). ml-service không có trực tiếp.
        Trả về thông tin từ user_interactions (MongoDB) thay thế,
        kết hợp với summary đơn giản.
        """
        # Tìm trong user_interactions: các purchase của user
        query: Dict[str, Any] = {"userId": user_id, "purchaseCount": {"$gt": 0}}
        try:
            interactions = list(
                self.db.user_interactions.find(query)
                .sort("updatedAt", -1)
                .limit(20)
            )

            orders = []
            for interaction in interactions:
                pid = interaction.get("productId", "")
                # Lấy thông tin sản phẩm
                product = self.db.products.find_one({"_id": pid}) or {}
                product_name = product.get("name", "")
                price = product.get("finalPrice") or product.get("price", 0)

                orders.append({
                    "_id": f"interaction_{interaction.get('_id', '')}",
                    "id": f"interaction_{interaction.get('_id', '')}",
                    "userId": user_id,
                    "productId": pid,
                    "productName": product_name,
                    "purchaseCount": interaction.get("purchaseCount", 0),
                    "estimatedTotal": (price or 0) * interaction.get("purchaseCount", 1),
                    "updatedAt": str(interaction.get("updatedAt", "")),
                    # Note: trạng thái thật nằm ở PostgreSQL
                    "status": "purchased",
                    "source": "user_interactions",
                })

            return orders

        except Exception as e:
            logger.error(f"Failed to retrieve order info: {e}")
            return []

    # ──────────────────────────────────────────────────────────────────────────
    # SHOP KPI RETRIEVAL  (SELLER role)
    # ──────────────────────────────────────────────────────────────────────────

    def retrieve_shop_metrics(
        self, shop_id: str, days: int = 7
    ) -> Dict[str, Any]:
        """
        Lay metrics cua shop (cho SELLER role).
        
        Data sources:
        - products (MongoDB): đếm sản phẩm, avg rating
        - user_interactions (MongoDB): purchase/view/cart events
        
        Note: Revenue thật từ PostgreSQL orders - nếu cần chính xác hoàn toàn
        thì gọi Spring Boot API, nhưng ta có thể ước tính từ user_interactions.
        """
        since = datetime.utcnow() - timedelta(days=days)
        metrics: Dict[str, Any] = {
            "shop_id": shop_id,
            "period_days": days,
        }

        try:
            # ── 1. Product stats ─────────────────────────────────────────
            products = list(self.db.products.find({"shopId": shop_id}))
            published_products = [p for p in products if p.get("isPublished", False)]
            total_products = len(products)
            published_count = len(published_products)

            avg_rating = 0.0
            avg_price = 0.0
            total_reviews = 0
            if published_products:
                ratings = [p.get("averageRating", 0) for p in published_products if p.get("averageRating")]
                prices = [p.get("finalPrice") or p.get("price", 0) for p in published_products]
                reviews = [p.get("reviewCount", 0) for p in published_products]
                avg_rating = sum(ratings) / len(ratings) if ratings else 0.0
                avg_price = sum(prices) / len(prices) if prices else 0.0
                total_reviews = sum(reviews)

            metrics["total_products"] = total_products
            metrics["published_products"] = published_count
            metrics["avg_product_rating"] = round(avg_rating, 2)
            metrics["avg_product_price"] = round(avg_price, 0)
            metrics["total_reviews"] = total_reviews

            # ── 2. Interaction stats từ user_interactions ─────────────────
            product_ids = [str(p["_id"]) for p in products]

            if product_ids:
                # Aggregation pipeline: tổng hợp tương tác theo shop's products
                pipeline = [
                    {
                        "$match": {
                            "productId": {"$in": product_ids},
                            "updatedAt": {"$gte": since},
                        }
                    },
                    {
                        "$group": {
                            "_id": None,
                            "total_views": {"$sum": "$viewCount"},
                            "total_carts": {"$sum": "$cartCount"},
                            "total_purchases": {"$sum": "$purchaseCount"},
                            "unique_users": {"$addToSet": "$userId"},
                        }
                    },
                ]
                agg_result = list(self.db.user_interactions.aggregate(pipeline))

                if agg_result:
                    row = agg_result[0]
                    total_views = row.get("total_views", 0)
                    total_carts = row.get("total_carts", 0)
                    total_purchases = row.get("total_purchases", 0)
                    unique_buyers = len(row.get("unique_users", []))

                    # Ước tính doanh thu từ purchases
                    # Lấy avg_price đã tính ở trên
                    estimated_revenue = total_purchases * avg_price

                    # Conversion rate = purchases / views
                    conversion_rate = (total_purchases / total_views * 100) if total_views > 0 else 0.0
                    # Cart abandonment = (carts - purchases) / carts
                    cart_abandonment = ((total_carts - total_purchases) / total_carts * 100) if total_carts > 0 else 0.0

                    metrics["total_views"] = total_views
                    metrics["total_carts"] = total_carts
                    metrics["total_orders"] = total_purchases  # estimate from interactions
                    metrics["unique_buyers"] = unique_buyers
                    metrics["estimated_revenue"] = round(estimated_revenue, 0)
                    metrics["conversion_rate"] = round(conversion_rate, 2)
                    metrics["cart_abandonment_rate"] = round(cart_abandonment, 2)
                else:
                    metrics.update({
                        "total_views": 0, "total_carts": 0,
                        "total_orders": 0, "unique_buyers": 0,
                        "estimated_revenue": 0, "conversion_rate": 0.0,
                        "cart_abandonment_rate": 0.0,
                    })

            # ── 3. Top sản phẩm của shop ─────────────────────────────────
            top_products = sorted(
                published_products,
                key=lambda p: (p.get("purchaseCount", 0), p.get("reviewCount", 0)),
                reverse=True,
            )[:5]
            metrics["top_products"] = [
                {
                    "id": str(p.get("_id", "")),
                    "name": p.get("name", ""),
                    "price": p.get("finalPrice") or p.get("price", 0),
                    "averageRating": p.get("averageRating", 0),
                    "reviewCount": p.get("reviewCount", 0),
                    "purchaseCount": p.get("purchaseCount", 0),
                }
                for p in top_products
            ]

            metrics["data_source"] = "mongodb_user_interactions"
            metrics["note"] = (
                "Revenue is estimated from interaction data. "
                "For exact revenue, query Spring Boot analytics API."
            )

        except Exception as e:
            logger.error(f"Failed to retrieve shop metrics: {e}")
            metrics["error"] = str(e)

        return metrics

    def retrieve_shop_bestsellers(
        self, shop_id: str, limit: int = 10
    ) -> List[Dict[str, Any]]:
        """Lay bestsellers cua shop (theo purchaseCount + reviewCount)."""
        try:
            products = list(
                self.db.products.find({"shopId": shop_id, "isPublished": True})
                .sort([("purchaseCount", -1), ("reviewCount", -1)])
                .limit(limit)
            )
            return [self._format_product(p) for p in products]
        except Exception as e:
            logger.error(f"Failed to retrieve bestsellers: {e}")
            return []

    # ──────────────────────────────────────────────────────────────────────────
    # SYSTEM METRICS RETRIEVAL  (ADMIN role)
    # ──────────────────────────────────────────────────────────────────────────

    def retrieve_system_metrics(self) -> Dict[str, Any]:
        """
        Lay system-level metrics (cho ADMIN role).
        Lấy từ MongoDB collections: products, user_interactions, reviews.
        """
        metrics: Dict[str, Any] = {}

        try:
            # Products (MongoDB)
            metrics["total_products"] = self.db.products.count_documents({})
            metrics["published_products"] = self.db.products.count_documents({"isPublished": True})

            # User interactions (MongoDB)
            metrics["total_interactions"] = self.db.user_interactions.count_documents({})
            metrics["unique_users_with_interactions"] = len(
                self.db.user_interactions.distinct("userId")
            )
            metrics["unique_products_interacted"] = len(
                self.db.user_interactions.distinct("productId")
            )

            # Reviews (MongoDB)
            try:
                metrics["total_reviews"] = self.db.reviews.count_documents({})
            except Exception:
                metrics["total_reviews"] = 0

            # Interaction breakdown
            pipeline = [
                {
                    "$group": {
                        "_id": None,
                        "total_views": {"$sum": "$viewCount"},
                        "total_carts": {"$sum": "$cartCount"},
                        "total_purchases": {"$sum": "$purchaseCount"},
                    }
                }
            ]
            agg = list(self.db.user_interactions.aggregate(pipeline))
            if agg:
                metrics["total_views"] = agg[0].get("total_views", 0)
                metrics["total_carts"] = agg[0].get("total_carts", 0)
                metrics["total_purchases_interactions"] = agg[0].get("total_purchases", 0)

            # Recent activity (last 7 days)
            since_7d = datetime.utcnow() - timedelta(days=7)
            metrics["new_interactions_7d"] = self.db.user_interactions.count_documents(
                {"updatedAt": {"$gte": since_7d}}
            )

            # Product health
            low_stock = self.db.products.count_documents(
                {"isPublished": True, "stockQuantity": {"$lte": 5, "$gt": 0}}
            )
            out_of_stock = self.db.products.count_documents(
                {"isPublished": True, "stockQuantity": {"$lte": 0}}
            )
            metrics["low_stock_products"] = low_stock
            metrics["out_of_stock_products"] = out_of_stock

            # Top categories by interaction
            cat_pipeline = [
                {
                    "$group": {
                        "_id": "$categoryId",
                        "interaction_count": {"$sum": 1},
                        "total_purchases": {"$sum": "$purchaseCount"},
                    }
                },
                {"$sort": {"interaction_count": -1}},
                {"$limit": 5},
            ]
            top_categories = list(self.db.user_interactions.aggregate(cat_pipeline))
            metrics["top_categories"] = [
                {
                    "category_id": c.get("_id", ""),
                    "interaction_count": c.get("interaction_count", 0),
                    "total_purchases": c.get("total_purchases", 0),
                }
                for c in top_categories
            ]

            metrics["data_source"] = "mongodb"

        except Exception as e:
            logger.error(f"Failed to retrieve system metrics: {e}")
            metrics["error"] = str(e)

        return metrics

    def retrieve_anomalies(self, days: int = 7) -> Dict[str, Any]:
        """
        Phat hien bat thuong trong he thong (ADMIN role).
        """
        since = datetime.utcnow() - timedelta(days=days)
        anomalies: Dict[str, Any] = {"period_days": days, "flags": []}

        try:
            # Sản phẩm hết hàng nhưng vẫn published
            out_of_stock_published = self.db.products.count_documents(
                {"isPublished": True, "stockQuantity": {"$lte": 0}}
            )
            if out_of_stock_published > 0:
                anomalies["flags"].append({
                    "type": "out_of_stock_published",
                    "count": out_of_stock_published,
                    "severity": "HIGH",
                    "message": f"{out_of_stock_published} sản phẩm đã hết hàng nhưng vẫn đang published",
                })

            # Sản phẩm rating thấp (< 2.0) nhưng vẫn active
            low_rating = self.db.products.count_documents(
                {"isPublished": True, "averageRating": {"$gt": 0, "$lt": 2.0}, "reviewCount": {"$gte": 5}}
            )
            if low_rating > 0:
                anomalies["flags"].append({
                    "type": "low_rating_products",
                    "count": low_rating,
                    "severity": "MEDIUM",
                    "message": f"{low_rating} sản phẩm có rating < 2.0 với ít nhất 5 đánh giá",
                })

            # Sản phẩm không có interaction gần đây
            all_product_ids = [str(p["_id"]) for p in self.db.products.find({"isPublished": True}, {"_id": 1})]
            if all_product_ids:
                active_product_ids = set(
                    self.db.user_interactions.distinct(
                        "productId",
                        {"updatedAt": {"$gte": since}},
                    )
                )
                inactive_products = len(set(all_product_ids) - active_product_ids)
                if inactive_products > 0:
                    anomalies["flags"].append({
                        "type": "inactive_products",
                        "count": inactive_products,
                        "severity": "LOW",
                        "message": f"{inactive_products} sản phẩm published không có tương tác trong {days} ngày qua",
                    })

            anomalies["total_flags"] = len(anomalies["flags"])
            anomalies["data_source"] = "mongodb"

        except Exception as e:
            logger.error(f"Failed to detect anomalies: {e}")
            anomalies["error"] = str(e)

        return anomalies

    # ──────────────────────────────────────────────────────────────────────────
    # COUPON RETRIEVAL  (SELLER / BUYER role)
    # ──────────────────────────────────────────────────────────────────────────

    def retrieve_coupon_strategy(self, shop_id: str) -> Dict[str, Any]:
        """
        Phan tich du lieu san pham de goi y chien luoc coupon cho SELLER.
        Data: product avg price, category distribution, interaction rates.
        """
        try:
            products = list(
                self.db.products.find({"shopId": shop_id, "isPublished": True})
            )
            if not products:
                return {"success": False, "message": "Shop chua co san pham nao"}

            product_ids = [str(p["_id"]) for p in products]

            # Interaction data
            pipeline = [
                {"$match": {"productId": {"$in": product_ids}}},
                {
                    "$group": {
                        "_id": "$productId",
                        "views": {"$sum": "$viewCount"},
                        "carts": {"$sum": "$cartCount"},
                        "purchases": {"$sum": "$purchaseCount"},
                    }
                },
            ]
            interactions = {
                row["_id"]: row
                for row in self.db.user_interactions.aggregate(pipeline)
            }

            # Sản phẩm nhiều view nhưng ít mua = candidates for coupon
            coupon_candidates = []
            for p in products:
                pid = str(p["_id"])
                inter = interactions.get(pid, {})
                views = inter.get("views", 0)
                carts = inter.get("carts", 0)
                purchases = inter.get("purchases", 0)
                conversion = purchases / views if views > 0 else 0

                if views >= 10 and conversion < 0.05:  # nhiều view, ít mua
                    coupon_candidates.append({
                        "productId": pid,
                        "name": p.get("name", ""),
                        "price": p.get("finalPrice") or p.get("price", 0),
                        "views": views,
                        "carts": carts,
                        "purchases": purchases,
                        "conversion_rate": round(conversion * 100, 2),
                        "suggested_discount": "10-15%",
                        "reason": "Nhiều lượt xem nhưng conversion thấp",
                    })

            coupon_candidates.sort(key=lambda x: x["views"], reverse=True)

            return {
                "shop_id": shop_id,
                "total_analyzed_products": len(products),
                "coupon_candidates": coupon_candidates[:10],
                "strategy_summary": (
                    f"Tìm thấy {len(coupon_candidates)} sản phẩm phù hợp để áp dụng coupon. "
                    "Ưu tiên các sản phẩm có lượt xem cao nhưng tỷ lệ chuyển đổi thấp."
                ),
            }

        except Exception as e:
            logger.error(f"Failed to retrieve coupon strategy: {e}")
            return {"success": False, "error": str(e)}

    def retrieve_inventory_analysis(self, shop_id: str) -> Dict[str, Any]:
        """
        Phan tich ton kho va stockout risk cho SELLER.
        """
        try:
            products = list(
                self.db.products.find({"shopId": shop_id, "isPublished": True})
            )
            if not products:
                return {"success": False, "message": "Khong co san pham nao"}

            product_ids = [str(p["_id"]) for p in products]

            # Lấy tốc độ bán gần đây (30 ngày)
            since_30d = datetime.utcnow() - timedelta(days=30)
            pipeline = [
                {
                    "$match": {
                        "productId": {"$in": product_ids},
                        "updatedAt": {"$gte": since_30d},
                    }
                },
                {
                    "$group": {
                        "_id": "$productId",
                        "monthly_sales": {"$sum": "$purchaseCount"},
                    }
                },
            ]
            sales_data = {
                row["_id"]: row["monthly_sales"]
                for row in self.db.user_interactions.aggregate(pipeline)
            }

            out_of_stock = []
            low_stock = []
            healthy = []
            overstocked = []

            for p in products:
                pid = str(p["_id"])
                stock = p.get("stockQuantity", 0) or 0
                monthly_sales = sales_data.get(pid, 0)

                # Days of stock = stock / daily_sales
                daily_sales = monthly_sales / 30 if monthly_sales > 0 else 0
                days_of_stock = stock / daily_sales if daily_sales > 0 else None

                item = {
                    "productId": pid,
                    "name": p.get("name", ""),
                    "stock": stock,
                    "monthly_sales": monthly_sales,
                    "daily_sales_rate": round(daily_sales, 2),
                    "days_of_stock": round(days_of_stock, 0) if days_of_stock is not None else None,
                }

                if stock <= 0:
                    item["risk"] = "OUT_OF_STOCK"
                    out_of_stock.append(item)
                elif days_of_stock is not None and days_of_stock <= 7:
                    item["risk"] = "CRITICAL_LOW"
                    low_stock.append(item)
                elif days_of_stock is not None and days_of_stock <= 14:
                    item["risk"] = "LOW"
                    low_stock.append(item)
                elif daily_sales == 0 and stock > 50:
                    item["risk"] = "OVERSTOCK"
                    overstocked.append(item)
                else:
                    item["risk"] = "HEALTHY"
                    healthy.append(item)

            return {
                "shop_id": shop_id,
                "total_products": len(products),
                "out_of_stock": out_of_stock,
                "low_stock": low_stock,
                "overstocked": overstocked,
                "healthy_stock_count": len(healthy),
                "summary": {
                    "out_of_stock_count": len(out_of_stock),
                    "low_stock_count": len(low_stock),
                    "overstock_count": len(overstocked),
                    "healthy_count": len(healthy),
                },
            }

        except Exception as e:
            logger.error(f"Failed to retrieve inventory: {e}")
            return {"success": False, "error": str(e)}
