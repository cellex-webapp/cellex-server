"""
PostgreSQL Database Client
--------------------------
Kết nối tới Supabase PostgreSQL (pooler connection).
Đọc dữ liệu từ các bảng: orders, order_items, users, shops, user_coupons.

Bảng → Spring Boot entity mapping:
  orders       → Order.java       (id UUID, user_id, shop_id, status, total_amount, is_paid, created_at)
  order_items  → OrderItem.java   (id UUID, order_id, product_id, quantity, price, subtotal)
  users        → User.java        (id UUID, email, full_name, role, total_spend, is_active, created_at)
  shops        → Shop.java        (id UUID, owner_id, shop_name, status, rating, created_at)
  user_coupons → UserCoupon.java  (id UUID, user_id, coupon_type, discount_value, status, redeemed_at)
"""

from __future__ import annotations

from typing import Optional, List
from contextlib import contextmanager

import pandas as pd
from sqlalchemy import create_engine, text, event
from sqlalchemy.pool import NullPool
from loguru import logger

from ..config import settings


class PostgresClient:
    """
    SQLAlchemy-based client cho Supabase PostgreSQL.
    
    Sử dụng Supabase Transaction Pooler (port 5432).
    Không dùng prepared statements để tránh lỗi với transaction pooler.
    """

    _instance: Optional[PostgresClient] = None
    _engine = None

    @classmethod
    def get_instance(cls) -> PostgresClient:
        """Singleton pattern."""
        if cls._instance is None:
            cls._instance = cls()
        return cls._instance

    def __init__(self):
        self._engine = None
        self._connected = False
        self._init_engine()

    def _init_engine(self):
        """Khởi tạo SQLAlchemy engine."""
        pg_url = settings.get_postgres_sqlalchemy_url()
        if not pg_url:
            logger.warning(
                "POSTGRES_URL not configured. PostgreSQL features (ML Heads with order data) disabled. "
                "Add POSTGRES_URL=postgresql+psycopg2://user:pass@host:port/dbname to .env"
            )
            return

        try:
            # Supabase transaction pooler configuration
            # - statement_cache_size=0: disable prepared statements
            # - pool_pre_ping=True: detect stale connections
            self._engine = create_engine(
                pg_url,
                pool_pre_ping=True,
                pool_size=settings.postgres_pool_size,
                max_overflow=settings.postgres_max_overflow,
                connect_args={
                    "options": "-c timezone=UTC",
                    "connect_timeout": 10,
                    "keepalives": 1,
                    "keepalives_idle": 30,
                    "keepalives_interval": 10,
                    "keepalives_count": 5,
                },
                execution_options={"no_parameters": True},
            )

            # Disable prepared statements for Supabase transaction pooler
            @event.listens_for(self._engine, "connect")
            def _set_pgbouncer_compat(dbapi_connection, connection_record):
                dbapi_connection.autocommit = True

            # Test connection
            with self._engine.connect() as conn:
                conn.execute(text("SELECT 1"))
            self._connected = True
            logger.info("PostgreSQL (Supabase) connected successfully")

        except Exception as e:
            logger.error(f"PostgreSQL connection failed: {e}")
            logger.warning("ML Heads will use MongoDB-only fallback mode")
            self._engine = None
            self._connected = False

    @property
    def is_connected(self) -> bool:
        return self._connected and self._engine is not None

    @contextmanager
    def connect(self):
        """Context manager cho database connection."""
        if not self.is_connected:
            raise ConnectionError("PostgreSQL not connected. Check POSTGRES_URL in .env")
        with self._engine.connect() as conn:
            yield conn

    def query_df(self, sql: str, params: Optional[dict] = None) -> pd.DataFrame:
        """
        Chạy SQL query và trả về DataFrame.
        
        Args:
            sql: SQL query string (dùng :param notation)
            params: Query parameters dict
            
        Returns:
            pandas DataFrame
        """
        if not self.is_connected:
            logger.warning("PostgreSQL not connected, returning empty DataFrame")
            return pd.DataFrame()

        try:
            with self._engine.connect() as conn:
                result = conn.execute(text(sql), params or {})
                df = pd.DataFrame(result.fetchall(), columns=result.keys())
                return df
        except Exception as e:
            logger.error(f"PostgreSQL query failed: {e}")
            logger.debug(f"SQL: {sql}")
            return pd.DataFrame()

    def query_scalar(self, sql: str, params: Optional[dict] = None):
        """Chạy query và trả về giá trị scalar đầu tiên."""
        df = self.query_df(sql, params)
        if df.empty:
            return None
        return df.iloc[0, 0]

    # ─── Domain-specific queries ────────────────────────────────────────────

    def get_orders_for_shop(self, shop_id: str, days: int = 90) -> pd.DataFrame:
        """
        Lấy đơn hàng của shop trong N ngày gần nhất.
        Trả về: id, user_id, shop_id, status, total_amount, is_paid, created_at
        """
        sql = """
            SELECT
                id::text,
                user_id::text,
                shop_id::text,
                status,
                total_amount,
                is_paid,
                created_at,
                discount_amount,
                coupon_code
            FROM orders
            WHERE shop_id = :shop_id::uuid
              AND created_at >= NOW() - INTERVAL ':days days'
            ORDER BY created_at DESC
        """
        # Workaround for interval interpolation
        sql = f"""
            SELECT
                id::text,
                user_id::text,
                shop_id::text,
                status,
                total_amount,
                is_paid,
                created_at,
                discount_amount,
                coupon_code
            FROM orders
            WHERE shop_id = :shop_id::uuid
              AND created_at >= NOW() - INTERVAL '{days} days'
            ORDER BY created_at DESC
        """
        return self.query_df(sql, {"shop_id": shop_id})

    def get_order_items_for_shop(self, shop_id: str, days: int = 90) -> pd.DataFrame:
        """
        Lấy order items của shop (join với orders).
        Trả về: order_id, product_id, product_name, quantity, price, subtotal, created_at, status
        """
        sql = f"""
            SELECT
                oi.order_id::text,
                oi.product_id,
                oi.product_name,
                oi.quantity,
                oi.price,
                oi.subtotal,
                o.created_at,
                o.status,
                o.is_paid,
                o.user_id::text
            FROM order_items oi
            INNER JOIN orders o ON o.id = oi.order_id
            WHERE o.shop_id = :shop_id::uuid
              AND o.created_at >= NOW() - INTERVAL '{days} days'
            ORDER BY o.created_at DESC
        """
        return self.query_df(sql, {"shop_id": shop_id})

    def get_all_order_items(self, days: int = 90) -> pd.DataFrame:
        """Lấy tất cả order items trong N ngày (cho system-level analysis)."""
        sql = f"""
            SELECT
                oi.order_id::text,
                oi.product_id,
                oi.product_name,
                oi.quantity,
                oi.price,
                oi.subtotal,
                o.shop_id::text,
                o.user_id::text,
                o.created_at,
                o.status,
                o.is_paid
            FROM order_items oi
            INNER JOIN orders o ON o.id = oi.order_id
            WHERE o.created_at >= NOW() - INTERVAL '{days} days'
              AND o.is_paid = true
              AND o.status = 'DELIVERED'
            ORDER BY o.created_at DESC
        """
        return self.query_df(sql, {})

    def get_user_order_history(self, user_id: str) -> pd.DataFrame:
        """Lấy lịch sử đơn hàng của user (cho churn risk)."""
        sql = """
            SELECT
                id::text,
                shop_id::text,
                status,
                total_amount,
                is_paid,
                created_at,
                coupon_code
            FROM orders
            WHERE user_id = :user_id::uuid
            ORDER BY created_at DESC
        """
        return self.query_df(sql, {"user_id": user_id})

    def get_all_users_rfm(self, days: int = 180) -> pd.DataFrame:
        """
        Tính RFM (Recency, Frequency, Monetary) cho tất cả users.
        Dùng cho Churn Risk training.
        """
        sql = f"""
            SELECT
                user_id::text                                           AS user_id,
                EXTRACT(EPOCH FROM (NOW() - MAX(created_at)))/86400    AS recency_days,
                COUNT(*)                                                AS frequency,
                SUM(total_amount)                                       AS monetary,
                COUNT(CASE WHEN status = 'CANCELLED' THEN 1 END)       AS cancelled_count,
                COUNT(CASE WHEN coupon_code IS NOT NULL THEN 1 END)     AS coupon_used_count,
                MIN(created_at)                                         AS first_order_at,
                MAX(created_at)                                         AS last_order_at
            FROM orders
            WHERE created_at >= NOW() - INTERVAL '{days} days'
              AND is_paid = true
            GROUP BY user_id
        """
        return self.query_df(sql, {})

    def get_shop_daily_sales(self, shop_id: str, days: int = 90) -> pd.DataFrame:
        """
        Tổng hợp doanh số theo ngày cho shop.
        Dùng cho Demand Forecast.
        """
        sql = f"""
            SELECT
                DATE(o.created_at)                    AS sale_date,
                oi.product_id,
                oi.product_name,
                SUM(oi.quantity)                      AS total_quantity,
                SUM(oi.subtotal)                      AS total_revenue,
                COUNT(DISTINCT o.id)                  AS order_count
            FROM order_items oi
            INNER JOIN orders o ON o.id = oi.order_id
            WHERE o.shop_id = :shop_id::uuid
              AND o.is_paid = true
              AND o.status = 'DELIVERED'
              AND o.created_at >= NOW() - INTERVAL '{days} days'
            GROUP BY DATE(o.created_at), oi.product_id, oi.product_name
            ORDER BY sale_date, total_quantity DESC
        """
        return self.query_df(sql, {"shop_id": shop_id})

    def get_product_daily_sales(self, product_id: str, days: int = 60) -> pd.DataFrame:
        """Tổng hợp doanh số theo ngày cho product cụ thể."""
        sql = f"""
            SELECT
                DATE(o.created_at)                    AS sale_date,
                SUM(oi.quantity)                      AS total_quantity,
                SUM(oi.subtotal)                      AS total_revenue,
                AVG(oi.price)                         AS avg_price
            FROM order_items oi
            INNER JOIN orders o ON o.id = oi.order_id
            WHERE oi.product_id = :product_id
              AND o.is_paid = true
              AND o.status = 'DELIVERED'
              AND o.created_at >= NOW() - INTERVAL '{days} days'
            GROUP BY DATE(o.created_at)
            ORDER BY sale_date
        """
        return self.query_df(sql, {"product_id": product_id})

    def get_coupon_effectiveness(self, shop_id: Optional[str] = None) -> pd.DataFrame:
        """
        Phân tích hiệu quả coupon.
        Dùng cho Coupon Uplift.
        """
        shop_filter = "AND o.shop_id = :shop_id::uuid" if shop_id else ""
        params: dict = {}
        if shop_id:
            params["shop_id"] = shop_id

        sql = f"""
            SELECT
                o.user_id::text                                         AS user_id,
                o.coupon_code,
                uc.coupon_type,
                uc.discount_value,
                o.total_amount,
                o.discount_amount,
                o.status,
                o.is_paid,
                o.created_at
            FROM orders o
            LEFT JOIN user_coupons uc ON uc.code = o.coupon_code
                AND uc.user_id = o.user_id::text
            WHERE o.created_at >= NOW() - INTERVAL '180 days'
            {shop_filter}
            ORDER BY o.created_at DESC
        """
        return self.query_df(sql, params)

    def get_user_purchase_counts(self, days: int = 90) -> pd.DataFrame:
        """Đếm số lần mua hàng per user (cho churn/uplift)."""
        sql = f"""
            SELECT
                user_id::text                           AS user_id,
                COUNT(*)                                AS order_count,
                SUM(total_amount)                       AS total_spent,
                AVG(total_amount)                       AS avg_order_value,
                MAX(created_at)                         AS last_order_at
            FROM orders
            WHERE is_paid = true
              AND created_at >= NOW() - INTERVAL '{days} days'
            GROUP BY user_id
            ORDER BY order_count DESC
        """
        return self.query_df(sql, {})

    def get_shop_revenue_summary(self, shop_id: str, days: int = 30) -> dict:
        """
        Tóm tắt doanh thu shop từ PostgreSQL (dữ liệu thực).
        """
        sql = f"""
            SELECT
                COUNT(*)                                AS total_orders,
                COUNT(CASE WHEN status='DELIVERED' AND is_paid=true THEN 1 END) AS completed_orders,
                COUNT(CASE WHEN status='CANCELLED' THEN 1 END) AS cancelled_orders,
                COUNT(CASE WHEN status='PENDING' THEN 1 END)   AS pending_orders,
                COALESCE(SUM(CASE WHEN status='DELIVERED' AND is_paid=true THEN total_amount END), 0) AS revenue,
                COALESCE(AVG(CASE WHEN status='DELIVERED' AND is_paid=true THEN total_amount END), 0) AS avg_order_value,
                COUNT(DISTINCT user_id)                 AS unique_customers
            FROM orders
            WHERE shop_id = :shop_id::uuid
              AND created_at >= NOW() - INTERVAL '{days} days'
        """
        df = self.query_df(sql, {"shop_id": shop_id})
        if df.empty:
            return {}
        row = df.iloc[0]
        return {
            "total_orders": int(row.get("total_orders", 0) or 0),
            "completed_orders": int(row.get("completed_orders", 0) or 0),
            "cancelled_orders": int(row.get("cancelled_orders", 0) or 0),
            "pending_orders": int(row.get("pending_orders", 0) or 0),
            "revenue": float(row.get("revenue", 0) or 0),
            "avg_order_value": float(row.get("avg_order_value", 0) or 0),
            "unique_customers": int(row.get("unique_customers", 0) or 0),
            "data_source": "postgresql",
        }


# Singleton instance
_postgres_client: Optional[PostgresClient] = None


def get_postgres_client() -> PostgresClient:
    """Lấy singleton PostgresClient instance."""
    global _postgres_client
    if _postgres_client is None:
        _postgres_client = PostgresClient()
    return _postgres_client
