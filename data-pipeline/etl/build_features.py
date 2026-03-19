"""
Build Features ETL
------------------
Xay dung features tu du lieu trong Gold layer.

Module nay thuc hien:
- Doc du lieu tu PostgreSQL (products table)
- Tao cac features cho recommendation/forecasting:
  - product_daily_metrics: views, sales, revenue theo ngay
  - product_features: features cho ML models
- Luu vao PostgreSQL

Medallion Architecture:
- Input: Gold (products - PostgreSQL)
- Output: Gold (feature tables - PostgreSQL)

Usage:
    python -m etl.build_features
"""

import os
import sys
from datetime import datetime, timezone, timedelta
from pathlib import Path
from typing import Any
import random

import psycopg
from psycopg.rows import dict_row
from dotenv import load_dotenv
from loguru import logger

# Load environment variables
project_root = Path(__file__).parent.parent
load_dotenv(project_root / ".env")

# Configure loguru
log_dir = project_root / "logs"
log_dir.mkdir(exist_ok=True)
logger.add(
    log_dir / "build_features_{time:YYYY-MM-DD}.log",
    rotation="1 day",
    retention="7 days",
    level="INFO",
)


# SQL Statements
CREATE_PRODUCT_DAILY_METRICS_TABLE = """
CREATE TABLE IF NOT EXISTS product_daily_metrics (
    id SERIAL PRIMARY KEY,
    product_id INTEGER NOT NULL REFERENCES products(id),
    date DATE NOT NULL,
    views INTEGER DEFAULT 0,
    clicks INTEGER DEFAULT 0,
    add_to_carts INTEGER DEFAULT 0,
    purchases INTEGER DEFAULT 0,
    revenue DECIMAL(15, 2) DEFAULT 0,
    avg_rating_change DECIMAL(3, 2) DEFAULT 0,
    reviews_count INTEGER DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(product_id, date)
);

-- Create indexes
CREATE INDEX IF NOT EXISTS idx_product_daily_metrics_product_id
    ON product_daily_metrics(product_id);
CREATE INDEX IF NOT EXISTS idx_product_daily_metrics_date
    ON product_daily_metrics(date);
CREATE INDEX IF NOT EXISTS idx_product_daily_metrics_product_date
    ON product_daily_metrics(product_id, date);
"""

CREATE_PRODUCT_FEATURES_TABLE = """
CREATE TABLE IF NOT EXISTS product_features (
    id SERIAL PRIMARY KEY,
    product_id INTEGER NOT NULL REFERENCES products(id) UNIQUE,

    -- Price features
    price_normalized DECIMAL(5, 4),
    discount_percentage DECIMAL(5, 2),
    price_tier VARCHAR(20),

    -- Popularity features
    popularity_score DECIMAL(10, 4),
    engagement_rate DECIMAL(5, 4),
    conversion_rate DECIMAL(5, 4),

    -- Rolling metrics (7 days)
    views_7d INTEGER DEFAULT 0,
    purchases_7d INTEGER DEFAULT 0,
    revenue_7d DECIMAL(15, 2) DEFAULT 0,

    -- Rolling metrics (30 days)
    views_30d INTEGER DEFAULT 0,
    purchases_30d INTEGER DEFAULT 0,
    revenue_30d DECIMAL(15, 2) DEFAULT 0,

    -- Trend indicators
    views_trend VARCHAR(20),
    sales_trend VARCHAR(20),

    -- Metadata
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_product_features_product_id
    ON product_features(product_id);
CREATE INDEX IF NOT EXISTS idx_product_features_popularity
    ON product_features(popularity_score DESC);
"""

UPSERT_DAILY_METRICS = """
INSERT INTO product_daily_metrics (
    product_id, date, views, clicks, add_to_carts,
    purchases, revenue, reviews_count, created_at, updated_at
) VALUES (
    %(product_id)s, %(date)s, %(views)s, %(clicks)s, %(add_to_carts)s,
    %(purchases)s, %(revenue)s, %(reviews_count)s, %(created_at)s, %(updated_at)s
)
ON CONFLICT (product_id, date)
DO UPDATE SET
    views = EXCLUDED.views,
    clicks = EXCLUDED.clicks,
    add_to_carts = EXCLUDED.add_to_carts,
    purchases = EXCLUDED.purchases,
    revenue = EXCLUDED.revenue,
    reviews_count = EXCLUDED.reviews_count,
    updated_at = CURRENT_TIMESTAMP;
"""

UPSERT_PRODUCT_FEATURES = """
INSERT INTO product_features (
    product_id, price_normalized, discount_percentage, price_tier,
    popularity_score, engagement_rate, conversion_rate,
    views_7d, purchases_7d, revenue_7d,
    views_30d, purchases_30d, revenue_30d,
    views_trend, sales_trend, created_at, updated_at
) VALUES (
    %(product_id)s, %(price_normalized)s, %(discount_percentage)s, %(price_tier)s,
    %(popularity_score)s, %(engagement_rate)s, %(conversion_rate)s,
    %(views_7d)s, %(purchases_7d)s, %(revenue_7d)s,
    %(views_30d)s, %(purchases_30d)s, %(revenue_30d)s,
    %(views_trend)s, %(sales_trend)s, %(created_at)s, %(updated_at)s
)
ON CONFLICT (product_id)
DO UPDATE SET
    price_normalized = EXCLUDED.price_normalized,
    discount_percentage = EXCLUDED.discount_percentage,
    price_tier = EXCLUDED.price_tier,
    popularity_score = EXCLUDED.popularity_score,
    engagement_rate = EXCLUDED.engagement_rate,
    conversion_rate = EXCLUDED.conversion_rate,
    views_7d = EXCLUDED.views_7d,
    purchases_7d = EXCLUDED.purchases_7d,
    revenue_7d = EXCLUDED.revenue_7d,
    views_30d = EXCLUDED.views_30d,
    purchases_30d = EXCLUDED.purchases_30d,
    revenue_30d = EXCLUDED.revenue_30d,
    views_trend = EXCLUDED.views_trend,
    sales_trend = EXCLUDED.sales_trend,
    updated_at = CURRENT_TIMESTAMP;
"""


class FeatureBuilder:
    """
    Class xay dung features tu du lieu san pham.
    Tao cac features cho recommendation va forecasting.
    """

    def __init__(self):
        """Khoi tao FeatureBuilder voi cau hinh tu environment."""
        self.postgres_uri = os.getenv("POSTGRES_URI")

        if not self.postgres_uri:
            raise ValueError("POSTGRES_URI environment variable is required")

        self.pg_conn = None

    def connect(self) -> None:
        """Ket noi den PostgreSQL."""
        logger.info("Connecting to PostgreSQL...")

        try:
            self.pg_conn = psycopg.connect(
                self.postgres_uri,
                autocommit=False,
                row_factory=dict_row,
            )
            logger.info("PostgreSQL connection successful")
        except psycopg.Error as e:
            logger.error(f"Failed to connect to PostgreSQL: {e}")
            raise

    def close(self) -> None:
        """Dong ket noi PostgreSQL."""
        if self.pg_conn:
            self.pg_conn.close()
            logger.info("PostgreSQL connection closed")

    def setup_tables(self) -> None:
        """Tao cac feature tables neu chua ton tai."""
        logger.info("Creating feature tables if not exist...")

        try:
            with self.pg_conn.cursor() as cur:
                cur.execute(CREATE_PRODUCT_DAILY_METRICS_TABLE)
                cur.execute(CREATE_PRODUCT_FEATURES_TABLE)
            self.pg_conn.commit()
            logger.info("Feature tables are ready")
        except psycopg.Error as e:
            self.pg_conn.rollback()
            logger.error(f"Failed to create tables: {e}")
            raise

    def get_products(self) -> list[dict[str, Any]]:
        """
        Lay danh sach san pham tu PostgreSQL.

        Returns:
            List cac product records
        """
        logger.info("Fetching products from database...")

        try:
            with self.pg_conn.cursor() as cur:
                cur.execute("""
                    SELECT id, external_id, title, brand, category,
                           price, original_price, rating, review_count, stock
                    FROM products
                    ORDER BY id
                """)
                products = cur.fetchall()
                logger.info(f"Fetched {len(products)} products")
                return products
        except psycopg.Error as e:
            logger.error(f"Failed to fetch products: {e}")
            return []

    def get_price_stats(self) -> dict[str, float]:
        """
        Lay thong ke gia de normalize.

        Returns:
            Dict chua min, max, avg price
        """
        try:
            with self.pg_conn.cursor() as cur:
                cur.execute("""
                    SELECT
                        MIN(price) as min_price,
                        MAX(price) as max_price,
                        AVG(price) as avg_price
                    FROM products
                    WHERE price > 0
                """)
                result = cur.fetchone()
                return {
                    "min": float(result["min_price"] or 0),
                    "max": float(result["max_price"] or 1),
                    "avg": float(result["avg_price"] or 0),
                }
        except psycopg.Error:
            return {"min": 0, "max": 1, "avg": 0}

    def generate_mock_daily_metrics(
        self, products: list[dict[str, Any]], days: int = 30
    ) -> list[dict[str, Any]]:
        """
        Generate mock daily metrics cho demo.
        Trong production, du lieu nay se tu thuc te.

        Args:
            products: Danh sach san pham
            days: So ngay can generate

        Returns:
            List daily metrics records
        """
        logger.info(f"Generating mock daily metrics for {days} days...")

        metrics = []
        now = datetime.now(timezone.utc)

        for product in products:
            product_id = product["id"]
            price = float(product["price"] or 0)
            rating = float(product["rating"] or 3.0)

            # Base daily stats tu rating va price
            base_views = int(50 + rating * 20 + random.randint(0, 50))
            base_clicks = int(base_views * (0.2 + random.random() * 0.2))
            base_carts = int(base_clicks * (0.1 + random.random() * 0.1))
            base_purchases = int(base_carts * (0.3 + random.random() * 0.2))

            for day_offset in range(days):
                date = (now - timedelta(days=day_offset)).date()

                # Add some randomness
                day_factor = 1.0 + random.uniform(-0.3, 0.3)

                # Weekend boost
                if date.weekday() >= 5:
                    day_factor *= 1.2

                views = int(base_views * day_factor)
                clicks = int(base_clicks * day_factor)
                add_to_carts = int(base_carts * day_factor)
                purchases = int(base_purchases * day_factor)
                revenue = purchases * price
                reviews = random.randint(0, 3) if purchases > 0 else 0

                metrics.append({
                    "product_id": product_id,
                    "date": date,
                    "views": views,
                    "clicks": clicks,
                    "add_to_carts": add_to_carts,
                    "purchases": purchases,
                    "revenue": revenue,
                    "reviews_count": reviews,
                    "created_at": now,
                    "updated_at": now,
                })

        logger.info(f"Generated {len(metrics)} daily metric records")
        return metrics

    def save_daily_metrics(self, metrics: list[dict[str, Any]]) -> int:
        """
        Luu daily metrics vao PostgreSQL.

        Args:
            metrics: List daily metric records

        Returns:
            So luong records da luu
        """
        logger.info("Saving daily metrics to database...")

        saved = 0
        batch_size = 500

        for i in range(0, len(metrics), batch_size):
            batch = metrics[i : i + batch_size]

            try:
                with self.pg_conn.cursor() as cur:
                    for record in batch:
                        cur.execute(UPSERT_DAILY_METRICS, record)
                        saved += 1
                self.pg_conn.commit()
            except psycopg.Error as e:
                self.pg_conn.rollback()
                logger.error(f"Failed to save daily metrics batch: {e}")

        logger.info(f"Saved {saved} daily metric records")
        return saved

    def calculate_price_tier(self, price: float, price_stats: dict[str, float]) -> str:
        """
        Xac dinh price tier.

        Args:
            price: Gia san pham
            price_stats: Thong ke gia

        Returns:
            Price tier string
        """
        if price <= 0:
            return "unknown"

        avg = price_stats["avg"]

        if price < avg * 0.3:
            return "budget"
        elif price < avg * 0.7:
            return "mid-range"
        elif price < avg * 1.5:
            return "premium"
        else:
            return "luxury"

    def calculate_trend(self, current: int, previous: int) -> str:
        """
        Xac dinh trend dua tren so sanh.

        Args:
            current: Gia tri hien tai
            previous: Gia tri truoc do

        Returns:
            Trend string
        """
        if previous == 0:
            return "new" if current > 0 else "stable"

        change = (current - previous) / previous

        if change > 0.2:
            return "rising"
        elif change < -0.2:
            return "falling"
        else:
            return "stable"

    def build_product_features(
        self, products: list[dict[str, Any]], price_stats: dict[str, float]
    ) -> list[dict[str, Any]]:
        """
        Xay dung features cho tung san pham.

        Args:
            products: Danh sach san pham
            price_stats: Thong ke gia

        Returns:
            List product feature records
        """
        logger.info("Building product features...")

        features_list = []
        now = datetime.now(timezone.utc)

        for product in products:
            product_id = product["id"]
            price = float(product["price"] or 0)
            original_price = float(product["original_price"] or price)
            rating = float(product["rating"] or 0)
            review_count = int(product["review_count"] or 0)

            # Price features
            price_range = price_stats["max"] - price_stats["min"]
            price_normalized = (
                (price - price_stats["min"]) / price_range
                if price_range > 0
                else 0.5
            )
            discount_percentage = (
                ((original_price - price) / original_price * 100)
                if original_price > price > 0
                else 0
            )
            price_tier = self.calculate_price_tier(price, price_stats)

            # Get rolling metrics from daily_metrics
            metrics_7d = self._get_rolling_metrics(product_id, 7)
            metrics_30d = self._get_rolling_metrics(product_id, 30)

            # Calculate derived metrics
            views_7d = metrics_7d.get("views", 0)
            views_30d = metrics_30d.get("views", 0)
            purchases_7d = metrics_7d.get("purchases", 0)
            purchases_30d = metrics_30d.get("purchases", 0)

            # Engagement & Conversion rates
            engagement_rate = (
                metrics_7d.get("clicks", 0) / views_7d if views_7d > 0 else 0
            )
            conversion_rate = purchases_7d / views_7d if views_7d > 0 else 0

            # Popularity score (composite)
            popularity_score = (
                0.3 * min(rating / 5.0, 1.0)
                + 0.3 * min(review_count / 1000.0, 1.0)
                + 0.2 * min(views_7d / 1000.0, 1.0)
                + 0.2 * min(purchases_7d / 100.0, 1.0)
            )

            # Trends
            views_prev_7d = self._get_rolling_metrics(product_id, 14, offset=7).get("views", 0)
            purchases_prev_7d = self._get_rolling_metrics(product_id, 14, offset=7).get("purchases", 0)
            views_trend = self.calculate_trend(views_7d, views_prev_7d)
            sales_trend = self.calculate_trend(purchases_7d, purchases_prev_7d)

            features_list.append({
                "product_id": product_id,
                "price_normalized": round(price_normalized, 4),
                "discount_percentage": round(discount_percentage, 2),
                "price_tier": price_tier,
                "popularity_score": round(popularity_score, 4),
                "engagement_rate": round(engagement_rate, 4),
                "conversion_rate": round(conversion_rate, 4),
                "views_7d": views_7d,
                "purchases_7d": purchases_7d,
                "revenue_7d": metrics_7d.get("revenue", 0),
                "views_30d": views_30d,
                "purchases_30d": purchases_30d,
                "revenue_30d": metrics_30d.get("revenue", 0),
                "views_trend": views_trend,
                "sales_trend": sales_trend,
                "created_at": now,
                "updated_at": now,
            })

        logger.info(f"Built features for {len(features_list)} products")
        return features_list

    def _get_rolling_metrics(
        self, product_id: int, days: int, offset: int = 0
    ) -> dict[str, Any]:
        """
        Lay rolling metrics cho san pham.

        Args:
            product_id: ID san pham
            days: So ngay can lay
            offset: So ngay lech ve truoc

        Returns:
            Dict chua metrics
        """
        try:
            with self.pg_conn.cursor() as cur:
                cur.execute(
                    """
                    SELECT
                        COALESCE(SUM(views), 0) as views,
                        COALESCE(SUM(clicks), 0) as clicks,
                        COALESCE(SUM(purchases), 0) as purchases,
                        COALESCE(SUM(revenue), 0) as revenue
                    FROM product_daily_metrics
                    WHERE product_id = %s
                      AND date >= CURRENT_DATE - INTERVAL '%s days' - INTERVAL '%s days'
                      AND date < CURRENT_DATE - INTERVAL '%s days'
                    """,
                    (product_id, days, offset, offset),
                )
                result = cur.fetchone()
                return {
                    "views": int(result["views"]) if result else 0,
                    "clicks": int(result["clicks"]) if result else 0,
                    "purchases": int(result["purchases"]) if result else 0,
                    "revenue": float(result["revenue"]) if result else 0,
                }
        except psycopg.Error:
            return {"views": 0, "clicks": 0, "purchases": 0, "revenue": 0}

    def save_product_features(self, features: list[dict[str, Any]]) -> int:
        """
        Luu product features vao PostgreSQL.

        Args:
            features: List feature records

        Returns:
            So luong records da luu
        """
        logger.info("Saving product features to database...")

        saved = 0

        try:
            with self.pg_conn.cursor() as cur:
                for feature in features:
                    cur.execute(UPSERT_PRODUCT_FEATURES, feature)
                    saved += 1
            self.pg_conn.commit()
        except psycopg.Error as e:
            self.pg_conn.rollback()
            logger.error(f"Failed to save product features: {e}")

        logger.info(f"Saved {saved} product feature records")
        return saved

    def run(self, generate_mock_metrics: bool = True) -> dict[str, int]:
        """
        Chay toan bo quy trinh build features.

        Args:
            generate_mock_metrics: Co generate mock daily metrics khong

        Returns:
            Dict chua thong ke ket qua
        """
        logger.info("=" * 60)
        logger.info("Starting feature building pipeline")
        logger.info("=" * 60)

        stats = {
            "products_count": 0,
            "daily_metrics_saved": 0,
            "product_features_saved": 0,
        }

        try:
            self.connect()
            self.setup_tables()

            # Get products
            products = self.get_products()
            stats["products_count"] = len(products)

            if not products:
                logger.warning("No products found. Exiting.")
                return stats

            # Get price stats
            price_stats = self.get_price_stats()
            logger.info(f"Price stats: {price_stats}")

            # Generate and save mock daily metrics (for demo)
            if generate_mock_metrics:
                daily_metrics = self.generate_mock_daily_metrics(products, days=30)
                stats["daily_metrics_saved"] = self.save_daily_metrics(daily_metrics)

            # Build and save product features
            features = self.build_product_features(products, price_stats)
            stats["product_features_saved"] = self.save_product_features(features)

            logger.info("=" * 60)
            logger.info("Feature building pipeline completed")
            logger.info(f"Products processed: {stats['products_count']}")
            logger.info(f"Daily metrics saved: {stats['daily_metrics_saved']}")
            logger.info(f"Product features saved: {stats['product_features_saved']}")
            logger.info("=" * 60)

        finally:
            self.close()

        return stats


def main():
    """Entry point cho build_features module."""
    builder = FeatureBuilder()
    stats = builder.run(generate_mock_metrics=True)

    if stats["product_features_saved"] == 0:
        logger.warning("No features were built")
        sys.exit(1)


if __name__ == "__main__":
    main()
