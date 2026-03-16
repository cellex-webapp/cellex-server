"""
routes.py
~~~~~~~~~
FastAPI endpoints exposed by the ML recommendation service.
"""

import logging
from typing import Optional

from fastapi import APIRouter, HTTPException, Query
from pydantic import BaseModel

from app.data.data_loader import load_interactions_df, load_products_map
from app.data.mock_generator import generate as generate_mock_data
from app.evaluation.metrics import evaluate_model
from app.models.svd_model import recommender

logger = logging.getLogger(__name__)
router = APIRouter()


# ── Response schemas ───────────────────────────────────────

class RecommendationItem(BaseModel):
    product_id: str
    product_name: Optional[str] = None
    product_image: Optional[str] = None
    price: Optional[float] = None
    final_price: Optional[float] = None
    average_rating: Optional[float] = None
    review_count: Optional[int] = None
    score: float
    rank: int
    recommendation_reason: str = "ML_SVD_PLUS_PLUS"
    explanation: str = "Goi y tu mo hinh AI SVD++ dua tren hanh vi nguoi dung tuong tu"


class TrainResponse(BaseModel):
    message: str
    meta: dict


class MockDataResponse(BaseModel):
    message: str
    summary: dict


class EvaluationResponse(BaseModel):
    message: str
    metrics: dict


class SimilarProductItem(BaseModel):
    product_id: str
    product_name: Optional[str] = None
    similarity: float
    rank: int


# ── Endpoints ──────────────────────────────────────────────

@router.post("/train", response_model=TrainResponse)
def train_model():
    """Trigger SVD++ training from the current MongoDB interaction data."""
    try:
        meta = recommender.train()
        return TrainResponse(message="Training completed successfully.", meta=meta)
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))
    except Exception as e:
        logger.exception("Training failed")
        raise HTTPException(status_code=500, detail=f"Training failed: {e}")


@router.get("/recommendations/{user_id}", response_model=list[RecommendationItem])
def get_recommendations(
    user_id: str,
    limit: int = Query(default=20, ge=1, le=100),
    category_id: Optional[str] = Query(default=None),
):
    """Get top-N product recommendations for a user via the trained SVD++ model."""
    if recommender.model is None:
        raise HTTPException(status_code=503, detail="Model not trained yet. Call POST /train first.")

    try:
        raw = recommender.recommend(user_id=user_id, n=limit * 2)
    except RuntimeError as e:
        raise HTTPException(status_code=503, detail=str(e))

    products_map = load_products_map()

    results: list[RecommendationItem] = []
    for item in raw:
        pid = item["product_id"]
        pinfo = products_map.get(pid, {})

        # Optional category filter
        if category_id and pinfo.get("categoryId") != category_id:
            continue

        images = pinfo.get("images", [])
        results.append(
            RecommendationItem(
                product_id=pid,
                product_name=pinfo.get("name"),
                product_image=images[0] if images else None,
                price=pinfo.get("price"),
                final_price=pinfo.get("finalPrice"),
                average_rating=pinfo.get("averageRating"),
                review_count=pinfo.get("reviewCount"),
                score=item["score"],
                rank=len(results) + 1,
            )
        )
        if len(results) >= limit:
            break

    return results


@router.get("/similar/{product_id}", response_model=list[SimilarProductItem])
def get_similar_products(
    product_id: str,
    limit: int = Query(default=10, ge=1, le=50),
):
    """Get products similar to the given product based on SVD++ latent factors."""
    if recommender.model is None:
        raise HTTPException(status_code=503, detail="Model not trained yet.")

    raw = recommender.similar_products(product_id=product_id, n=limit)
    products_map = load_products_map()

    results = []
    for item in raw:
        pid = item["product_id"]
        pinfo = products_map.get(pid, {})
        results.append(
            SimilarProductItem(
                product_id=pid,
                product_name=pinfo.get("name"),
                similarity=item["similarity"],
                rank=item["rank"],
            )
        )
    return results


@router.get("/metrics", response_model=EvaluationResponse)
def get_evaluation_metrics():
    """Evaluate the current model with Precision@K, Recall@K, NDCG@K."""
    if recommender.model is None:
        raise HTTPException(status_code=503, detail="Model not trained yet.")

    try:
        metrics = evaluate_model()
        return EvaluationResponse(message="Evaluation completed.", metrics=metrics)
    except Exception as e:
        logger.exception("Evaluation failed")
        raise HTTPException(status_code=500, detail=f"Evaluation failed: {e}")


@router.post("/mock-data", response_model=MockDataResponse)
def generate_mock(
    n_users: int = Query(default=200, ge=10),
    n_products: int = Query(default=0, ge=0, description="0 = use real products from MongoDB"),
    density: float = Query(default=0.08, ge=0.01, le=0.5),
    seed: int = Query(default=42),
    clear: bool = Query(default=False, description="Clear existing interactions first"),
):
    """Generate mock user interaction data in MongoDB."""
    try:
        summary = generate_mock_data(
            n_users=n_users,
            n_synthetic_products=n_products,
            density=density,
            seed=seed,
            clear_existing=clear,
        )
        return MockDataResponse(message="Mock data generated.", summary=summary)
    except Exception as e:
        logger.exception("Mock data generation failed")
        raise HTTPException(status_code=500, detail=f"Mock data generation failed: {e}")


@router.get("/model-info")
def model_info():
    """Return metadata about the currently loaded model."""
    if recommender.model is None:
        return {"status": "no_model", "meta": {}}
    return {"status": "loaded", "meta": recommender.meta}
