"""
routes.py
~~~~~~~~~
FastAPI endpoints exposed by the ML recommendation service.

Endpoints grouped by functionality:
    - /train, /model-info, /versions: Model management
    - /recommendations, /hybrid: Getting recommendations
    - /similar: Similar products
    - /trending, /latest: Popularity-based
    - /tune: Hyperparameter tuning
    - /metrics: Model evaluation
    - /mock-data: Test data generation
"""

import logging
from typing import Optional

from fastapi import APIRouter, HTTPException, Query, BackgroundTasks
from pydantic import BaseModel, Field

from app.config import settings
from app.data.data_loader import load_interactions_df, load_products_map
from app.data.mock_generator import generate as generate_mock_data
from app.evaluation.metrics import evaluate_model
from app.models.svd_model import recommender
from app.models.model_store import list_versions, rollback_to_version, get_model_info
from app.models.tuning import tuner
from app.services.popularity_service import popularity_service
from app.services.hybrid_recommender import hybrid_recommender

logger = logging.getLogger(__name__)
router = APIRouter()


# ══════════════════════════════════════════════════════════════════════════════
# Response Schemas
# ══════════════════════════════════════════════════════════════════════════════

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


class HybridRecommendationItem(RecommendationItem):
    svd_score: Optional[float] = None
    popularity_score: Optional[float] = None


class SimilarProductItem(BaseModel):
    product_id: str
    product_name: Optional[str] = None
    product_image: Optional[str] = None
    price: Optional[float] = None
    final_price: Optional[float] = None
    similarity: float
    rank: int
    recommendation_reason: str = "ML_LATENT_SIMILARITY"


class TrendingProductItem(BaseModel):
    product_id: str
    product_name: Optional[str] = None
    product_image: Optional[str] = None
    price: Optional[float] = None
    final_price: Optional[float] = None
    popularity_score: float
    rank: int
    recommendation_reason: str = "TRENDING"


class TrainResponse(BaseModel):
    message: str
    meta: dict


class TrainWithTuningResponse(BaseModel):
    message: str
    meta: dict
    tuning_results: Optional[dict] = None


class MockDataResponse(BaseModel):
    message: str
    summary: dict


class EvaluationResponse(BaseModel):
    message: str
    metrics: dict


class TuningResponse(BaseModel):
    message: str
    best_params: Optional[dict]
    best_rmse: float
    all_results: list[dict]


class ModelVersionResponse(BaseModel):
    version: str
    timestamp: str
    size_mb: float
    meta: dict


class RollbackResponse(BaseModel):
    message: str
    success: bool


class PopularityStatsResponse(BaseModel):
    total_products: int
    avg_score: Optional[float] = None
    max_score: Optional[float] = None
    min_score: Optional[float] = None
    cache_age_seconds: Optional[float] = None


# ══════════════════════════════════════════════════════════════════════════════
# Model Management Endpoints
# ══════════════════════════════════════════════════════════════════════════════

@router.post("/train", response_model=TrainResponse)
def train_model(
    use_tuning: bool = Query(default=False, description="Run hyperparameter tuning before training")
):
    """
    Train SVD++ model from MongoDB interaction data.

    Set use_tuning=true to run hyperparameter tuning first (slower but better results).
    """
    try:
        if use_tuning:
            logger.info("Running hyperparameter tuning before training...")
            tuning_result = tuner.tune()
            best_params = tuning_result.get("best_params", {})

            # Update settings with best params
            if best_params:
                settings.svd_n_factors = best_params.get("n_factors", settings.svd_n_factors)
                settings.svd_n_epochs = best_params.get("n_epochs", settings.svd_n_epochs)
                settings.svd_lr_all = best_params.get("lr_all", settings.svd_lr_all)
                settings.svd_reg_all = best_params.get("reg_all", settings.svd_reg_all)

        meta = recommender.train()
        return TrainResponse(message="Training completed successfully.", meta=meta)

    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))
    except Exception as e:
        logger.exception("Training failed")
        raise HTTPException(status_code=500, detail=f"Training failed: {e}")


@router.post("/train-async")
async def train_model_async(
    background_tasks: BackgroundTasks,
    use_tuning: bool = Query(default=False)
):
    """
    Start training in the background. Check /model-info for status.
    """
    def train_task():
        try:
            if use_tuning:
                tuning_result = tuner.tune()
                best_params = tuning_result.get("best_params", {})
                if best_params:
                    settings.svd_n_factors = best_params.get("n_factors", settings.svd_n_factors)
                    settings.svd_n_epochs = best_params.get("n_epochs", settings.svd_n_epochs)
                    settings.svd_lr_all = best_params.get("lr_all", settings.svd_lr_all)
                    settings.svd_reg_all = best_params.get("reg_all", settings.svd_reg_all)
            recommender.train()
        except Exception as e:
            logger.exception("Background training failed: %s", e)

    background_tasks.add_task(train_task)
    return {"message": "Training started in background. Check /model-info for status."}


@router.get("/model-info")
def model_info():
    """Return metadata about the currently loaded model and available versions."""
    info = get_model_info()

    if recommender.model is not None:
        info["current_model"] = {
            "status": "loaded",
            "meta": recommender.meta
        }
    else:
        info["current_model"] = {"status": "not_loaded"}

    return info


@router.get("/versions", response_model=list[ModelVersionResponse])
def list_model_versions():
    """List all available model versions."""
    versions = list_versions()
    return [
        ModelVersionResponse(
            version=v["version"],
            timestamp=v["timestamp"],
            size_mb=v["size_mb"],
            meta=v.get("meta", {})
        )
        for v in versions
    ]


@router.post("/rollback/{version}", response_model=RollbackResponse)
def rollback_model(version: str):
    """Rollback to a specific model version."""
    success = rollback_to_version(version)

    if success:
        # Reload the model
        recommender.load()
        return RollbackResponse(
            message=f"Successfully rolled back to version {version}",
            success=True
        )
    else:
        return RollbackResponse(
            message=f"Failed to rollback to version {version}",
            success=False
        )


# ══════════════════════════════════════════════════════════════════════════════
# Recommendation Endpoints
# ══════════════════════════════════════════════════════════════════════════════

@router.get("/recommendations/{user_id}", response_model=list[RecommendationItem])
def get_recommendations(
    user_id: str,
    limit: int = Query(default=20, ge=1, le=100),
    category_id: Optional[str] = Query(default=None),
):
    """
    Get top-N product recommendations for a user via SVD++ model.

    Pure ML-based recommendations without hybrid scoring.
    Use /hybrid/{user_id} for hybrid recommendations with fallbacks.
    """
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


@router.get("/hybrid/{user_id}", response_model=list[HybridRecommendationItem])
def get_hybrid_recommendations(
    user_id: str,
    limit: int = Query(default=20, ge=1, le=100),
    category_id: Optional[str] = Query(default=None),
):
    """
    Get hybrid recommendations combining SVD++ with popularity.

    Automatically handles:
        - Cold-start users (falls back to trending)
        - Missing model (falls back to popularity + latest)
        - Score diversification

    This is the RECOMMENDED endpoint for production use.
    """
    try:
        results = hybrid_recommender.recommend(
            user_id=user_id,
            n=limit,
            category_id=category_id,
            use_hybrid=True
        )

        return [
            HybridRecommendationItem(
                product_id=r["product_id"],
                product_name=r.get("product_name"),
                product_image=r.get("product_image"),
                price=r.get("price"),
                final_price=r.get("final_price"),
                average_rating=r.get("average_rating"),
                review_count=r.get("review_count"),
                score=r.get("score", 0),
                rank=r.get("rank", 0),
                recommendation_reason=r.get("recommendation_reason", "HYBRID"),
                explanation=r.get("explanation", ""),
                svd_score=r.get("svd_score"),
                popularity_score=r.get("popularity_score")
            )
            for r in results
        ]
    except Exception as e:
        logger.exception("Hybrid recommendation failed")
        raise HTTPException(status_code=500, detail=f"Recommendation failed: {e}")


@router.get("/similar/{product_id}", response_model=list[SimilarProductItem])
def get_similar_products(
    product_id: str,
    limit: int = Query(default=10, ge=1, le=50),
    category_id: Optional[str] = Query(default=None),
):
    """
    Get products similar to the given product.

    Uses SVD++ latent factors for ML-based similarity.
    Falls back to category-based similarity if model not available.
    """
    try:
        results = hybrid_recommender.get_similar_products(
            product_id=product_id,
            n=limit,
            category_id=category_id
        )

        return [
            SimilarProductItem(
                product_id=r["product_id"],
                product_name=r.get("product_name"),
                product_image=r.get("product_image"),
                price=r.get("price"),
                final_price=r.get("final_price"),
                similarity=r.get("similarity", 0),
                rank=r.get("rank", 0),
                recommendation_reason=r.get("recommendation_reason", "SIMILAR")
            )
            for r in results
        ]
    except Exception as e:
        logger.exception("Similar products failed")
        raise HTTPException(status_code=500, detail=f"Similar products failed: {e}")


# ══════════════════════════════════════════════════════════════════════════════
# Trending / Popularity Endpoints
# ══════════════════════════════════════════════════════════════════════════════

@router.get("/trending", response_model=list[TrendingProductItem])
def get_trending_products(
    limit: int = Query(default=20, ge=1, le=100),
    category_id: Optional[str] = Query(default=None),
):
    """
    Get trending/popular products based on recent interaction patterns.

    Uses time-decayed popularity scoring.
    """
    try:
        results = popularity_service.get_trending(
            n=limit,
            category_id=category_id
        )

        products_map = load_products_map()

        return [
            TrendingProductItem(
                product_id=r["product_id"],
                product_name=products_map.get(r["product_id"], {}).get("name"),
                product_image=(products_map.get(r["product_id"], {}).get("images") or [None])[0],
                price=products_map.get(r["product_id"], {}).get("price"),
                final_price=products_map.get(r["product_id"], {}).get("finalPrice"),
                popularity_score=r.get("popularity_score", 0),
                rank=r.get("rank", 0),
                recommendation_reason=r.get("recommendation_reason", "TRENDING")
            )
            for r in results
        ]
    except Exception as e:
        logger.exception("Trending products failed")
        raise HTTPException(status_code=500, detail=f"Trending products failed: {e}")


@router.get("/latest", response_model=list[TrendingProductItem])
def get_latest_products(
    limit: int = Query(default=20, ge=1, le=100),
    category_id: Optional[str] = Query(default=None),
):
    """Get latest published products."""
    try:
        results = popularity_service.get_latest_products(
            n=limit,
            category_id=category_id
        )

        products_map = load_products_map()

        return [
            TrendingProductItem(
                product_id=r["product_id"],
                product_name=products_map.get(r["product_id"], {}).get("name"),
                product_image=(products_map.get(r["product_id"], {}).get("images") or [None])[0],
                price=products_map.get(r["product_id"], {}).get("price"),
                final_price=products_map.get(r["product_id"], {}).get("finalPrice"),
                popularity_score=0,
                rank=r.get("rank", 0),
                recommendation_reason="LATEST"
            )
            for r in results
        ]
    except Exception as e:
        logger.exception("Latest products failed")
        raise HTTPException(status_code=500, detail=f"Latest products failed: {e}")


@router.get("/popularity-stats", response_model=PopularityStatsResponse)
def get_popularity_stats():
    """Get statistics about the popularity cache."""
    stats = popularity_service.get_stats()
    return PopularityStatsResponse(**stats)


@router.post("/popularity-refresh")
def refresh_popularity():
    """Force refresh the popularity cache."""
    popularity_service.compute_popularity(force_refresh=True)
    return {"message": "Popularity cache refreshed."}


# ══════════════════════════════════════════════════════════════════════════════
# Hyperparameter Tuning Endpoints
# ══════════════════════════════════════════════════════════════════════════════

@router.post("/tune", response_model=TuningResponse)
def run_hyperparameter_tuning(
    n_factors: Optional[str] = Query(default=None, description="Comma-separated n_factors values"),
    n_epochs: Optional[str] = Query(default=None, description="Comma-separated n_epochs values"),
    lr_all: Optional[str] = Query(default=None, description="Comma-separated learning rates"),
    reg_all: Optional[str] = Query(default=None, description="Comma-separated regularization values"),
    cv_folds: int = Query(default=3, ge=2, le=10),
):
    """
    Run hyperparameter tuning via grid search.

    Pass comma-separated values for each parameter to customize the search.
    Leave empty to use defaults from config.
    """
    try:
        # Parse parameter ranges
        nf = [int(x.strip()) for x in n_factors.split(",")] if n_factors else None
        ne = [int(x.strip()) for x in n_epochs.split(",")] if n_epochs else None
        lr = [float(x.strip()) for x in lr_all.split(",")] if lr_all else None
        reg = [float(x.strip()) for x in reg_all.split(",")] if reg_all else None

        result = tuner.tune(
            n_factors=nf,
            n_epochs=ne,
            lr_all=lr,
            reg_all=reg,
            cv_folds=cv_folds
        )

        return TuningResponse(
            message="Tuning completed successfully.",
            best_params=result.get("best_params"),
            best_rmse=result.get("best_rmse", 0),
            all_results=result.get("all_results", [])
        )
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))
    except Exception as e:
        logger.exception("Tuning failed")
        raise HTTPException(status_code=500, detail=f"Tuning failed: {e}")


# ══════════════════════════════════════════════════════════════════════════════
# Evaluation Endpoints
# ══════════════════════════════════════════════════════════════════════════════

@router.get("/metrics", response_model=EvaluationResponse)
def get_evaluation_metrics(
    k: int = Query(default=10, ge=1, le=50),
    n_folds: int = Query(default=3, ge=2, le=10),
):
    """
    Evaluate the model with Precision@K, Recall@K, NDCG@K, RMSE, MAE.

    Uses cross-validation for robust metrics.
    """
    try:
        metrics = evaluate_model(k=k, n_folds=n_folds)
        return EvaluationResponse(message="Evaluation completed.", metrics=metrics)
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))
    except Exception as e:
        logger.exception("Evaluation failed")
        raise HTTPException(status_code=500, detail=f"Evaluation failed: {e}")


# ══════════════════════════════════════════════════════════════════════════════
# Mock Data Generation
# ══════════════════════════════════════════════════════════════════════════════

@router.post("/mock-data", response_model=MockDataResponse)
def generate_mock(
    n_users: int = Query(default=200, ge=10),
    n_products: int = Query(default=0, ge=0, description="0 = use real products from MongoDB"),
    density: float = Query(default=0.08, ge=0.01, le=0.5),
    seed: int = Query(default=42),
    clear: bool = Query(default=False, description="Clear existing interactions first"),
):
    """
    Generate mock user interaction data in MongoDB.

    Useful for testing and development.
    Set n_products=0 to use real products from the database.
    """
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


# ══════════════════════════════════════════════════════════════════════════════
# Data & Stats Endpoints
# ══════════════════════════════════════════════════════════════════════════════

@router.get("/stats")
def get_data_stats():
    """Get statistics about the interaction data."""
    try:
        df = load_interactions_df()

        if df.empty:
            return {
                "total_interactions": 0,
                "unique_users": 0,
                "unique_products": 0,
                "density": 0,
                "avg_score": 0,
                "score_distribution": {}
            }

        return {
            "total_interactions": len(df),
            "unique_users": df["user_id"].nunique(),
            "unique_products": df["product_id"].nunique(),
            "density": round(len(df) / (df["user_id"].nunique() * df["product_id"].nunique()), 4),
            "avg_score": round(df["rating"].mean(), 2),
            "min_score": round(df["rating"].min(), 2),
            "max_score": round(df["rating"].max(), 2),
            "score_percentiles": {
                "25%": round(df["rating"].quantile(0.25), 2),
                "50%": round(df["rating"].quantile(0.50), 2),
                "75%": round(df["rating"].quantile(0.75), 2),
                "95%": round(df["rating"].quantile(0.95), 2),
            }
        }
    except Exception as e:
        logger.exception("Stats failed")
        raise HTTPException(status_code=500, detail=f"Failed to get stats: {e}")


@router.get("/user-stats/{user_id}")
def get_user_stats(user_id: str):
    """Get statistics and recommendations info for a specific user."""
    try:
        df = load_interactions_df()

        user_df = df[df["user_id"] == user_id]
        is_cold_start = hybrid_recommender.is_cold_start_user(user_id)

        return {
            "user_id": user_id,
            "is_cold_start": is_cold_start,
            "interaction_count": len(user_df),
            "unique_products_interacted": user_df["product_id"].nunique() if not user_df.empty else 0,
            "total_score": round(user_df["rating"].sum(), 2) if not user_df.empty else 0,
            "avg_score": round(user_df["rating"].mean(), 2) if not user_df.empty else 0,
            "recommendation_strategy": "COLD_START_FALLBACK" if is_cold_start else "ML_HYBRID"
        }
    except Exception as e:
        logger.exception("User stats failed")
        raise HTTPException(status_code=500, detail=f"Failed to get user stats: {e}")
