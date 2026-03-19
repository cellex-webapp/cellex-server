"""
Clean Products ETL
------------------
Bronze -> Silver: Lam sach va chuan hoa du lieu san pham.

Module nay thuc hien:
- Doc raw data tu MongoDB (raw_products collection)
- Lam sach va chuan hoa du lieu
- Loai bo duplicates (bang external_id va fuzzy matching)
- Luu vao products_clean collection

Medallion Architecture:
- Input: Bronze (raw_products)
- Output: Silver (products_clean)

Usage:
    python -m etl.clean_products
"""

import os
import re
import sys
from datetime import datetime, timezone
from pathlib import Path
from typing import Any

import pandas as pd
from dotenv import load_dotenv
from loguru import logger
from pymongo import MongoClient
from pymongo.errors import ConnectionFailure
from rapidfuzz import fuzz

# Load environment variables
project_root = Path(__file__).parent.parent
load_dotenv(project_root / ".env")

# Configure loguru
log_dir = project_root / "logs"
log_dir.mkdir(exist_ok=True)
logger.add(
    log_dir / "clean_products_{time:YYYY-MM-DD}.log",
    rotation="1 day",
    retention="7 days",
    level="INFO",
)


class ProductCleaner:
    """
    Class lam sach va chuan hoa du lieu san pham.
    """

    # Nguong similarity cho fuzzy matching
    FUZZY_THRESHOLD = 85

    def __init__(self):
        """Khoi tao ProductCleaner voi cau hinh tu environment."""
        self.mongo_uri = os.getenv("MONGO_URI")
        self.mongo_db = os.getenv("MONGO_DB_NAME", "cellex_prod")
        self.raw_collection = os.getenv("MONGO_RAW_COLLECTION", "raw_products")
        self.clean_collection = os.getenv("MONGO_CLEAN_COLLECTION", "products_clean")

        if not self.mongo_uri:
            raise ValueError("MONGO_URI environment variable is required")

        self.client = None
        self.db = None

    def connect(self) -> None:
        """Ket noi den MongoDB."""
        logger.info(f"Connecting to MongoDB database: {self.mongo_db}")

        try:
            self.client = MongoClient(
                self.mongo_uri,
                serverSelectionTimeoutMS=10000,
                connectTimeoutMS=10000,
            )
            # Test connection
            self.client.admin.command("ping")
            self.db = self.client[self.mongo_db]
            logger.info("MongoDB connection successful")
        except ConnectionFailure as e:
            logger.error(f"Failed to connect to MongoDB: {e}")
            raise

    def close(self) -> None:
        """Dong ket noi MongoDB."""
        if self.client:
            self.client.close()
            logger.info("MongoDB connection closed")

    def read_raw_data(self) -> list[dict[str, Any]]:
        """
        Doc raw data tu Bronze layer (raw_products collection).

        Returns:
            List cac document san pham
        """
        logger.info(f"Reading raw data from {self.raw_collection}")

        collection = self.db[self.raw_collection]
        raw_data = list(collection.find({}))

        logger.info(f"Read {len(raw_data)} raw products")
        return raw_data

    def clean_price(self, price: Any) -> float | None:
        """
        Chuan hoa gia san pham.

        Args:
            price: Gia dang raw (co the la string hoac number)

        Returns:
            Gia dang float hoac None neu khong hop le
        """
        if price is None:
            return None

        if isinstance(price, (int, float)):
            return float(price) if price > 0 else None

        # Neu la string, loai bo ky tu khong phai so
        price_str = str(price)
        # Giu lai so va dau cham
        price_clean = re.sub(r"[^\d.]", "", price_str)

        if not price_clean:
            return None

        try:
            cleaned = float(price_clean)
            return cleaned if cleaned > 0 else None
        except ValueError:
            return None

    def clean_text(self, text: Any) -> str | None:
        """
        Lam sach va chuan hoa text.

        Args:
            text: Text can lam sach

        Returns:
            Text da lam sach hoac None
        """
        if text is None:
            return None

        cleaned = str(text).strip()
        # Loai bo khoang trang thua
        cleaned = " ".join(cleaned.split())

        return cleaned if cleaned else None

    def clean_rating(self, rating: Any) -> float | None:
        """
        Chuan hoa diem rating.

        Args:
            rating: Rating raw

        Returns:
            Rating dang float (0-5) hoac None
        """
        if rating is None:
            return None

        try:
            r = float(rating)
            # Clamp ve khoang 0-5
            return max(0.0, min(5.0, r))
        except (ValueError, TypeError):
            return None

    def clean_stock(self, stock: Any) -> int:
        """
        Chuan hoa so luong ton kho.

        Args:
            stock: Stock raw

        Returns:
            Stock dang int (>= 0)
        """
        if stock is None:
            return 0

        try:
            s = int(stock)
            return max(0, s)
        except (ValueError, TypeError):
            return 0

    def clean_product(self, raw_product: dict[str, Any]) -> dict[str, Any] | None:
        """
        Lam sach mot san pham.

        Args:
            raw_product: Du lieu san pham raw

        Returns:
            San pham da lam sach hoac None neu khong hop le
        """
        # Kiem tra required fields
        external_id = raw_product.get("external_id")
        title = self.clean_text(raw_product.get("title"))

        if not external_id or not title:
            return None

        cleaned = {
            "external_id": str(external_id),
            "title": title,
            "title_normalized": title.lower() if title else None,  # Cho fuzzy matching
            "price": self.clean_price(raw_product.get("price")),
            "original_price": self.clean_price(raw_product.get("original_price")),
            "brand": self.clean_text(raw_product.get("brand")),
            "category": self.clean_text(raw_product.get("category")),
            "description": self.clean_text(raw_product.get("description")),
            "image_urls": raw_product.get("image_urls", []),
            "rating": self.clean_rating(raw_product.get("rating")),
            "review_count": self.clean_stock(raw_product.get("review_count")),
            "stock": self.clean_stock(raw_product.get("stock")),
            "specifications": raw_product.get("specifications", {}),
            "source": raw_product.get("source"),
            "source_url": raw_product.get("source_url"),
            "crawl_time": raw_product.get("crawl_time"),
            "cleaned_at": datetime.now(timezone.utc).isoformat(),
        }

        return cleaned

    def detect_duplicates_by_external_id(
        self, products: list[dict[str, Any]]
    ) -> list[dict[str, Any]]:
        """
        Loai bo duplicates dua tren external_id + source.
        Giu lai ban ghi moi nhat.

        Args:
            products: Danh sach san pham

        Returns:
            Danh sach san pham da loai bo duplicate
        """
        logger.info("Detecting duplicates by external_id + source")

        df = pd.DataFrame(products)

        if df.empty:
            return []

        # Sort theo crawl_time giam dan de giu ban ghi moi nhat
        df["crawl_time_parsed"] = pd.to_datetime(df["crawl_time"], errors="coerce")
        df = df.sort_values("crawl_time_parsed", ascending=False)

        # Drop duplicates, giu ban ghi dau tien (moi nhat)
        df_deduped = df.drop_duplicates(subset=["external_id", "source"], keep="first")

        duplicates_removed = len(df) - len(df_deduped)
        logger.info(f"Removed {duplicates_removed} exact duplicates")

        # Bo cot tam
        df_deduped = df_deduped.drop(columns=["crawl_time_parsed"])

        return df_deduped.to_dict("records")

    def detect_duplicates_by_fuzzy_matching(
        self, products: list[dict[str, Any]]
    ) -> list[dict[str, Any]]:
        """
        Loai bo duplicates dua tren fuzzy matching title.
        Su dung rapidfuzz de so sanh title.

        Args:
            products: Danh sach san pham

        Returns:
            Danh sach san pham da loai bo fuzzy duplicates
        """
        logger.info(f"Detecting fuzzy duplicates (threshold: {self.FUZZY_THRESHOLD})")

        if not products:
            return []

        # Track cac san pham da duoc danh dau la duplicate
        seen_indices = set()
        unique_products = []

        for i, product in enumerate(products):
            if i in seen_indices:
                continue

            title_i = product.get("title_normalized", "")
            brand_i = product.get("brand", "")
            category_i = product.get("category", "")

            # Tim cac san pham tuong tu sau vi tri hien tai
            for j, other in enumerate(products[i + 1:], start=i + 1):
                if j in seen_indices:
                    continue

                title_j = other.get("title_normalized", "")
                brand_j = other.get("brand", "")
                category_j = other.get("category", "")

                # Chi so sanh neu cung brand va category
                if brand_i != brand_j or category_i != category_j:
                    continue

                # Tinh fuzzy similarity
                similarity = fuzz.ratio(title_i, title_j)

                if similarity >= self.FUZZY_THRESHOLD:
                    # Danh dau j la duplicate
                    seen_indices.add(j)
                    logger.debug(
                        f"Fuzzy duplicate found: '{title_i}' ~ '{title_j}' "
                        f"(similarity: {similarity}%)"
                    )

            unique_products.append(product)

        fuzzy_removed = len(products) - len(unique_products)
        logger.info(f"Removed {fuzzy_removed} fuzzy duplicates")

        return unique_products

    def save_clean_data(
        self, clean_products: list[dict[str, Any]], clear_existing: bool = True
    ) -> int:
        """
        Luu du lieu da lam sach vao Silver layer (products_clean collection).

        Args:
            clean_products: Danh sach san pham da lam sach
            clear_existing: Xoa du lieu cu truoc khi luu moi

        Returns:
            So luong san pham da luu
        """
        logger.info(f"Saving clean data to {self.clean_collection}")

        collection = self.db[self.clean_collection]

        if clear_existing:
            result = collection.delete_many({})
            logger.info(f"Cleared {result.deleted_count} existing documents")

        if not clean_products:
            logger.warning("No products to save")
            return 0

        # Insert all clean products
        result = collection.insert_many(clean_products)

        # Tao indexes
        collection.create_index([("external_id", 1), ("source", 1)], unique=True)
        collection.create_index([("category", 1)])
        collection.create_index([("brand", 1)])
        collection.create_index([("price", 1)])

        logger.info(f"Saved {len(result.inserted_ids)} clean products")
        return len(result.inserted_ids)

    def run(self) -> dict[str, int]:
        """
        Chay toan bo quy trinh lam sach du lieu.

        Returns:
            Dict chua thong ke ket qua
        """
        logger.info("=" * 60)
        logger.info("Starting product cleaning pipeline")
        logger.info("=" * 60)

        stats = {
            "raw_count": 0,
            "after_cleaning": 0,
            "after_exact_dedup": 0,
            "after_fuzzy_dedup": 0,
            "final_count": 0,
        }

        try:
            self.connect()

            # Step 1: Doc raw data
            raw_data = self.read_raw_data()
            stats["raw_count"] = len(raw_data)

            if not raw_data:
                logger.warning("No raw data found. Exiting.")
                return stats

            # Step 2: Lam sach tung san pham
            logger.info("Cleaning products...")
            clean_products = []
            for raw in raw_data:
                cleaned = self.clean_product(raw)
                if cleaned:
                    clean_products.append(cleaned)
                else:
                    logger.debug(f"Skipped invalid product: {raw.get('external_id')}")

            stats["after_cleaning"] = len(clean_products)
            logger.info(f"Valid products after cleaning: {len(clean_products)}")

            # Step 3: Loai bo exact duplicates
            clean_products = self.detect_duplicates_by_external_id(clean_products)
            stats["after_exact_dedup"] = len(clean_products)

            # Step 4: Loai bo fuzzy duplicates
            clean_products = self.detect_duplicates_by_fuzzy_matching(clean_products)
            stats["after_fuzzy_dedup"] = len(clean_products)

            # Step 5: Luu vao Silver layer
            saved_count = self.save_clean_data(clean_products)
            stats["final_count"] = saved_count

            logger.info("=" * 60)
            logger.info("Cleaning pipeline completed")
            logger.info(f"Raw products: {stats['raw_count']}")
            logger.info(f"After cleaning: {stats['after_cleaning']}")
            logger.info(f"After exact dedup: {stats['after_exact_dedup']}")
            logger.info(f"After fuzzy dedup: {stats['after_fuzzy_dedup']}")
            logger.info(f"Final saved: {stats['final_count']}")
            logger.info("=" * 60)

        finally:
            self.close()

        return stats


def main():
    """Entry point cho clean_products module."""
    cleaner = ProductCleaner()
    stats = cleaner.run()

    # Exit voi error code neu khong co du lieu
    if stats["final_count"] == 0:
        logger.warning("No products were cleaned and saved")
        sys.exit(1)


if __name__ == "__main__":
    main()
