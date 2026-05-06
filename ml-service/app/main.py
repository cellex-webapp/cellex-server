"""
Cellex ML Service
~~~~~~~~~~~~~~~~~
FastAPI microservice for:
- SVD++ recommendations
- LLM + RAG + Tool-calling chatbot
- ML Heads: Demand Forecast, Stockout Risk, Churn Risk, Coupon Uplift

Connects to MongoDB (same instance as Spring Boot) and PostgreSQL (Supabase).
"""

import logging
from contextlib import asynccontextmanager

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from pymongo import MongoClient

from app.config import settings
from app.api.routes import router as ml_router
from app.api.chatbot_routes import router as chatbot_router
from app.api.internal_routes import router as internal_router
from app.api.ml_heads_routes import router as ml_heads_router
from app.models.svd_model import recommender

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s: %(message)s",
)
logger = logging.getLogger(__name__)

# ── Global instances ──────────────────────────────────────────────────────
mongo_client = None
chatbot_agent_instance = None


@asynccontextmanager
async def lifespan(app: FastAPI):
    """Startup / shutdown events."""
    global mongo_client, chatbot_agent_instance

    logger.info("ML Service starting ...")

    # ── 1. MongoDB ────────────────────────────────────────────────────────
    try:
        mongo_client = MongoClient(settings.mongo_uri, serverSelectionTimeoutMS=5000)
        mongo_client.admin.command("ping")
        logger.info(f"MongoDB connected: {settings.mongo_db}")
    except Exception as e:
        logger.error(f"MongoDB connection failed: {e}")
        logger.warning("Some features will not work without MongoDB")
        mongo_client = None

    # ── 2. PostgreSQL (Supabase) ──────────────────────────────────────────
    from app.database.postgres import get_postgres_client
    pg_client = get_postgres_client()
    if pg_client.is_connected:
        logger.info("PostgreSQL (Supabase) connected — ML Heads will use real order data")
    else:
        logger.warning(
            "PostgreSQL not connected. ML Heads will use MongoDB-only mode. "
            "Set POSTGRES_URL in .env to enable full ML Heads features."
        )

    # ── 3. SVD++ Model ────────────────────────────────────────────────────
    loaded = recommender.load()
    if loaded:
        logger.info("Pre-trained SVD++ model loaded successfully.")
    else:
        logger.info("No pre-trained SVD++ model found. Call POST /api/v1/ml/train to train one.")

    if settings.retrain_on_startup and not loaded:
        logger.info("RETRAIN_ON_STARTUP=true — training now ...")
        try:
            recommender.train()
        except Exception as e:
            logger.warning("Auto-train failed (will need manual /train): %s", e)

    # ── 4. ML Heads Services ──────────────────────────────────────────────
    if mongo_client is not None:
        try:
            from app.ml_heads import (
                DemandForecastService,
                StockoutRiskService,
                ChurnRiskService,
                CouponUpliftService,
            )
            from app.api import ml_heads_routes

            ml_heads_routes.demand_service = DemandForecastService(
                pg_client, mongo_client, settings.mongo_db
            )
            ml_heads_routes.stockout_service = StockoutRiskService(
                pg_client, mongo_client, settings.mongo_db
            )
            ml_heads_routes.churn_service = ChurnRiskService(
                pg_client, mongo_client, settings.mongo_db
            )
            ml_heads_routes.coupon_service = CouponUpliftService(
                pg_client, mongo_client, settings.mongo_db
            )
            logger.info("ML Heads services initialized successfully")
        except Exception as e:
            logger.error(f"Failed to initialize ML Heads: {e}")
    else:
        logger.warning("ML Heads skipped — MongoDB not connected")

    # ── 5. Chatbot Agent ──────────────────────────────────────────────────
    if mongo_client is not None:
        try:
            logger.info("Initializing chatbot agent...")
            from app.chatbot.agent import ChatbotAgent
            from app.api import chatbot_routes

            chatbot_agent_instance = ChatbotAgent(mongo_client)
            chatbot_routes.chatbot_agent = chatbot_agent_instance

            logger.info("Chatbot agent initialized successfully")
            logger.info("To index products, call: POST /api/v1/chatbot/index-products")

        except Exception as e:
            logger.error(f"Failed to initialize chatbot: {e}")
            logger.warning(
                "Chatbot will not be available. "
                "Check GEMINI_API_KEY and MONGO_URI in .env"
            )
    else:
        logger.warning("Chatbot skipped — MongoDB not connected")

    yield

    # ── Cleanup ───────────────────────────────────────────────────────────
    logger.info("ML Service shutting down.")
    if mongo_client:
        mongo_client.close()


app = FastAPI(
    title="Cellex ML Service",
    description=(
        "AI/ML microservice for Cellex e-commerce platform.\n\n"
        "**Modules:**\n"
        "- `/api/v1/ml/*` — SVD++ recommendations\n"
        "- `/api/v1/chatbot/*` — LLM chatbot + KPI endpoints\n"
        "- `/api/v1/ml-heads/*` — Demand Forecast, Stockout Risk, Churn Risk, Coupon Uplift\n\n"
        "**Data Sources:**\n"
        "- MongoDB: products, user_interactions, reviews\n"
        "- PostgreSQL (Supabase): orders, order_items, users, shops, user_coupons"
    ),
    version="3.0.0",
    lifespan=lifespan,
)

# ── CORS ──────────────────────────────────────────────────────────────────
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # In production: specify exact origins
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# ── Routers ───────────────────────────────────────────────────────────────
app.include_router(ml_router, prefix="/api/v1/ml")
app.include_router(chatbot_router)
app.include_router(internal_router)
app.include_router(ml_heads_router)


@app.get("/health")
def health_check():
    """System-level health check."""
    from app.database.postgres import get_postgres_client
    pg = get_postgres_client()
    return {
        "status": "ok",
        "version": "3.0.0",
        "components": {
            "mongodb": mongo_client is not None,
            "postgresql": pg.is_connected,
            "svd_recommender": recommender.model is not None,
            "chatbot": chatbot_agent_instance is not None,
            "ml_heads": {
                "demand_forecast": True,
                "stockout_risk": True,
                "churn_risk": True,
                "coupon_uplift": True,
            },
        },
    }

from fastapi.responses import RedirectResponse

@app.get("/")
def read_root():
    """Redirect root to swagger docs."""
    return RedirectResponse(url="/docs")

@app.get("/swagger-ui/index.html", include_in_schema=False)
def swagger_ui_redirect():
    """Redirect Spring Boot style swagger URL to FastAPI docs."""
    return RedirectResponse(url="/docs")

@app.get("/favicon.ico", include_in_schema=False)
def favicon():
    """Return empty response for favicon."""
    return {}
