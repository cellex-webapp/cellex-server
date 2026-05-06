"""
Churn Risk
----------
Dự báo nguy cơ churn của khách hàng (BUYER) và cửa hàng (SELLER).

Data sources:
  - PostgreSQL: orders (RFM data - Recency, Frequency, Monetary)
  - MongoDB: user_interactions (engagement signals)

Model:
  - RFM Scoring: mỗi chiều được score 1-5, tổng hợp thành churn score
  - Rule-based classification với optional sklearn LogisticRegression
  - Không cần training data có nhãn (unsupervised RFM)

Churn definition:
  - BUYER: Không có đơn hàng trong 60+ ngày (high risk), 30-60 ngày (medium)
  - SELLER: Shop không có đơn mới trong 30+ ngày
"""

from __future__ import annotations

from datetime import datetime, timedelta
from typing import Dict, Any, List, Optional

import numpy as np
import pandas as pd
from loguru import logger
from pymongo import MongoClient


class ChurnRiskService:
    """
    Dự báo nguy cơ churn dựa trên RFM model.
    """

    # Thresholds (days since last order)
    BUYER_THRESHOLDS = {
        "HIGH": 60,       # Không mua trong 60+ ngày → HIGH churn risk
        "MEDIUM": 30,     # 30-60 ngày → MEDIUM
        "LOW": 14,        # 14-30 ngày → LOW
    }

    SELLER_THRESHOLDS = {
        "HIGH": 30,       # Không có đơn mới trong 30+ ngày
        "MEDIUM": 14,
        "LOW": 7,
    }

    def __init__(self, postgres_client, mongo_client: MongoClient, mongo_db: str = "cellex"):
        self.pg = postgres_client
        self.db = mongo_client[mongo_db]

    def analyze_buyer(self, user_id: str) -> Dict[str, Any]:
        """
        Phân tích churn risk cho một user (BUYER).
        
        Returns:
            dict: risk_level, rfm_scores, recommendations, churn_probability
        """
        # Lấy order history từ PostgreSQL
        rfm = self._compute_buyer_rfm(user_id)
        if rfm is None:
            # Fallback: dùng user_interactions MongoDB
            rfm = self._compute_rfm_from_interactions(user_id)

        if rfm is None:
            return {
                "success": True,
                "user_id": user_id,
                "risk_level": "UNKNOWN",
                "message": "Không có đủ dữ liệu để phân tích churn risk",
                "churn_probability": 0.0,
            }

        risk_level, churn_prob = self._classify_buyer_churn(rfm)
        recommendations = self._get_buyer_recommendations(rfm, risk_level)

        return {
            "success": True,
            "user_id": user_id,
            "risk_level": risk_level,
            "churn_probability": churn_prob,
            "rfm": {
                "recency_days": round(rfm.get("recency_days", 0), 1),
                "frequency": int(rfm.get("frequency", 0)),
                "monetary": round(rfm.get("monetary", 0), 0),
                "avg_order_value": round(rfm.get("avg_order_value", 0), 0),
                "cancellation_rate": round(rfm.get("cancellation_rate", 0), 2),
            },
            "recommendations": recommendations,
            "data_source": rfm.get("data_source", "unknown"),
        }

    def analyze_all_buyers(self, limit: int = 100) -> Dict[str, Any]:
        """
        Phân tích churn risk cho tất cả users (ADMIN view).
        
        Returns:
            dict: Danh sách users theo risk level, summary stats
        """
        # Lấy RFM data từ PostgreSQL
        rfm_df = self._get_all_buyers_rfm()

        if rfm_df.empty:
            return {
                "success": False,
                "message": "Không có đủ dữ liệu PostgreSQL để phân tích churn",
            }

        results = []
        for _, row in rfm_df.iterrows():
            rfm = row.to_dict()
            rfm["data_source"] = "postgresql"
            risk_level, churn_prob = self._classify_buyer_churn(rfm)
            results.append({
                "user_id": rfm.get("user_id", ""),
                "risk_level": risk_level,
                "churn_probability": churn_prob,
                "recency_days": round(rfm.get("recency_days", 0), 1),
                "frequency": int(rfm.get("frequency", 0)),
                "monetary": round(rfm.get("monetary", 0), 0),
            })

        # Sort by churn probability desc
        results.sort(key=lambda x: x["churn_probability"], reverse=True)
        results = results[:limit]

        # Summary
        high_risk = [r for r in results if r["risk_level"] == "HIGH"]
        medium_risk = [r for r in results if r["risk_level"] == "MEDIUM"]
        low_risk = [r for r in results if r["risk_level"] == "LOW"]

        return {
            "success": True,
            "total_analyzed": len(results),
            "summary": {
                "high_risk": len(high_risk),
                "medium_risk": len(medium_risk),
                "low_risk": len(low_risk),
            },
            "high_risk_users": high_risk[:20],   # Top 20 most at risk
            "churn_rate_estimate": round(len(high_risk) / max(len(results), 1) * 100, 1),
        }

    def analyze_shop_churn_risk(self, shop_id: str) -> Dict[str, Any]:
        """
        Phân tích churn risk của shop (không có đơn hàng mới gần đây).
        Dành cho ADMIN xem sức khỏe các cửa hàng.
        """
        # Lấy thông tin đơn hàng gần nhất
        if self.pg.is_connected:
            df = self.pg.get_orders_for_shop(shop_id, days=90)
            if not df.empty and "created_at" in df.columns:
                df["created_at"] = pd.to_datetime(df["created_at"])
                last_order_date = df["created_at"].max()
                recency_days = (datetime.utcnow() - last_order_date.replace(tzinfo=None)).days
                total_orders = len(df)
                avg_daily_orders = total_orders / 90
            else:
                recency_days = 9999
                total_orders = 0
                avg_daily_orders = 0.0
        else:
            # Fallback: MongoDB user_interactions aggregation
            product_ids = [
                str(p["_id"]) for p in
                self.db.products.find({"shopId": shop_id}, {"_id": 1})
            ]
            interactions = list(
                self.db.user_interactions.find(
                    {"productId": {"$in": product_ids}, "purchaseCount": {"$gt": 0}},
                ).sort("updatedAt", -1).limit(1)
            )
            if interactions:
                last_inter = interactions[0]
                last_date = last_inter.get("updatedAt") or last_inter.get("createdAt")
                if last_date:
                    recency_days = (datetime.utcnow() - last_date).days
                else:
                    recency_days = 9999
            else:
                recency_days = 9999
            total_orders = 0
            avg_daily_orders = 0.0

        # Classify
        if recency_days > self.SELLER_THRESHOLDS["HIGH"]:
            risk_level = "HIGH"
            churn_prob = min(0.9, 0.5 + (recency_days - 30) / 100)
        elif recency_days > self.SELLER_THRESHOLDS["MEDIUM"]:
            risk_level = "MEDIUM"
            churn_prob = 0.3 + (recency_days - 14) / 60
        elif recency_days > self.SELLER_THRESHOLDS["LOW"]:
            risk_level = "LOW"
            churn_prob = 0.1 + recency_days / 100
        else:
            risk_level = "ACTIVE"
            churn_prob = max(0.0, recency_days / 50)

        return {
            "success": True,
            "shop_id": shop_id,
            "risk_level": risk_level,
            "churn_probability": round(churn_prob, 2),
            "days_since_last_order": recency_days if recency_days < 9999 else None,
            "total_orders_90d": total_orders,
            "avg_daily_orders": round(avg_daily_orders, 2),
            "recommendation": self._get_shop_recommendation(risk_level, recency_days),
        }

    # ─── Internal ───────────────────────────────────────────────────────────

    def _compute_buyer_rfm(self, user_id: str) -> Optional[Dict]:
        """Tính RFM từ PostgreSQL."""
        if not self.pg.is_connected:
            return None
        df = self.pg.get_user_order_history(user_id)
        if df.empty:
            return None

        df["created_at"] = pd.to_datetime(df["created_at"])
        now = datetime.utcnow()

        recency_days = (now - df["created_at"].max().replace(tzinfo=None)).days
        frequency = len(df)
        monetary = float(df["total_amount"].sum())
        avg_order_value = monetary / frequency if frequency > 0 else 0
        cancelled = len(df[df["status"] == "CANCELLED"])
        cancellation_rate = cancelled / frequency if frequency > 0 else 0

        coupon_used = df["coupon_code"].notna().sum()

        return {
            "recency_days": recency_days,
            "frequency": frequency,
            "monetary": monetary,
            "avg_order_value": avg_order_value,
            "cancellation_rate": cancellation_rate,
            "coupon_used_count": int(coupon_used),
            "data_source": "postgresql",
        }

    def _compute_rfm_from_interactions(self, user_id: str) -> Optional[Dict]:
        """Fallback: tính RFM từ MongoDB user_interactions."""
        interactions = list(
            self.db.user_interactions.find({"userId": user_id})
        )
        if not interactions:
            return None

        total_purchases = sum(i.get("purchaseCount", 0) for i in interactions)
        total_views = sum(i.get("viewCount", 0) for i in interactions)

        # Recency: lấy từ updatedAt của interaction gần nhất
        dates = [i.get("updatedAt") for i in interactions if i.get("updatedAt")]
        if dates:
            last_date = max(dates)
            recency_days = (datetime.utcnow() - last_date).days
        else:
            recency_days = 999

        return {
            "recency_days": recency_days,
            "frequency": total_purchases,
            "monetary": 0,  # Không có giá từ interactions
            "avg_order_value": 0,
            "cancellation_rate": 0,
            "coupon_used_count": 0,
            "total_views": total_views,
            "data_source": "mongodb_interactions",
        }

    def _get_all_buyers_rfm(self) -> pd.DataFrame:
        """Lấy RFM data toàn bộ users từ PostgreSQL."""
        if not self.pg.is_connected:
            return pd.DataFrame()
        return self.pg.get_all_users_rfm(days=180)

    @staticmethod
    def _classify_buyer_churn(rfm: Dict) -> tuple[str, float]:
        """
        Phân loại churn risk và tính probability.
        
        Weights:
          - Recency: 50% (quan trọng nhất)
          - Frequency: 30%
          - Monetary: 20%
        """
        recency = rfm.get("recency_days", 0)
        frequency = rfm.get("frequency", 1)
        monetary = rfm.get("monetary", 0)
        cancellation_rate = rfm.get("cancellation_rate", 0)

        # Recency score (0-1, higher = more churn risk)
        recency_score = min(1.0, recency / 90)  # Normalize to 90 days

        # Frequency score (inverse: low frequency = high churn risk)
        freq_score = max(0.0, 1 - min(frequency, 20) / 20)

        # Monetary score (inverse)
        mon_score = max(0.0, 1 - min(monetary, 5_000_000) / 5_000_000)

        # Composite churn probability
        churn_prob = (
            0.5 * recency_score +
            0.3 * freq_score +
            0.1 * mon_score +
            0.1 * cancellation_rate
        )
        churn_prob = min(1.0, round(churn_prob, 3))

        # Classify
        if recency > 60 or churn_prob > 0.7:
            return "HIGH", churn_prob
        if recency > 30 or churn_prob > 0.4:
            return "MEDIUM", churn_prob
        if recency > 14 or churn_prob > 0.2:
            return "LOW", churn_prob
        return "ACTIVE", churn_prob

    @staticmethod
    def _get_buyer_recommendations(rfm: Dict, risk_level: str) -> List[str]:
        """Gợi ý hành động dựa trên risk level."""
        recommendations = []
        recency = rfm.get("recency_days", 0)
        frequency = rfm.get("frequency", 0)
        monetary = rfm.get("monetary", 0)

        if risk_level == "HIGH":
            recommendations.append("Gửi email win-back với coupon ưu đãi đặc biệt (20-30%)")
            if monetary > 1_000_000:
                recommendations.append("User có giá trị cao - ưu tiên chăm sóc cá nhân")
            recommendations.append("Push notification về sản phẩm đã xem gần đây")

        elif risk_level == "MEDIUM":
            recommendations.append("Gửi reminder về sản phẩm yêu thích hoặc xem gần đây")
            recommendations.append(f"Gợi ý coupon nhỏ ({10 if frequency > 5 else 15}%)")

        elif risk_level == "LOW":
            recommendations.append("Giữ engagement qua newsletter và flash sale")
            if frequency == 1:
                recommendations.append("Khuyến khích mua lần 2 - tỷ lệ giữ chân tăng cao sau lần thứ 2")

        return recommendations

    @staticmethod
    def _get_shop_recommendation(risk_level: str, recency_days: int) -> str:
        """Gợi ý cho shop dựa trên risk level."""
        if risk_level == "HIGH":
            return (f"Shop không có đơn hàng trong {recency_days} ngày. "
                    "Cần kiểm tra: sản phẩm có published không, giá có cạnh tranh, review có tốt không")
        if risk_level == "MEDIUM":
            return "Shop hoạt động chậm. Xem xét chạy flash sale hoặc thêm coupon để kích cầu"
        if risk_level == "LOW":
            return "Shop hoạt động bình thường nhưng có thể tăng thêm"
        return "Shop đang hoạt động tốt"
