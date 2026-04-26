"""
Coupon Uplift
-------------
Dự báo uplift (tăng thêm doanh số) khi áp dụng coupon.

Data sources:
  - PostgreSQL: orders (coupon_code, discount_amount, total_amount, user_id)
  - PostgreSQL: user_coupons (coupon_type, discount_value, status)
  - MongoDB: user_interactions (view/cart signals cho targeting)

Approach: Two-Group Comparison + Feature Scoring
  - Group A (Treatment): Users đã nhận coupon
  - Group B (Control): Users không có coupon
  - Uplift = Conversion(A) - Conversion(B)
  
  Coupon candidates scoring:
  - High view + Low purchase = High uplift potential
  - Price elasticity estimate từ discount history
  
Output:
  - effectiveness: % tăng doanh số khi dùng coupon
  - best_coupon_type: PERCENTAGE / FIXED_AMOUNT
  - suggested_discount: giá trị discount tối ưu
  - target_users: user segments phù hợp
"""

from __future__ import annotations

from datetime import datetime, timedelta
from typing import Dict, Any, List, Optional

import numpy as np
import pandas as pd
from loguru import logger
from pymongo import MongoClient


class CouponUpliftService:
    """
    Phân tích uplift của coupon dựa trên historical data.
    """

    def __init__(self, postgres_client, mongo_client: MongoClient, mongo_db: str = "cellex"):
        self.pg = postgres_client
        self.db = mongo_client[mongo_db]

    def analyze_coupon_effectiveness(
        self,
        shop_id: Optional[str] = None,
        days: int = 90,
    ) -> Dict[str, Any]:
        """
        Phân tích hiệu quả coupon của shop / toàn hệ thống.
        
        Returns:
            dict: uplift_rate, best_discount_range, coupon_roi, recommendations
        """
        if self.pg.is_connected:
            df = self.pg.get_coupon_effectiveness(shop_id)
        else:
            return self._mongodb_only_analysis(shop_id)

        if df.empty:
            return {
                "success": True,
                "shop_id": shop_id,
                "message": "Chưa có dữ liệu coupon để phân tích",
                "uplift_analysis": None,
            }

        analysis = self._analyze_coupon_df(df)

        return {
            "success": True,
            "shop_id": shop_id,
            "analysis_days": days,
            "total_orders_analyzed": len(df),
            **analysis,
        }

    def predict_user_uplift(
        self,
        user_id: str,
        shop_id: Optional[str] = None,
    ) -> Dict[str, Any]:
        """
        Dự báo khả năng uplift nếu gửi coupon cho user cụ thể.
        
        Returns:
            dict: uplift_score, recommended_coupon, reason
        """
        # 1. User history từ PostgreSQL
        rfm = self._get_user_rfm(user_id)

        # 2. User interaction signals từ MongoDB
        interaction_signals = self._get_interaction_signals(user_id, shop_id)

        # 3. Tính uplift score
        uplift_score, recommended_coupon, reason = self._score_user_uplift(
            rfm, interaction_signals, shop_id
        )

        return {
            "success": True,
            "user_id": user_id,
            "shop_id": shop_id,
            "uplift_score": uplift_score,  # 0.0 - 1.0
            "uplift_tier": self._tier_from_score(uplift_score),
            "recommended_coupon": recommended_coupon,
            "reason": reason,
        }

    def get_high_uplift_products(self, shop_id: str, limit: int = 10) -> Dict[str, Any]:
        """
        Tìm sản phẩm có high uplift potential (nhiều view nhưng ít mua).
        Đây là target tốt nhất cho coupon.
        """
        products = list(
            self.db.products.find(
                {"shopId": shop_id, "isPublished": True},
                {"_id": 1, "name": 1, "price": 1, "finalPrice": 1, "purchaseCount": 1, "reviewCount": 1}
            )
        )
        if not products:
            return {"success": False, "message": "Không có sản phẩm nào"}

        product_ids = [str(p["_id"]) for p in products]
        product_map = {str(p["_id"]): p for p in products}

        # Interaction signals
        interactions = list(
            self.db.user_interactions.find({"productId": {"$in": product_ids}})
        )

        inter_map: Dict[str, Dict] = {}
        for inter in interactions:
            pid = inter.get("productId", "")
            if pid not in inter_map:
                inter_map[pid] = {"views": 0, "carts": 0, "purchases": 0}
            inter_map[pid]["views"] += inter.get("viewCount", 0)
            inter_map[pid]["carts"] += inter.get("cartCount", 0)
            inter_map[pid]["purchases"] += inter.get("purchaseCount", 0)

        scored_products = []
        for pid in product_ids:
            p = product_map[pid]
            inter = inter_map.get(pid, {"views": 0, "carts": 0, "purchases": 0})
            views = inter["views"]
            carts = inter["carts"]
            purchases = inter["purchases"]

            # View-to-Cart rate
            view_to_cart = carts / views if views > 0 else 0
            # Cart-to-Purchase rate
            cart_to_purchase = purchases / carts if carts > 0 else 0
            # Overall conversion
            overall_conversion = purchases / views if views > 0 else 0

            # Uplift potential: high views + low conversion = high potential
            uplift_potential = 0.0
            if views >= 10:
                # Views đủ nhiều → có traffic
                conversion_gap = max(0, 0.1 - overall_conversion)  # Gap từ benchmark 10%
                cart_intent = view_to_cart  # Đã có ý định mua (add to cart)
                uplift_potential = min(1.0, conversion_gap * 5 + cart_intent * 0.3)

            price = p.get("finalPrice") or p.get("price", 0) or 0
            # Suggested discount: cao hơn cho sản phẩm đắt tiền
            suggested_discount = self._suggest_discount(price, overall_conversion)

            scored_products.append({
                "product_id": pid,
                "product_name": p.get("name", ""),
                "price": price,
                "views": views,
                "carts": carts,
                "purchases": purchases,
                "view_to_cart_rate": round(view_to_cart * 100, 2),
                "cart_to_purchase_rate": round(cart_to_purchase * 100, 2),
                "overall_conversion_rate": round(overall_conversion * 100, 2),
                "uplift_potential_score": round(uplift_potential, 3),
                "suggested_discount_pct": suggested_discount,
                "reason": self._explain_uplift(views, overall_conversion, view_to_cart),
            })

        # Sort by uplift potential desc
        scored_products.sort(key=lambda x: x["uplift_potential_score"], reverse=True)
        top_products = scored_products[:limit]

        return {
            "success": True,
            "shop_id": shop_id,
            "total_analyzed": len(scored_products),
            "high_uplift_products": top_products,
            "avg_conversion_rate": round(
                np.mean([p["overall_conversion_rate"] for p in scored_products]) if scored_products else 0,
                2
            ),
        }

    # ─── Internal ───────────────────────────────────────────────────────────

    def _analyze_coupon_df(self, df: pd.DataFrame) -> Dict[str, Any]:
        """Phân tích DataFrame coupon effectiveness."""
        df = df.copy()
        df["has_coupon"] = df["coupon_code"].notna() & (df["coupon_code"] != "")
        df["is_completed"] = (df["status"] == "DELIVERED") & (df["is_paid"] == True)

        # Treatment group (có coupon)
        treatment = df[df["has_coupon"]]
        control = df[~df["has_coupon"]]

        treatment_rate = len(treatment[treatment["is_completed"]]) / max(len(treatment), 1)
        control_rate = len(control[control["is_completed"]]) / max(len(control), 1)
        uplift_rate = treatment_rate - control_rate

        # Average discount
        discount_df = treatment[treatment["discount_amount"] > 0]
        avg_discount_amount = float(discount_df["discount_amount"].mean()) if not discount_df.empty else 0
        avg_order_with_coupon = float(treatment["total_amount"].mean()) if not treatment.empty else 0
        avg_order_without_coupon = float(control["total_amount"].mean()) if not control.empty else 0

        # Best coupon type
        if not treatment.empty and "coupon_type" in treatment.columns:
            type_completion = treatment.groupby("coupon_type")["is_completed"].mean()
            best_type = str(type_completion.idxmax()) if not type_completion.empty else "PERCENTAGE"
        else:
            best_type = "PERCENTAGE"

        # ROI: (additional revenue from treatment) / (total discount given)
        if avg_discount_amount > 0 and avg_order_with_coupon > avg_order_without_coupon:
            roi = (avg_order_with_coupon - avg_order_without_coupon) / avg_discount_amount
        else:
            roi = 0.0

        return {
            "uplift_analysis": {
                "treatment_completion_rate": round(treatment_rate * 100, 2),
                "control_completion_rate": round(control_rate * 100, 2),
                "uplift_rate_pct": round(uplift_rate * 100, 2),
                "coupon_orders_count": len(treatment),
                "no_coupon_orders_count": len(control),
            },
            "coupon_economics": {
                "avg_discount_amount": round(avg_discount_amount, 0),
                "avg_order_with_coupon": round(avg_order_with_coupon, 0),
                "avg_order_without_coupon": round(avg_order_without_coupon, 0),
                "order_value_lift": round(avg_order_with_coupon - avg_order_without_coupon, 0),
                "estimated_roi": round(roi, 2),
            },
            "recommendation": {
                "best_coupon_type": best_type,
                "suggested_discount_pct": self._suggest_optimal_discount(uplift_rate, avg_discount_amount),
                "notes": self._generate_coupon_notes(uplift_rate, roi),
            },
        }

    def _get_user_rfm(self, user_id: str) -> Dict:
        """Lấy RFM của user."""
        if not self.pg.is_connected:
            return {}
        df = self.pg.get_user_order_history(user_id)
        if df.empty:
            return {}
        df["created_at"] = pd.to_datetime(df["created_at"])
        return {
            "recency_days": (datetime.utcnow() - df["created_at"].max().replace(tzinfo=None)).days,
            "frequency": len(df),
            "monetary": float(df["total_amount"].sum()),
            "has_used_coupon": df["coupon_code"].notna().any(),
        }

    def _get_interaction_signals(self, user_id: str, shop_id: Optional[str]) -> Dict:
        """Lấy interaction signals từ MongoDB."""
        query: Dict = {"userId": user_id}
        if shop_id:
            product_ids = [str(p["_id"]) for p in self.db.products.find({"shopId": shop_id}, {"_id": 1})]
            if product_ids:
                query["productId"] = {"$in": product_ids}

        interactions = list(self.db.user_interactions.find(query))
        if not interactions:
            return {}

        total_views = sum(i.get("viewCount", 0) for i in interactions)
        total_carts = sum(i.get("cartCount", 0) for i in interactions)
        total_purchases = sum(i.get("purchaseCount", 0) for i in interactions)
        high_intent = [i for i in interactions if i.get("cartCount", 0) > 0 and i.get("purchaseCount", 0) == 0]

        return {
            "total_views": total_views,
            "total_carts": total_carts,
            "total_purchases": total_purchases,
            "cart_abandonment_count": len(high_intent),
            "view_to_cart_rate": total_carts / total_views if total_views > 0 else 0,
        }

    def _score_user_uplift(
        self,
        rfm: Dict,
        signals: Dict,
        shop_id: Optional[str],
    ) -> tuple[float, Dict, str]:
        """Tính uplift score cho user."""
        score = 0.0
        reasons = []

        # RFM signals
        recency = rfm.get("recency_days", 999)
        frequency = rfm.get("frequency", 0)

        if 14 <= recency <= 60:
            score += 0.3
            reasons.append("Chưa mua gần đây, coupon có thể kéo trở lại")

        if frequency >= 2:
            score += 0.2
            reasons.append("Đã từng mua nhiều lần, có loyalty")

        # Interaction signals
        cart_abandonment = signals.get("cart_abandonment_count", 0)
        if cart_abandonment > 0:
            score += 0.4
            reasons.append(f"Đã add {cart_abandonment} sản phẩm vào giỏ nhưng chưa mua")

        view_rate = signals.get("view_to_cart_rate", 0)
        if view_rate > 0.1:
            score += 0.1
            reasons.append("Tỷ lệ xem-đến-giỏ cao")

        score = min(1.0, round(score, 2))

        # Recommended coupon
        monetary = rfm.get("monetary", 0)
        if monetary > 2_000_000 or frequency >= 5:
            recommended_coupon = {"type": "PERCENTAGE", "value": 10, "reason": "Loyal customer"}
        elif cart_abandonment > 2:
            recommended_coupon = {"type": "PERCENTAGE", "value": 15, "reason": "Cart abandonment recovery"}
        elif recency > 30:
            recommended_coupon = {"type": "FIXED_AMOUNT", "value": 50_000, "reason": "Win-back"}
        else:
            recommended_coupon = {"type": "PERCENTAGE", "value": 5, "reason": "Engagement boost"}

        reason = "; ".join(reasons) if reasons else "Không đủ tín hiệu để đánh giá"
        return score, recommended_coupon, reason

    def _mongodb_only_analysis(self, shop_id: Optional[str]) -> Dict[str, Any]:
        """Fallback analysis khi không có PostgreSQL."""
        query: Dict = {"isPublished": True}
        if shop_id:
            query["shopId"] = shop_id

        products = list(self.db.products.find(query, {"_id": 1, "purchaseCount": 1, "reviewCount": 1}))
        product_ids = [str(p["_id"]) for p in products]

        if not product_ids:
            return {"success": False, "message": "Không có sản phẩm"}

        # Aggregate views và purchases từ user_interactions
        pipeline = [
            {"$match": {"productId": {"$in": product_ids}}},
            {
                "$group": {
                    "_id": None,
                    "total_views": {"$sum": "$viewCount"},
                    "total_carts": {"$sum": "$cartCount"},
                    "total_purchases": {"$sum": "$purchaseCount"},
                }
            },
        ]
        agg = list(self.db.user_interactions.aggregate(pipeline))
        row = agg[0] if agg else {}

        total_views = row.get("total_views", 0)
        total_purchases = row.get("total_purchases", 0)
        conversion_rate = total_purchases / total_views if total_views > 0 else 0

        return {
            "success": True,
            "shop_id": shop_id,
            "data_source": "mongodb_only",
            "note": "PostgreSQL không kết nối được. Phân tích dựa trên user_interactions MongoDB.",
            "interaction_summary": {
                "total_views": total_views,
                "total_carts": row.get("total_carts", 0),
                "total_purchases": total_purchases,
                "conversion_rate": round(conversion_rate * 100, 2),
            },
            "recommendation": {
                "notes": "Kết nối PostgreSQL để có phân tích coupon uplift chi tiết hơn",
                "suggested_discount_pct": 10 if conversion_rate < 0.05 else 5,
            },
        }

    @staticmethod
    def _suggest_discount(price: float, conversion_rate: float) -> int:
        """Gợi ý mức discount (%)."""
        if price > 5_000_000:
            return 10  # Sản phẩm đắt: giảm ít nhưng vẫn hiệu quả
        if price > 1_000_000:
            return 15 if conversion_rate < 0.02 else 10
        return 20 if conversion_rate < 0.05 else 10

    @staticmethod
    def _suggest_optimal_discount(uplift_rate: float, avg_discount: float) -> int:
        """Gợi ý discount tối ưu từ historical data."""
        if uplift_rate <= 0:
            return 10  # Default
        if uplift_rate > 0.2:
            return 5   # Uplift tốt, không cần giảm nhiều
        if uplift_rate > 0.1:
            return 10
        return 15      # Uplift thấp, cần giảm nhiều hơn

    @staticmethod
    def _generate_coupon_notes(uplift_rate: float, roi: float) -> List[str]:
        """Tạo ghi chú phân tích."""
        notes = []
        if uplift_rate > 0.1:
            notes.append("Coupon có hiệu quả tốt trong việc tăng tỷ lệ hoàn thành đơn hàng")
        elif uplift_rate > 0:
            notes.append("Coupon có hiệu quả nhẹ, cân nhắc tăng giá trị ưu đãi")
        else:
            notes.append("Coupon chưa cải thiện tỷ lệ chuyển đổi, cần xem xét chiến lược mới")

        if roi > 2:
            notes.append(f"ROI cao ({roi:.1f}x): đầu tư vào coupon rất hiệu quả")
        elif roi > 1:
            notes.append(f"ROI dương ({roi:.1f}x): coupon mang lại lợi nhuận")
        elif roi > 0:
            notes.append("ROI thấp: coupon hòa vốn, cần tối ưu giá trị ưu đãi")

        return notes

    @staticmethod
    def _tier_from_score(score: float) -> str:
        """Chuyển uplift score thành tier."""
        if score >= 0.7:
            return "HIGH"
        if score >= 0.4:
            return "MEDIUM"
        if score >= 0.1:
            return "LOW"
        return "MINIMAL"

    @staticmethod
    def _explain_uplift(views: int, conversion: float, view_to_cart: float) -> str:
        """Giải thích tại sao sản phẩm có uplift potential."""
        if views < 10:
            return "Ít lượt xem, cần tăng exposure trước"
        if view_to_cart > 0.1 and conversion < 0.03:
            return "Nhiều người add vào giỏ nhưng không mua - coupon có thể giải quyết price barrier"
        if views > 50 and conversion < 0.02:
            return "Traffic cao nhưng conversion rất thấp - giá có thể quá cao"
        if conversion < 0.05:
            return "Conversion thấp hơn benchmark (5%) - coupon có thể tăng chuyển đổi"
        return "Sản phẩm có tiềm năng cải thiện với ưu đãi phù hợp"
