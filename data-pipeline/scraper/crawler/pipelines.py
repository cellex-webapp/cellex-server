"""
Scrapy Pipelines
----------------
Xu ly va luu tru items vao MongoDB.
Day la Bronze layer trong Medallion Architecture - raw data.
"""

import re
from datetime import datetime, timezone
from typing import Any

from itemadapter import ItemAdapter
from loguru import logger
from pymongo import MongoClient
from pymongo.errors import ConnectionFailure


class ValidatePipeline:
    """
    Pipeline kiem tra va validate item truoc khi luu.
    Loai bo cac item khong hop le.
    """

    def process_item(self, item: Any, spider: Any) -> Any:
        """
        Validate item co cac truong bat buoc.

        Args:
            item: Scrapy item can validate
            spider: Spider dang chay

        Returns:
            Item neu hop le

        Raises:
            DropItem: Neu item khong hop le
        """
        adapter = ItemAdapter(item)

        # Kiem tra cac truong bat buoc
        required_fields = ["external_id", "title", "source"]

        for field in required_fields:
            if not adapter.get(field):
                from scrapy.exceptions import DropItem

                logger.warning(f"Missing required field '{field}' - dropping item")
                raise DropItem(f"Missing required field: {field}")

        # Clean va normalize title
        if adapter.get("title"):
            # Loai bo khoang trang thua
            adapter["title"] = " ".join(adapter["title"].split())

        # Clean price - chuyen ve dang so
        if adapter.get("price"):
            price_str = str(adapter["price"])
            # Loai bo cac ky tu khong phai so (giu lai dau cham)
            price_clean = re.sub(r"[^\d.]", "", price_str)
            if price_clean:
                try:
                    adapter["price"] = float(price_clean)
                except ValueError:
                    adapter["price"] = None

        # Dam bao crawl_time co gia tri
        if not adapter.get("crawl_time"):
            adapter["crawl_time"] = datetime.now(timezone.utc).isoformat()

        return item


class MongoPipeline:
    """
    Pipeline luu item vao MongoDB.
    Su dung upsert de tranh duplicate trong cung mot session crawl.
    """

    def __init__(self, mongo_uri: str, mongo_db: str, mongo_collection: str):
        """
        Khoi tao MongoDB Pipeline.

        Args:
            mongo_uri: MongoDB connection URI
            mongo_db: Ten database
            mongo_collection: Ten collection luu raw data
        """
        self.mongo_uri = mongo_uri
        self.mongo_db = mongo_db
        self.mongo_collection = mongo_collection
        self.client = None
        self.db = None
        self.collection = None
        self.items_count = 0

    @classmethod
    def from_crawler(cls, crawler):
        """
        Tao instance tu Scrapy crawler settings.
        """
        return cls(
            mongo_uri=crawler.settings.get("MONGO_URI"),
            mongo_db=crawler.settings.get("MONGO_DATABASE", "cellex_prod"),
            mongo_collection=crawler.settings.get(
                "MONGO_COLLECTION", "raw_products"
            ),
        )

    def open_spider(self, spider: Any) -> None:
        """
        Mo ket noi MongoDB khi spider bat dau.

        Args:
            spider: Spider dang chay
        """
        logger.info(f"Connecting to MongoDB: {self.mongo_db}/{self.mongo_collection}")

        try:
            self.client = MongoClient(
                self.mongo_uri,
                serverSelectionTimeoutMS=5000,
                connectTimeoutMS=5000,
            )
            # Test connection
            self.client.admin.command("ping")
            logger.info("MongoDB connection successful")

            self.db = self.client[self.mongo_db]
            self.collection = self.db[self.mongo_collection]

            # Tao index cho external_id va source de tang toc truy van
            self.collection.create_index(
                [("external_id", 1), ("source", 1)], unique=True
            )
            logger.info("Created index on (external_id, source)")

        except ConnectionFailure as e:
            logger.error(f"Failed to connect to MongoDB: {e}")
            raise

    def close_spider(self, spider: Any) -> None:
        """
        Dong ket noi MongoDB khi spider ket thuc.

        Args:
            spider: Spider dang chay
        """
        if self.client:
            self.client.close()
            logger.info(f"Spider closed. Total items saved: {self.items_count}")

    def process_item(self, item: Any, spider: Any) -> Any:
        """
        Luu item vao MongoDB su dung upsert.

        Args:
            item: Scrapy item can luu
            spider: Spider dang chay

        Returns:
            Item da xu ly
        """
        adapter = ItemAdapter(item)
        data = dict(adapter)

        # Them metadata
        data["_updated_at"] = datetime.now(timezone.utc)

        # Upsert: update neu ton tai, insert neu chua co
        result = self.collection.update_one(
            {"external_id": data["external_id"], "source": data["source"]},
            {"$set": data, "$setOnInsert": {"_created_at": datetime.now(timezone.utc)}},
            upsert=True,
        )

        if result.upserted_id:
            logger.debug(f"Inserted new product: {data['external_id']}")
        else:
            logger.debug(f"Updated existing product: {data['external_id']}")

        self.items_count += 1

        return item
