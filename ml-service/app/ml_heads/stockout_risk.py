"""
Stockout Risk
-------------
Dự báo rủi ro hết hàng cho sản phẩm.
Data sources:
  - MongoDB (products): stockQuantity, purchaseCount, isPublished
  - PostgreSQL (order_items): tốc độ bán gần đây (actual velocity)
  - MongoDB (user_interactions): view/cart/purchase rates

Risk Model:
  1. Tính daily_sales_velocity từ PostgreSQL (nếu có) hoặc user_interactions
  2. Tính days_until_stockout = stock / velocity
  3. Classify: CRITICAL (≤3 days), HIGH (≤7 days), MEDIUM (≤14 days), LOW (>14 days), SAFE (no data)
  4. Thêm confidence score dựa trên độ ổn định của velocity

Output:
  - risk_level, days_until_stockout, reorder_suggestion, confidence
"""

from __future__ import annotations

from datetime import datetime, timedelta
from typing import Dict, Any, List, Optional, Tuple

import numpy as np
import pandas as pd
from loguru import logger
from pymongo import MongoClient


class StockoutRiskService:
    """
    Phân tích và dự báo rủi ro stockout.
    """

    # Risk thresholds (days of stock remaining)
    THRESHOLDS = {
        "CRITICAL": 3,
        "HIGH": 7,
        "MEDIUM": 14,
        "LOW": 30,
    }

    def __init__(self, postgres_client, mongo_client: MongoClient, mongo_db: str = "cellex"):
        self.pg = postgres_client
        self.db = mongo_client[mongo_db]

    def analyze_shop(
        self,
        shop_id: str,
        analysis_days: int = 30,
    ) -> Dict[str, Any]:
        """
        Phân tích stockout risk cho tất cả sản phẩm của shop.

        Returns:
            dict: Danh sách sản phẩm theo risk level, summary
        """
        products = list(
            self.db.products.find({"shopId": shop_id, "isPublished": True})
        )
        if not products:
            return {
                "success": False,
                "shop_id": shop_id,
                "message": "Shop không có sản phẩm nào được published",
            }

        results = []
        for product in products:
            risk = self._analyze_single_product(product, shop_id, analysis_days)
            results.append(risk)

        # Phân loại theo risk level
        by_risk: Dict[str, list] = {"CRITICAL": [], "HIGH": [], "MEDIUM": [], "LOW": [], "SAFE": []}
        for r in results:
            level = r.get("risk_level", "SAFE")
            by_risk.setdefault(level, []).append(r)

        # Sort by days_until_stockout ascending
        for level in by_risk:
            by_risk[level].sort(
                key=lambda x: x.get("days_until_stockout") or 9999
            )

        return {
            "success": True,
            "shop_id": shop_id,
            "analysis_days": analysis_days,
            "total_products": len(results),
            "summary": {
                "critical": len(by_risk["CRITICAL"]),
                "high": len(by_risk["HIGH"]),
                "medium": len(by_risk["MEDIUM"]),
                "low": len(by_risk["LOW"]),
                "safe": len(by_risk["SAFE"]),
            },
            "products_by_risk": by_risk,
            "urgent_action_required": by_risk["CRITICAL"] + by_risk["HIGH"],
        }

    def analyze_product(
        self,
        product_id: str,
        shop_id: Optional[str] = None,
        analysis_days: int = 30,
    ) -> Dict[str, Any]:
        """Phân tích stockout risk cho một sản phẩm cụ thể."""
        product = self.db.products.find_one({"_id": product_id})
        if not product:
            return {"success": False, "message": f"Product {product_id} not found"}

        risk = self._analyze_single_product(product, shop_id or product.get("shopId", ""), analysis_days)
        return {"success": True, **risk}

    # ─── Internal ───────────────────────────────────────────────────────────

    def _analyze_single_product(
        self,
        product: dict,
        shop_id: str,
        analysis_days: int,
    ) -> Dict[str, Any]:
        """Tính toán risk cho 1 sản phẩm."""
        product_id = str(product.get("_id", ""))
        stock = int(product.get("stockQuantity", 0) or 0)

        # Tính velocity từ PostgreSQL hoặc MongoDB fallback
        velocity, velocity_std, data_source = self._get_velocity(
            product_id, shop_id, analysis_days
        )

        # Tính days_until_stockout
        days_until_stockout = None
        if velocity > 0 and stock >= 0:
            days_until_stockout = round(stock / velocity, 1)

        # Risk level
        risk_level = self._classify_risk(stock, velocity, days_until_stockout)

        # Reorder suggestion
        reorder_suggestion = self._compute_reorder_suggestion(velocity, days_until_stockout)

        # Confidence
        confidence = "high" if data_source == "postgresql" and analysis_days >= 30 else \
                     "medium" if data_source == "postgresql" else "low"

        return {
            "product_id": product_id,
            "product_name": product.get("name", ""),
            "current_stock": stock,
            "daily_velocity": round(velocity, 3),
            "velocity_std": round(velocity_std, 3),
            "days_until_stockout": days_until_stockout,
            "risk_level": risk_level,
            "reorder_suggestion": reorder_suggestion,
            "confidence": confidence,
            "data_source": data_source,
            "price": product.get("finalPrice") or product.get("price", 0),
            "average_rating": product.get("averageRating", 0),
        }

    def _get_velocity(
        self,
        product_id: str,
        shop_id: str,
        days: int,
    ) -> Tuple[float, float, str]:
        """
        Tính daily sales velocity.
        Returns: (mean_velocity, std_velocity, data_source)
        """
        # Try PostgreSQL first
        if self.pg.is_connected:
            df = self.pg.get_product_daily_sales(product_id, days)
            if not df.empty and "total_quantity" in df.columns:
                qty = df["total_quantity"].values.astype(float)
                if len(qty) > 0 and qty.sum() > 0:
                    return float(np.mean(qty)), float(np.std(qty)), "postgresql"

        # Fallback: MongoDB user_interactions
        interaction = self.db.user_interactions.find_one({"productId": product_id})
        if interaction:
            purchase_count = interaction.get("purchaseCount", 0)
            if purchase_count > 0:
                # Estimate: purchaseCount / (days since created or 30 days)
                created = interaction.get("createdAt")
                if created:
                    delta = (datetime.utcnow() - created).days
                    days_active = max(delta, 1)
                else:
                    days_active = 30
                velocity = purchase_count / days_active
                return velocity, velocity * 0.3, "mongodb_interactions"  # 30% std estimate

        return 0.0, 0.0, "no_data"

    @staticmethod
    def _classify_risk(
        stock: int,
        velocity: float,
        days_until_stockout: Optional[float],
    ) -> str:
        """Phân loại risk level."""
        if stock <= 0:
            return "CRITICAL"  # Đã hết hàng
        if velocity <= 0 or days_until_stockout is None:
            return "SAFE"  # Không bán được → không có risk (nhưng có thể overstock)
        if days_until_stockout <= 3:
            return "CRITICAL"
        if days_until_stockout <= 7:
            return "HIGH"
        if days_until_stockout <= 14:
            return "MEDIUM"
        if days_until_stockout <= 30:
            return "LOW"
        return "SAFE"

    @staticmethod
    def _compute_reorder_suggestion(
        velocity: float,
        days_until_stockout: Optional[float],
    ) -> Dict[str, Any]:
        """Gợi ý đặt hàng lại."""
        if velocity <= 0:
            return {"should_reorder": False, "reason": "Sản phẩm không có doanh số gần đây"}

        # Reorder point: đặt hàng khi còn đủ cho 7 ngày
        reorder_point = round(velocity * 7)
        # Suggested order: đủ cho 30 ngày
        suggested_quantity = round(velocity * 30)

        if days_until_stockout is not None and days_until_stockout <= 7:
            return {
                "should_reorder": True,
                "urgency": "URGENT" if days_until_stockout <= 3 else "HIGH",
                "reorder_point": reorder_point,
                "suggested_quantity": suggested_quantity,
                "reason": f"Ước tính hết hàng trong {days_until_stockout:.0f} ngày",
            }

        return {
            "should_reorder": days_until_stockout is not None and days_until_stockout <= 14,
            "urgency": "MEDIUM",
            "reorder_point": reorder_point,
            "suggested_quantity": suggested_quantity,
            "reason": f"Đặt hàng khi tồn kho còn dưới {reorder_point} đơn vị",
        }

    def get_system_stockout_report(self) -> Dict[str, Any]:
        """
        Báo cáo stockout risk toàn hệ thống (cho ADMIN).
        """
        critical = list(
            self.db.products.find(
                {"isPublished": True, "stockQuantity": {"$lte": 0}},
                {"_id": 1, "name": 1, "shopId": 1, "stockQuantity": 1}
            ).limit(50)
        )
        low_stock = list(
            self.db.products.find(
                {"isPublished": True, "stockQuantity": {"$gt": 0, "$lte": 10}},
                {"_id": 1, "name": 1, "shopId": 1, "stockQuantity": 1}
            ).limit(50)
        )

        return {
            "success": True,
            "out_of_stock_count": len(critical),
            "low_stock_count": len(low_stock),
            "out_of_stock_products": [
                {"id": str(p["_id"]), "name": p.get("name", ""), "shopId": p.get("shopId"), "stock": p.get("stockQuantity", 0)}
                for p in critical
            ],
            "low_stock_products": [
                {"id": str(p["_id"]), "name": p.get("name", ""), "shopId": p.get("shopId"), "stock": p.get("stockQuantity", 0)}
                for p in low_stock
            ],
        }
