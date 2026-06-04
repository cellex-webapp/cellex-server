"""
Image Search API Routes
-----------------------
FastAPI endpoints cho tìm kiếm sản phẩm bằng hình ảnh.

Endpoints:
  POST /api/v1/ml/image-search/index         - Index ảnh sản phẩm (từ URL)
  POST /api/v1/ml/image-search/search        - Tìm kiếm bằng ảnh upload
  DELETE /api/v1/ml/image-search/{product_id} - Xóa embeddings của product
  GET  /api/v1/ml/image-search/health        - Health check
  GET  /api/v1/ml/image-search/stats         - Thống kê
"""
from __future__ import annotations

import io
from typing import List, Optional

from fastapi import APIRouter, File, HTTPException, UploadFile
from loguru import logger
from PIL import Image
from pydantic import BaseModel, Field

from ..config import settings
from ..image_search.clip_model import clip_manager
from ..image_search.pgvector_client import pgvector_client

router = APIRouter(prefix="/api/v1/ml/image-search", tags=["image-search"])


# ── Request / Response Schemas ────────────────────────────────────────────────

class IndexRequest(BaseModel):
    product_id: str = Field(..., description="MongoDB Product ID")
    image_urls: List[str] = Field(..., description="Danh sách URL ảnh từ Cloudinary")


class IndexResponse(BaseModel):
    success: bool
    product_id: str
    indexed_count: int
    failed_count: int
    message: str


class SearchResultItem(BaseModel):
    product_id: str
    similarity_score: float
    rank: int


class SearchResponse(BaseModel):
    success: bool
    results: List[SearchResultItem]
    total: int
    message: str


class DeleteResponse(BaseModel):
    success: bool
    product_id: str
    deleted_count: int
    message: str


# ── Endpoints ─────────────────────────────────────────────────────────────────

@router.post("/index", response_model=IndexResponse)
async def index_product_images(request: IndexRequest):
    """
    Index ảnh của một sản phẩm vào pgvector.

    Được gọi từ Spring Boot sau khi sản phẩm được tạo/cập nhật.
    Với mỗi URL trong image_urls: tải ảnh → CLIP encode → lưu vào Supabase.

    Body example:
```json
    {
      "product_id": "64abc123...",
      "image_urls": [
        "https://res.cloudinary.com/.../product1.jpg"
      ]
    }
```
    """
    if not request.image_urls:
        return IndexResponse(
            success=False,
            product_id=request.product_id,
            indexed_count=0,
            failed_count=0,
            message="Không có URL ảnh nào được cung cấp.",
        )

    indexed = 0
    failed = 0

    for url in request.image_urls:
        try:
            embedding = clip_manager.encode_image_from_url(url)
            ok = pgvector_client.upsert_embedding(
                product_id=request.product_id,
                image_url=url,
                embedding=embedding,
            )
            if ok:
                indexed += 1
            else:
                failed += 1
        except Exception as e:
            logger.warning(f"Lỗi index ảnh {url} cho product {request.product_id}: {e}")
            failed += 1

    success = indexed > 0
    message = (
        f"Đã index {indexed}/{len(request.image_urls)} ảnh thành công."
        if success
        else "Không thể index ảnh nào."
    )

    logger.info(f"Index product {request.product_id}: {indexed} OK, {failed} failed")
    return IndexResponse(
        success=success,
        product_id=request.product_id,
        indexed_count=indexed,
        failed_count=failed,
        message=message,
    )


@router.post("/search", response_model=SearchResponse)
async def search_by_image(
    file: UploadFile = File(..., description="File ảnh để tìm kiếm (JPEG/PNG/WEBP)"),
    top_k: int = settings.image_search_top_k,
):
    """
    Tìm kiếm sản phẩm tương tự bằng ảnh upload.

    Nhận file ảnh từ Spring Boot (multipart/form-data),
    encode thành CLIP embedding, tìm kiếm cosine similarity trên pgvector.

    Returns danh sách product_id và similarity_score theo thứ tự tương đồng giảm dần.
    """
    # Validate file type
    if file.content_type and file.content_type not in (
        "image/jpeg", "image/png", "image/webp", "image/gif", "image/bmp"
    ):
        raise HTTPException(
            status_code=400,
            detail=f"Loại file không hỗ trợ: {file.content_type}. Chỉ chấp nhận ảnh.",
        )

    try:
        contents = await file.read()
        if len(contents) == 0:
            raise HTTPException(status_code=400, detail="File ảnh rỗng.")

        image = Image.open(io.BytesIO(contents)).convert("RGB")
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"Lỗi đọc ảnh upload: {e}")
        raise HTTPException(status_code=400, detail=f"Không thể đọc file ảnh: {str(e)}")

    try:
        query_embedding = clip_manager.encode_image(image)
    except Exception as e:
        logger.error(f"Lỗi encode ảnh tìm kiếm: {e}")
        raise HTTPException(status_code=500, detail="Lỗi xử lý ảnh với CLIP model.")

    similar = pgvector_client.search_similar(
        query_embedding=query_embedding,
        top_k=min(top_k, 50),
        min_score=settings.image_search_min_score,
    )

    results = [
        SearchResultItem(
            product_id=pid,
            similarity_score=round(score, 4),
            rank=rank + 1,
        )
        for rank, (pid, score) in enumerate(similar)
    ]

    logger.info(f"Image search: tìm thấy {len(results)} sản phẩm tương tự.")
    return SearchResponse(
        success=True,
        results=results,
        total=len(results),
        message=f"Tìm thấy {len(results)} sản phẩm tương tự.",
    )


@router.delete("/{product_id}", response_model=DeleteResponse)
async def delete_product_embeddings(product_id: str):
    """
    Xóa tất cả embeddings của một sản phẩm.
    Được gọi từ Spring Boot khi sản phẩm bị xóa.
    """
    try:
        deleted = pgvector_client.delete_by_product_id(product_id)
        logger.info(f"Xóa {deleted} embeddings của product {product_id}")
        return DeleteResponse(
            success=True,
            product_id=product_id,
            deleted_count=deleted,
            message=f"Đã xóa {deleted} embeddings.",
        )
    except Exception as e:
        logger.error(f"Lỗi xóa embeddings product {product_id}: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@router.get("/health")
async def health_check():
    """Health check cho image search service."""
    pg_connected = pgvector_client.pg.is_connected
    model_loaded = clip_manager._model is not None
    total_embeddings = pgvector_client.count_embeddings() if pg_connected else -1

    return {
        "status": "healthy" if (pg_connected and model_loaded) else "degraded",
        "clip_model_loaded": model_loaded,
        "pgvector_connected": pg_connected,
        "total_embeddings": total_embeddings,
    }


@router.get("/stats")
async def get_stats():
    """Thống kê về số lượng embeddings đã index."""
    total = pgvector_client.count_embeddings()
    return {
        "total_embeddings": total,
        "clip_model": settings.clip_model_name,
        "embedding_dim": 512,
        "top_k_default": settings.image_search_top_k,
    }
