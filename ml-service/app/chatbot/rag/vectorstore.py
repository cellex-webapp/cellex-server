"""
VectorStore
-----------
Quan ly vector embeddings cho RAG system. Su dung ChromaDB.

Cập nhật: index_products giờ dùng field names đúng với Product.java (MongoDB):
  name, description, images, price, finalPrice, averageRating, reviewCount, shopId, categoryId
"""

import os
from typing import Optional, List, Dict, Any
from pathlib import Path
import chromadb
from chromadb.config import Settings
from loguru import logger


class VectorStore:
    """Vector store de luu tru va truy van embeddings."""

    def __init__(self, persist_directory: str = "./vector_store"):
        self.persist_directory = Path(persist_directory)
        self.persist_directory.mkdir(parents=True, exist_ok=True)

        self.client = chromadb.PersistentClient(
            path=str(self.persist_directory),
            settings=Settings(anonymized_telemetry=False),
        )

        # Collections (lazy-init)
        self.products_collection = None
        self.reviews_collection = None

        logger.info(f"VectorStore initialized at {self.persist_directory}")

    def get_or_create_collection(
        self, collection_name: str, metadata: Optional[Dict] = None
    ) -> chromadb.Collection:
        return self.client.get_or_create_collection(
            name=collection_name, metadata=metadata or {}
        )

    def index_products(self, products: List[Dict[str, Any]]) -> int:
        """
        Index products vao vector store.
        Nhận product documents từ MongoDB (Product.java schema):
            name, description, images, price, finalPrice, averageRating, reviewCount, shopId, categoryId
        """
        if not products:
            logger.warning("No products to index")
            return 0

        self.products_collection = self.get_or_create_collection(
            "products", {"description": "Product catalog embeddings"}
        )

        ids = []
        documents = []
        metadatas = []

        for product in products:
            product_id = str(product.get("id") or product.get("_id"))
            ids.append(product_id)

            # Tao document text cho embedding — dùng field names của Product.java
            doc_parts = [
                f"Ten san pham: {product.get('name', '')}",
                f"Mo ta: {product.get('description', '')}",
            ]

            # Thuộc tính sản phẩm
            attribute_values = product.get("attributeValues") or []
            if attribute_values:
                attrs_text = ", ".join(
                    [f"{av.get('attributeName', '')}: {av.get('value', '')} {av.get('unit', '')}"
                     for av in attribute_values if av.get("value")]
                )
                if attrs_text:
                    doc_parts.append(f"Thong so ky thuat: {attrs_text}")

            documents.append("\n".join(doc_parts))

            # Metadata — chỉ dùng primitive types cho ChromaDB
            brand = "N/A"
            for av in attribute_values:
                # Check commonly used brand keys
                if av.get("attributeKey") in ["brand", "thuong-hieu"] or av.get("attributeName") in ["Brand", "Thương hiệu"]:
                    brand = str(av.get("value", "N/A"))
                    break

            metadatas.append({
                "product_id": product_id,
                "name": str(product.get("name", "")),
                "brand": brand,
                "shop_id": str(product.get("shopId", "")),
                "category_id": str(product.get("categoryId", "")),
                "price": float(product.get("price") or 0),
                "final_price": float(product.get("finalPrice") or product.get("price") or 0),
                "average_rating": float(product.get("averageRating") or 0),
                "review_count": int(product.get("reviewCount") or 0),
                "is_published": bool(product.get("isPublished", True)),
            })

        # Add to collection — handle duplicates gracefully
        try:
            existing_ids = set(self.products_collection.get(ids=ids)["ids"])
            new_ids = [i for i in ids if i not in existing_ids]
            new_docs = [documents[ids.index(i)] for i in new_ids]
            new_metas = [metadatas[ids.index(i)] for i in new_ids]

            if new_ids:
                self.products_collection.add(
                    ids=new_ids, documents=new_docs, metadatas=new_metas
                )

            # Upsert existing ones
            existing_list = [i for i in ids if i in existing_ids]
            if existing_list:
                upd_docs = [documents[ids.index(i)] for i in existing_list]
                upd_metas = [metadatas[ids.index(i)] for i in existing_list]
                self.products_collection.update(
                    ids=existing_list, documents=upd_docs, metadatas=upd_metas
                )

            logger.info(f"Indexed/updated {len(ids)} products into vector store")
            return len(ids)
        except Exception as e:
            # Fallback: upsert all
            try:
                self.products_collection.upsert(
                    ids=ids, documents=documents, metadatas=metadatas
                )
                logger.info(f"Upserted {len(ids)} products into vector store")
                return len(ids)
            except Exception as e2:
                logger.error(f"Failed to index products: {e2}")
                return 0

    def search_products(
        self, query: str, top_k: int = 5, filters: Optional[Dict] = None
    ) -> List[Dict[str, Any]]:
        """Tim kiem products gan nhat voi query."""
        if self.products_collection is None:
            try:
                self.products_collection = self.client.get_collection("products")
            except Exception:
                logger.warning("Products collection not initialized, run /index-products first")
                return []

        try:
            results = self.products_collection.query(
                query_texts=[query],
                n_results=min(top_k, max(1, self.products_collection.count())),
                where=filters,
            )

            products = []
            if results["ids"] and len(results["ids"]) > 0:
                for i, product_id in enumerate(results["ids"][0]):
                    products.append({
                        "product_id": product_id,
                        "distance": results["distances"][0][i],
                        "metadata": results["metadatas"][0][i],
                        "document": results["documents"][0][i],
                    })

            return products

        except Exception as e:
            logger.error(f"Product search failed: {e}")
            return []

    def index_reviews(self, reviews: List[Dict[str, Any]]) -> int:
        """Index reviews vao vector store."""
        if not reviews:
            return 0

        self.reviews_collection = self.get_or_create_collection(
            "reviews", {"description": "Product reviews"}
        )

        ids, documents, metadatas = [], [], []

        for review in reviews:
            review_id = str(review.get("id") or review.get("_id"))
            ids.append(review_id)

            doc = f"Rating: {review.get('rating', 0)}/5\nReview: {review.get('content', review.get('reviewText', ''))}"
            documents.append(doc)

            metadatas.append({
                "review_id": review_id,
                "product_id": str(review.get("productId", "")),
                "user_id": str(review.get("userId", "")),
                "rating": int(review.get("rating", 0)),
            })

        try:
            self.reviews_collection.upsert(
                ids=ids, documents=documents, metadatas=metadatas
            )
            logger.info(f"Indexed {len(ids)} reviews")
            return len(ids)
        except Exception as e:
            logger.error(f"Failed to index reviews: {e}")
            return 0

    def search_reviews(
        self, query: str, product_id: Optional[str] = None, top_k: int = 5
    ) -> List[Dict[str, Any]]:
        """Tim kiem reviews."""
        if self.reviews_collection is None:
            try:
                self.reviews_collection = self.client.get_collection("reviews")
            except Exception:
                return []

        filters = {"product_id": product_id} if product_id else None

        try:
            results = self.reviews_collection.query(
                query_texts=[query],
                n_results=min(top_k, max(1, self.reviews_collection.count())),
                where=filters,
            )

            reviews = []
            if results["ids"] and len(results["ids"]) > 0:
                for i, review_id in enumerate(results["ids"][0]):
                    reviews.append({
                        "review_id": review_id,
                        "distance": results["distances"][0][i],
                        "metadata": results["metadatas"][0][i],
                        "document": results["documents"][0][i],
                    })

            return reviews
        except Exception as e:
            logger.error(f"Review search failed: {e}")
            return []

    def delete_collection(self, collection_name: str) -> bool:
        try:
            self.client.delete_collection(collection_name)
            logger.info(f"Deleted collection: {collection_name}")
            return True
        except Exception as e:
            logger.error(f"Failed to delete collection {collection_name}: {e}")
            return False

    def get_collection_stats(self) -> Dict[str, Any]:
        """Lay thong tin thong ke ve collections."""
        collections = self.client.list_collections()
        stats = {"total_collections": len(collections), "collections": []}

        for col in collections:
            stats["collections"].append({
                "name": col.name,
                "count": col.count(),
                "metadata": col.metadata,
            })

        return stats
