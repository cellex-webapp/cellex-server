"""
Cellex ML Recommendation Service
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
FastAPI microservice that trains and serves SVD++ recommendations.
Connects to the same MongoDB instance as the Spring Boot backend.
"""

import logging
from contextlib import asynccontextmanager

from fastapi import FastAPI

from app.config import settings
from app.api.routes import router
from app.models.svd_model import recommender

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s: %(message)s",
)
logger = logging.getLogger(__name__)


@asynccontextmanager
async def lifespan(app: FastAPI):
    """Startup / shutdown events."""
    logger.info("ML Service starting ...")
    loaded = recommender.load()
    if loaded:
        logger.info("Pre-trained model loaded successfully.")
    else:
        logger.info("No pre-trained model found. Call POST /api/v1/ml/train to train one.")

    if settings.retrain_on_startup and not loaded:
        logger.info("RETRAIN_ON_STARTUP=true — training now ...")
        try:
            recommender.train()
        except Exception as e:
            logger.warning("Auto-train failed (will need manual /train): %s", e)

    yield
    logger.info("ML Service shutting down.")


app = FastAPI(
    title="Cellex ML Recommendation Service",
    description="SVD++ recommendation engine for the Cellex e-commerce platform",
    version="1.0.0",
    lifespan=lifespan,
)

app.include_router(router, prefix="/api/v1/ml")


@app.get("/health")
def health_check():
    return {
        "status": "ok",
        "model_loaded": recommender.model is not None,
    }
