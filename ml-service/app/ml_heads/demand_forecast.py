"""
Demand Forecast
---------------
Dự báo nhu cầu (số lượng bán) theo sản phẩm và theo ngày.
Data sources:
  - PostgreSQL (orders + order_items): doanh số thực tế theo ngày
  - MongoDB (products): thông tin sản phẩm (stockQuantity, categoryId)

Approach:
  - Kết hợp trend + seasonality (day-of-week) + rolling average
  - Fallback về moving average khi không có đủ data
  - Không cần training phức tạp, hoạt động tốt với ít dữ liệu
"""

from __future__ import annotations

from datetime import date, datetime, timedelta
from typing import Dict, Any, List, Optional

import numpy as np
import pandas as pd
from loguru import logger
from pymongo import MongoClient


class DemandForecastService:
    """
    Dự báo nhu cầu sản phẩm.
    
    Thuật toán:
    1. Lấy doanh số theo ngày từ PostgreSQL (orders + order_items)
    2. Tính weighted moving average (7-day, 14-day, 30-day)
    3. Áp dụng day-of-week seasonality
    4. Extrapolate trend tuyến tính
    """

    def __init__(self, postgres_client, mongo_client: MongoClient, mongo_db: str = "cellex"):
        self.pg = postgres_client
        self.db = mongo_client[mongo_db]

    def forecast_product(
        self,
        product_id: str,
        shop_id: str,
        forecast_days: int = 14,
        history_days: int = 60,
    ) -> Dict[str, Any]:
        """
        Dự báo nhu cầu cho một sản phẩm cụ thể.
        
        Args:
            product_id: MongoDB product ID
            shop_id: Shop UUID
            forecast_days: Số ngày cần dự báo (7, 14, 30)
            history_days: Số ngày lịch sử sử dụng
            
        Returns:
            dict: {product_id, forecast, daily_forecast, summary}
        """
        # Lấy thông tin sản phẩm từ MongoDB
        product = self.db.products.find_one({"_id": product_id})
        if not product:
            return {"success": False, "message": f"Product {product_id} not found"}

        product_name = product.get("name", "")
        stock_quantity = product.get("stockQuantity", 0) or 0

        # Lấy doanh số lịch sử từ PostgreSQL
        history_df = self._get_product_history(product_id, history_days)

        # Tính dự báo
        forecast = self._compute_forecast(history_df, forecast_days)

        # Phân tích stock
        total_forecasted = sum(f["quantity"] for f in forecast)
        days_until_stockout = None
        if stock_quantity > 0:
            cumulative = 0
            for i, f in enumerate(forecast):
                cumulative += f["quantity"]
                if cumulative >= stock_quantity:
                    days_until_stockout = i + 1
                    break

        risk_level = "LOW"
        if days_until_stockout is not None:
            if days_until_stockout <= 3:
                risk_level = "CRITICAL"
            elif days_until_stockout <= 7:
                risk_level = "HIGH"
            elif days_until_stockout <= 14:
                risk_level = "MEDIUM"

        return {
            "success": True,
            "product_id": product_id,
            "product_name": product_name,
            "shop_id": shop_id,
            "forecast_days": forecast_days,
            "current_stock": stock_quantity,
            "total_forecasted_demand": round(total_forecasted, 1),
            "days_until_stockout": days_until_stockout,
            "stockout_risk": risk_level,
            "daily_forecast": forecast,
            "avg_daily_demand": round(total_forecasted / forecast_days, 2),
            "history_days_used": history_days,
            "data_points": len(history_df),
        }

    def forecast_shop(
        self,
        shop_id: str,
        forecast_days: int = 14,
        history_days: int = 90,
        top_n_products: int = 10,
    ) -> Dict[str, Any]:
        """
        Dự báo nhu cầu cho toàn bộ shop.
        
        Returns:
            dict: Tổng hợp dự báo theo product, shop-level trend
        """
        # Lấy doanh số lịch sử theo product từ PostgreSQL
        if self.pg.is_connected:
            df = self.pg.get_shop_daily_sales(shop_id, history_days)
        else:
            # Fallback: dùng user_interactions từ MongoDB
            df = self._get_interactions_fallback(shop_id, history_days)

        if df.empty:
            return {
                "success": False,
                "shop_id": shop_id,
                "message": "Không có đủ dữ liệu doanh số để dự báo",
            }

        # Tổng doanh số theo ngày (shop-level)
        shop_daily = (
            df.groupby("sale_date")["total_quantity"]
            .sum()
            .reset_index()
            .rename(columns={"sale_date": "date"})
        )
        shop_daily["date"] = pd.to_datetime(shop_daily["date"])

        shop_forecast = self._compute_forecast(shop_daily, forecast_days, col="total_quantity")

        # Top products
        if "product_id" in df.columns:
            top_products_df = (
                df.groupby("product_id")["total_quantity"]
                .sum()
                .nlargest(top_n_products)
                .reset_index()
            )
            top_products = top_products_df.to_dict("records")
        else:
            top_products = []

        return {
            "success": True,
            "shop_id": shop_id,
            "forecast_days": forecast_days,
            "shop_level_forecast": {
                "total_forecasted_units": round(sum(f["quantity"] for f in shop_forecast), 0),
                "avg_daily_units": round(
                    sum(f["quantity"] for f in shop_forecast) / max(forecast_days, 1), 2
                ),
                "daily_forecast": shop_forecast,
            },
            "top_products_by_demand": top_products,
            "history_days_used": history_days,
        }

    # ─── Internal helpers ───────────────────────────────────────────────────

    def _get_product_history(self, product_id: str, days: int) -> pd.DataFrame:
        """Lấy lịch sử bán hàng theo ngày cho product."""
        if self.pg.is_connected:
            df = self.pg.get_product_daily_sales(product_id, days)
            if not df.empty:
                df["date"] = pd.to_datetime(df["sale_date"])
                return df[["date", "total_quantity"]].copy()

        # Fallback: user_interactions MongoDB (purchaseCount by date is not tracked day by day,
        # so we return empty to trigger moving average with overall data)
        interaction = self.db.user_interactions.find_one({"productId": product_id})
        if interaction:
            # Spread purchaseCount evenly over the last `days` days as approximation
            purchase_count = interaction.get("purchaseCount", 0)
            if purchase_count > 0:
                dates = [datetime.utcnow() - timedelta(days=i) for i in range(days)]
                daily_avg = purchase_count / days
                return pd.DataFrame({
                    "date": pd.to_datetime(dates),
                    "total_quantity": [daily_avg] * days,
                })
        return pd.DataFrame(columns=["date", "total_quantity"])

    def _get_interactions_fallback(self, shop_id: str, days: int) -> pd.DataFrame:
        """Fallback dùng MongoDB user_interactions khi không có PostgreSQL."""
        products = list(self.db.products.find({"shopId": shop_id, "isPublished": True}, {"_id": 1}))
        product_ids = [str(p["_id"]) for p in products]
        if not product_ids:
            return pd.DataFrame()

        interactions = list(self.db.user_interactions.find({"productId": {"$in": product_ids}}))
        if not interactions:
            return pd.DataFrame()

        rows = []
        for inter in interactions:
            rows.append({
                "product_id": inter.get("productId", ""),
                "sale_date": datetime.utcnow().date(),
                "total_quantity": inter.get("purchaseCount", 0),
            })
        return pd.DataFrame(rows)

    def _compute_forecast(
        self,
        df: pd.DataFrame,
        forecast_days: int,
        col: str = "total_quantity",
    ) -> List[Dict]:
        """
        Tính dự báo từ time series.
        
        Thuật toán:
        1. Fill missing dates với 0
        2. Tính weighted moving average (7-day WMA)
        3. Tính linear trend
        4. Áp dụng day-of-week seasonality factor
        """
        if df.empty or col not in df.columns:
            # Không có dữ liệu → dự báo 0
            return [
                {
                    "date": (datetime.utcnow() + timedelta(days=i + 1)).strftime("%Y-%m-%d"),
                    "quantity": 0.0,
                    "confidence": "low",
                }
                for i in range(forecast_days)
            ]

        df = df.copy()
        df["date"] = pd.to_datetime(df["date"])
        df = df.sort_values("date")

        # Fill missing dates
        if len(df) > 1:
            date_range = pd.date_range(start=df["date"].min(), end=df["date"].max(), freq="D")
            df = df.set_index("date").reindex(date_range, fill_value=0).reset_index()
            df.columns = ["date", col]

        qty = df[col].values.astype(float)

        # Weighted moving average (7-day)
        window = min(7, len(qty))
        weights = np.arange(1, window + 1, dtype=float)
        if len(qty) >= window:
            wma = np.convolve(qty, weights / weights.sum(), mode="valid")
            base_demand = wma[-1]
        else:
            base_demand = float(np.mean(qty)) if len(qty) > 0 else 0.0

        # Linear trend (slope per day)
        if len(qty) >= 7:
            x = np.arange(len(qty))
            slope = np.polyfit(x, qty, 1)[0]
        else:
            slope = 0.0

        # Day-of-week seasonality (from historical data)
        dow_factors = self._compute_dow_factors(df, col)

        # Generate forecast
        last_date = df["date"].max()
        forecast = []
        for i in range(1, forecast_days + 1):
            pred_date = last_date + timedelta(days=i)
            dow = pred_date.dayofweek  # 0=Mon, 6=Sun
            trend_adj = base_demand + slope * i
            seasonal_adj = trend_adj * dow_factors.get(dow, 1.0)
            quantity = max(0.0, round(seasonal_adj, 2))

            # Confidence based on data availability
            confidence = "high" if len(qty) >= 30 else "medium" if len(qty) >= 7 else "low"

            forecast.append({
                "date": pred_date.strftime("%Y-%m-%d"),
                "day_of_week": pred_date.strftime("%A"),
                "quantity": quantity,
                "confidence": confidence,
            })

        return forecast

    @staticmethod
    def _compute_dow_factors(df: pd.DataFrame, col: str) -> Dict[int, float]:
        """Tính seasonal factor theo day-of-week."""
        if df.empty or col not in df.columns:
            return {}
        df = df.copy()
        df["dow"] = pd.to_datetime(df["date"]).dt.dayofweek
        dow_avg = df.groupby("dow")[col].mean()
        global_avg = df[col].mean()
        if global_avg == 0:
            return {}
        factors = {int(dow): float(avg / global_avg) for dow, avg in dow_avg.items()}
        return factors
