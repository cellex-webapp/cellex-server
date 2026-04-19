"""
Vector Store
------------
Quan ly vector embeddings cho RAG system.
Su dung ChromaDB de luu tru va truy van.
"""

import os
from typing import Optional, List, Dict, Any
from pathlib import Path
import chromadb
from chromadb.config import Settings
from loguru import logger


class VectorStore:
    """
    Vector store de luu tru va truy van embeddings.
    """

    def __init__(self, persist_directory: str = "./vector_store"):
        """
        Khoi tao vector store.

        Args:
            persist_directory: Thu muc luu tru ChromaDB
        """
        self.persist_directory = Path(persist_directory)
        self.persist_directory.mkdir(parents=True, exist_ok=True)

        # Khoi tao ChromaDB client
        self.client = chromadb.PersistentClient(
            path=str(self.persist_directory),
            settings=Settings(anonymized_telemetry=False),
        )

        # Collections
        self.products_collection = None
        self.reviews_collection = None
        self.docs_collection = None

        logger.info(f"VectorStore initialized at {self.persist_directory}")

    def get_or_create_collection(
        self, collection_name: str, metadata: Optional[Dict] = None
    ) -> chromadb.Collection:
        """
        Lay hoac tao collection.

        Args:
            collection_name: Ten collection
            metadata: Metadata cho collection

        Returns:
            ChromaDB collection
        """
        return self.client.get_or_create_collection(
            name=collection_name, metadata=metadata or {}
        )

    def index_products(self, products: List[Dict[str, Any]]) -> int:
        """
        Index products vao vector store.

        Args:
            products: List cac product documents

        Returns:
            So luong products da index
        """
        if not products:
            logger.warning("No products to index")
            return 0

        self.products_collection = self.get_or_create_collection(
            "products", {"description": "Product catalog embeddings"}
        )

        # Prepare data
        ids = []
        documents = []
        metadatas = []

        for product in products:
            product_id = str(product.get("id") or product.get("_id"))
            ids.append(product_id)

            # Tao document text cho embedding
            doc_parts = [
                f"Title: {product.get('title', '')}",
                f"Brand: {product.get('brand', '')}",
                f"Category: {product.get('category', '')}",
                f"Description: {product.get('description', '')}",
            ]

            # Them specifications
            specs = product.get("specifications", {})
            if specs:
                spec_text = ", ".join([f"{k}: {v}" for k, v in specs.items()])
                doc_parts.append(f"Specifications: {spec_text}")

            documents.append("\n".join(doc_parts))

            # Metadata
            metadatas.append(
                {
                    "product_id": product_id,
                    "title": product.get("title", ""),
                    "brand": product.get("brand", ""),
                    "category": product.get("category", ""),
                    "price": float(product.get("price", 0)),
                    "rating": float(product.get("rating", 0)),
                }
            )

        # Add to collection
        try:
            self.products_collection.add(
                ids=ids, documents=documents, metadatas=metadatas
            )
            logger.info(f"Indexed {len(ids)} products into vector store")
            return len(ids)
        except Exception as e:
            logger.error(f"Failed to index products: {e}")
            return 0

    def search_products(
        self, query: str, top_k: int = 5, filters: Optional[Dict] = None
    ) -> List[Dict[str, Any]]:
        """
        Tim kiem products gan nhat voi query.

        Args:
            query: Search query
            top_k: So luong ket qua
            filters: Metadata filters (vd: {"category": "Laptop"})

        Returns:
            List cac products voi distance scores
        """
        if self.products_collection is None:
            logger.warning("Products collection not initialized")
            return []

        try:
            results = self.products_collection.query(
                query_texts=[query], n_results=top_k, where=filters
            )

            # Format results
            products = []
            if results["ids"] and len(results["ids"]) > 0:
                for i, product_id in enumerate(results["ids"][0]):
                    products.append(
                        {
                            "product_id": product_id,
                            "distance": results["distances"][0][i],
                            "metadata": results["metadatas"][0][i],
                            "document": results["documents"][0][i],
                        }
                    )

            return products

        except Exception as e:
            logger.error(f"Product search failed: {e}")
            return []

    def index_reviews(self, reviews: List[Dict[str, Any]]) -> int:
        """
        Index reviews vao vector store.

        Args:
            reviews: List cac review documents

        Returns:
            So luong reviews da index
        """
        if not reviews:
            logger.warning("No reviews to index")
            return 0

        self.reviews_collection = self.get_or_create_collection(
            "reviews", {"description": "Product reviews"}
        )

        ids = []
        documents = []
        metadatas = []

        for review in reviews:
            review_id = str(review.get("id") or review.get("_id"))
            ids.append(review_id)

            # Review text
            doc = f"Rating: {review.get('rating', 0)}/5\n"
            doc += f"Review: {review.get('review_text', '')}"
            documents.append(doc)

            metadatas.append(
                {
                    "review_id": review_id,
                    "product_id": str(review.get("product_id", "")),
                    "user_id": str(review.get("user_id", "")),
                    "rating": int(review.get("rating", 0)),
                }
            )

        try:
            self.reviews_collection.add(
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
        """
        Tim kiem reviews gan nhat voi query.

        Args:
            query: Search query
            product_id: Filter by product_id
            top_k: So luong ket qua

        Returns:
            List reviews
        """
        if self.reviews_collection is None:
            return []

        filters = {"product_id": product_id} if product_id else None

        try:
            results = self.reviews_collection.query(
                query_texts=[query], n_results=top_k, where=filters
            )

            reviews = []
            if results["ids"] and len(results["ids"]) > 0:
                for i, review_id in enumerate(results["ids"][0]):
                    reviews.append(
                        {
                            "review_id": review_id,
                            "distance": results["distances"][0][i],
                            "metadata": results["metadatas"][0][i],
                            "document": results["documents"][0][i],
                        }
                    )

            return reviews

        except Exception as e:
            logger.error(f"Review search failed: {e}")
            return []

    def delete_collection(self, collection_name: str) -> bool:
        """
        Xoa collection.

        Args:
            collection_name: Ten collection

        Returns:
            True neu thanh cong
        """
        try:
            self.client.delete_collection(collection_name)
            logger.info(f"Deleted collection: {collection_name}")
            return True
        except Exception as e:
            logger.error(f"Failed to delete collection {collection_name}: {e}")
            return False

    def get_collection_stats(self) -> Dict[str, Any]:
        """
        Lay thong tin thong ke ve collections.

        Returns:
            Dict chua stats
        """
        collections = self.client.list_collections()

        stats = {"total_collections": len(collections), "collections": []}

        for col in collections:
            col_info = {
                "name": col.name,
                "count": col.count(),
                "metadata": col.metadata,
            }
            stats["collections"].append(col_info)

        return stats
