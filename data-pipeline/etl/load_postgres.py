"""
Load to PostgreSQL ETL
----------------------
Silver -> Gold: Load du lieu da clean vao PostgreSQL (Supabase).

Module nay thuc hien:
- Doc clean data tu MongoDB (products_clean collection)
- Ket noi den PostgreSQL (Supabase)
- Tao table "products" neu chua ton tai
- Upsert du lieu bang ON CONFLICT

Medallion Architecture:
- Input: Silver (products_clean - MongoDB)
- Output: Gold (products table - PostgreSQL)

IMPORTANT: Su dung psycopg v3 (KHONG phai psycopg2) de tuong thich Python 3.13

Usage:
    python -m etl.load_postgres
"""

import os
import sys
import json
from datetime import datetime, timezone
from pathlib import Path
from typing import Any

import psycopg
from psycopg.rows import dict_row
from dotenv import load_dotenv
from loguru import logger
from pymongo import MongoClient
from pymongo.errors import ConnectionFailure

# Load environment variables
project_root = Path(__file__).parent.parent
load_dotenv(project_root / ".env")

# Configure loguru
log_dir = project_root / "logs"
log_dir.mkdir(exist_ok=True)
logger.add(
    log_dir / "load_postgres_{time:YYYY-MM-DD}.log",
    rotation="1 day",
    retention="7 days",
    level="INFO",
)


# SQL Statements
CREATE_PRODUCTS_TABLE = """
CREATE TABLE IF NOT EXISTS products (
    id SERIAL PRIMARY KEY,
    external_id VARCHAR(100) NOT NULL,
    source VARCHAR(50) NOT NULL,
    title VARCHAR(500) NOT NULL,
    brand VARCHAR(100),
    category VARCHAR(100),
    price DECIMAL(15, 2),
    original_price DECIMAL(15, 2),
    stock INTEGER DEFAULT 0,
    rating DECIMAL(3, 2),
    review_count INTEGER DEFAULT 0,
    description TEXT,
    image_urls JSONB DEFAULT '[]'::jsonb,
    specifications JSONB DEFAULT '{}'::jsonb,
    source_url VARCHAR(1000),
    crawl_time TIMESTAMP WITH TIME ZONE,
    cleaned_at TIMESTAMP WITH TIME ZONE,
    loaded_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(external_id, source)
);

-- Create indexes for common queries
CREATE INDEX IF NOT EXISTS idx_products_category ON products(category);
CREATE INDEX IF NOT EXISTS idx_products_brand ON products(brand);
CREATE INDEX IF NOT EXISTS idx_products_price ON products(price);
CREATE INDEX IF NOT EXISTS idx_products_rating ON products(rating);
CREATE INDEX IF NOT EXISTS idx_products_source ON products(source);
"""

UPSERT_PRODUCT = """
INSERT INTO products (
    external_id, source, title, brand, category, price, original_price,
    stock, rating, review_count, description, image_urls, specifications,
    source_url, crawl_time, cleaned_at, loaded_at, updated_at
) VALUES (
    %(external_id)s, %(source)s, %(title)s, %(brand)s, %(category)s,
    %(price)s, %(original_price)s, %(stock)s, %(rating)s, %(review_count)s,
    %(description)s, %(image_urls)s, %(specifications)s, %(source_url)s,
    %(crawl_time)s, %(cleaned_at)s, %(loaded_at)s, %(updated_at)s
)
ON CONFLICT (external_id, source)
DO UPDATE SET
    title = EXCLUDED.title,
    brand = EXCLUDED.brand,
    category = EXCLUDED.category,
    price = EXCLUDED.price,
    original_price = EXCLUDED.original_price,
    stock = EXCLUDED.stock,
    rating = EXCLUDED.rating,
    review_count = EXCLUDED.review_count,
    description = EXCLUDED.description,
    image_urls = EXCLUDED.image_urls,
    specifications = EXCLUDED.specifications,
    source_url = EXCLUDED.source_url,
    crawl_time = EXCLUDED.crawl_time,
    cleaned_at = EXCLUDED.cleaned_at,
    updated_at = CURRENT_TIMESTAMP;
"""


class PostgresLoader:
    """
    Class load du lieu tu MongoDB Silver layer vao PostgreSQL Gold layer.
    Su dung psycopg v3 de tuong thich Python 3.13.
    """

    def __init__(self):
        """Khoi tao PostgresLoader voi cau hinh tu environment."""
        # MongoDB config
        self.mongo_uri = os.getenv("MONGO_URI")
        self.mongo_db = os.getenv("MONGO_DB_NAME", "cellex_prod")
        self.clean_collection = os.getenv("MONGO_CLEAN_COLLECTION", "products_clean")

        # PostgreSQL config
        self.postgres_uri = os.getenv("POSTGRES_URI")

        if not self.mongo_uri:
            raise ValueError("MONGO_URI environment variable is required")
        if not self.postgres_uri:
            raise ValueError("POSTGRES_URI environment variable is required")

        self.mongo_client = None
        self.mongo_db_conn = None
        self.pg_conn = None

    def connect_mongo(self) -> None:
        """Ket noi den MongoDB."""
        logger.info(f"Connecting to MongoDB: {self.mongo_db}/{self.clean_collection}")

        try:
            self.mongo_client = MongoClient(
                self.mongo_uri,
                serverSelectionTimeoutMS=10000,
                connectTimeoutMS=10000,
            )
            self.mongo_client.admin.command("ping")
            self.mongo_db_conn = self.mongo_client[self.mongo_db]
            logger.info("MongoDB connection successful")
        except ConnectionFailure as e:
            logger.error(f"Failed to connect to MongoDB: {e}")
            raise

    def connect_postgres(self) -> None:
        """Ket noi den PostgreSQL (Supabase)."""
        logger.info("Connecting to PostgreSQL (Supabase)...")

        try:
            # psycopg v3 syntax
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
        """Dong tat ca ket noi."""
        if self.mongo_client:
            self.mongo_client.close()
            logger.info("MongoDB connection closed")

        if self.pg_conn:
            self.pg_conn.close()
            logger.info("PostgreSQL connection closed")

    def setup_postgres_table(self) -> None:
        """Tao table products trong PostgreSQL neu chua ton tai."""
        logger.info("Creating products table if not exists...")

        try:
            with self.pg_conn.cursor() as cur:
                cur.execute(CREATE_PRODUCTS_TABLE)
            self.pg_conn.commit()
            logger.info("Products table is ready")
        except psycopg.Error as e:
            self.pg_conn.rollback()
            logger.error(f"Failed to create table: {e}")
            raise

    def read_clean_data(self) -> list[dict[str, Any]]:
        """
        Doc clean data tu MongoDB Silver layer.

        Returns:
            List cac document san pham da clean
        """
        logger.info(f"Reading clean data from {self.clean_collection}")

        collection = self.mongo_db_conn[self.clean_collection]
        clean_data = list(collection.find({}))

        logger.info(f"Read {len(clean_data)} clean products")
        return clean_data

    def transform_for_postgres(self, product: dict[str, Any]) -> dict[str, Any]:
        """
        Transform du lieu tu MongoDB format sang PostgreSQL format.

        Args:
            product: Product document tu MongoDB

        Returns:
            Dict ready cho PostgreSQL upsert
        """
        now = datetime.now(timezone.utc)

        # Parse timestamps
        crawl_time = None
        if product.get("crawl_time"):
            try:
                crawl_time = datetime.fromisoformat(
                    product["crawl_time"].replace("Z", "+00:00")
                )
            except (ValueError, AttributeError):
                pass

        cleaned_at = None
        if product.get("cleaned_at"):
            try:
                cleaned_at = datetime.fromisoformat(
                    product["cleaned_at"].replace("Z", "+00:00")
                )
            except (ValueError, AttributeError):
                pass

        return {
            "external_id": str(product.get("external_id", "")),
            "source": product.get("source", "unknown"),
            "title": product.get("title", "")[:500],  # Truncate to fit VARCHAR(500)
            "brand": product.get("brand")[:100] if product.get("brand") else None,
            "category": product.get("category")[:100] if product.get("category") else None,
            "price": product.get("price"),
            "original_price": product.get("original_price"),
            "stock": product.get("stock", 0),
            "rating": product.get("rating"),
            "review_count": product.get("review_count", 0),
            "description": product.get("description"),
            "image_urls": json.dumps(product.get("image_urls", [])),
            "specifications": json.dumps(product.get("specifications", {})),
            "source_url": product.get("source_url")[:1000] if product.get("source_url") else None,
            "crawl_time": crawl_time,
            "cleaned_at": cleaned_at,
            "loaded_at": now,
            "updated_at": now,
        }

    def upsert_products(self, products: list[dict[str, Any]]) -> dict[str, int]:
        """
        Upsert danh sach san pham vao PostgreSQL.

        Args:
            products: Danh sach san pham da transform

        Returns:
            Dict chua so luong success va failed
        """
        logger.info(f"Upserting {len(products)} products to PostgreSQL")

        stats = {"success": 0, "failed": 0}

        # Batch upsert de tang performance
        batch_size = 100

        for i in range(0, len(products), batch_size):
            batch = products[i : i + batch_size]

            try:
                with self.pg_conn.cursor() as cur:
                    for product in batch:
                        pg_data = self.transform_for_postgres(product)
                        cur.execute(UPSERT_PRODUCT, pg_data)
                        stats["success"] += 1

                self.pg_conn.commit()
                logger.debug(f"Batch {i // batch_size + 1} committed")

            except psycopg.Error as e:
                self.pg_conn.rollback()
                logger.error(f"Batch failed: {e}")
                stats["failed"] += len(batch)

        return stats

    def get_row_count(self) -> int:
        """
        Lay so luong row trong table products.

        Returns:
            So luong row
        """
        try:
            with self.pg_conn.cursor() as cur:
                cur.execute("SELECT COUNT(*) as count FROM products")
                result = cur.fetchone()
                return result["count"] if result else 0
        except psycopg.Error as e:
            logger.error(f"Failed to get row count: {e}")
            return 0

    def run(self) -> dict[str, int]:
        """
        Chay toan bo quy trinh load du lieu.

        Returns:
            Dict chua thong ke ket qua
        """
        logger.info("=" * 60)
        logger.info("Starting PostgreSQL loading pipeline")
        logger.info("=" * 60)

        stats = {
            "clean_count": 0,
            "success": 0,
            "failed": 0,
            "total_in_db": 0,
        }

        try:
            # Connect to databases
            self.connect_mongo()
            self.connect_postgres()

            # Setup PostgreSQL table
            self.setup_postgres_table()

            # Read clean data from MongoDB
            clean_data = self.read_clean_data()
            stats["clean_count"] = len(clean_data)

            if not clean_data:
                logger.warning("No clean data found. Exiting.")
                return stats

            # Upsert to PostgreSQL
            upsert_stats = self.upsert_products(clean_data)
            stats["success"] = upsert_stats["success"]
            stats["failed"] = upsert_stats["failed"]

            # Get final row count
            stats["total_in_db"] = self.get_row_count()

            logger.info("=" * 60)
            logger.info("PostgreSQL loading pipeline completed")
            logger.info(f"Clean products read: {stats['clean_count']}")
            logger.info(f"Successfully loaded: {stats['success']}")
            logger.info(f"Failed: {stats['failed']}")
            logger.info(f"Total products in database: {stats['total_in_db']}")
            logger.info("=" * 60)

        finally:
            self.close()

        return stats


def main():
    """Entry point cho load_postgres module."""
    loader = PostgresLoader()
    stats = loader.run()

    # Exit voi error code neu co nhieu failures
    if stats["failed"] > stats["success"]:
        logger.error("More failures than successes")
        sys.exit(1)


if __name__ == "__main__":
    main()
