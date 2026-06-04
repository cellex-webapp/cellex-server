"""
PgVector Client
---------------
Thao tác với bảng product_image_embeddings trên Supabase PostgreSQL.
"""
from __future__ import annotations

from typing import List, Tuple, Optional

import numpy as np
from loguru import logger
from sqlalchemy import text

from ..database.postgres import get_postgres_client
from ..config import settings


class PgVectorClient:
    """Client thao tác với pgvector embeddings."""

    def __init__(self) -> None:
        self.pg = get_postgres_client()

    def upsert_embedding(
        self,
        product_id: str,
        image_url: str,
        embedding: np.ndarray,
    ) -> bool:
        """
        Lưu hoặc cập nhật embedding cho một ảnh sản phẩm.
        Dùng ON CONFLICT (product_id, image_url) DO UPDATE.
        """
        if not self.pg.is_connected:
            logger.error("PostgreSQL chưa kết nối, không thể lưu embedding.")
            return False

        if len(image_url) > 1000:
            import hashlib
            image_url_hash = hashlib.md5(image_url.encode('utf-8')).hexdigest()
            image_url = f"base64_{image_url_hash}"

        try:
            embedding_list = embedding.tolist()
            sql = text("""
                INSERT INTO product_image_embeddings (product_id, image_url, embedding, updated_at)
                VALUES (:product_id, :image_url, CAST(:embedding AS vector), NOW())
                ON CONFLICT (product_id, image_url)
                DO UPDATE SET
                    embedding = EXCLUDED.embedding,
                    updated_at = NOW()
            """)
            with self.pg._engine.connect() as conn:
                conn.execute(sql, {
                    "product_id": product_id,
                    "image_url": image_url,
                    "embedding": str(embedding_list),
                })
                conn.commit()
            return True
        except Exception as e:
            logger.error(f"Lỗi upsert embedding product {product_id}: {e}")
            return False

    def delete_by_product_id(self, product_id: str) -> int:
        """Xóa tất cả embeddings của một sản phẩm (khi product bị xóa)."""
        if not self.pg.is_connected:
            return 0
        try:
            sql = text(
                "DELETE FROM product_image_embeddings WHERE product_id = :product_id"
            )
            with self.pg._engine.connect() as conn:
                result = conn.execute(sql, {"product_id": product_id})
                conn.commit()
                return result.rowcount
        except Exception as e:
            logger.error(f"Lỗi xóa embeddings product {product_id}: {e}")
            return 0

    def search_similar(
        self,
        query_embedding: np.ndarray,
        top_k: int = 20,
        min_score: float = 0.0,
    ) -> List[Tuple[str, float]]:
        """
        Tìm kiếm sản phẩm tương tự bằng cosine similarity.

        Returns:
            List of (product_id, similarity_score), sorted by score desc.
        """
        if not self.pg.is_connected:
            logger.error("PostgreSQL chưa kết nối.")
            return []

        try:
            embedding_list = query_embedding.tolist()
            # 1 - cosine_distance = cosine_similarity
            sql = text("""
                SELECT
                    product_id,
                    1 - (embedding <=> CAST(:query_embedding AS vector)) AS similarity
                FROM product_image_embeddings
                ORDER BY embedding <=> CAST(:query_embedding AS vector)
                LIMIT :top_k
            """)
            with self.pg._engine.connect() as conn:
                rows = conn.execute(sql, {
                    "query_embedding": str(embedding_list),
                    "top_k": top_k,
                }).fetchall()

            results = [
                (row[0], float(row[1]))
                for row in rows
                if float(row[1]) >= min_score
            ]
            # Deduplicate: giữ score cao nhất per product_id
            seen: dict[str, float] = {}
            for pid, score in results:
                if pid not in seen or score > seen[pid]:
                    seen[pid] = score

            deduped = sorted(seen.items(), key=lambda x: x[1], reverse=True)
            return deduped[:top_k]

        except Exception as e:
            logger.error(f"Lỗi tìm kiếm similarity: {e}")
            return []

    def count_embeddings(self) -> int:
        """Đếm tổng số embeddings đã lưu."""
        if not self.pg.is_connected:
            return 0
        try:
            df = self.pg.query_df("SELECT COUNT(*) as cnt FROM product_image_embeddings")
            return int(df.iloc[0]["cnt"]) if not df.empty else 0
        except Exception:
            return 0


# Module-level singleton
pgvector_client = PgVectorClient()
