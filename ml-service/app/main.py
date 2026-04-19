"""
Cellex ML Service
~~~~~~~~~~~~~~~~~
FastAPI microservice for:
- SVD++ recommendations
- LLM + RAG + Tool-calling chatbot

Connects to the same MongoDB instance as the Spring Boot backend.
"""

import logging
from contextlib import asynccontextmanager

from fastapi import FastAPI
from pymongo import MongoClient

from app.config import settings
from app.api.routes import router
from app.api.chatbot_routes import router as chatbot_router
from app.models.svd_model import recommender

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s: %(message)s",
)
logger = logging.getLogger(__name__)

# Global instances
mongo_client = None
chatbot_agent_instance = None


@asynccontextmanager
async def lifespan(app: FastAPI):
    """Startup / shutdown events."""
    global mongo_client, chatbot_agent_instance

    logger.info("ML Service starting ...")

    # 1. Load SVD++ model
    loaded = recommender.load()
    if loaded:
        logger.info("Pre-trained SVD++ model loaded successfully.")
    else:
        logger.info("No pre-trained model found. Call POST /api/v1/ml/train to train one.")

    if settings.retrain_on_startup and not loaded:
        logger.info("RETRAIN_ON_STARTUP=true — training now ...")
        try:
            recommender.train()
        except Exception as e:
            logger.warning("Auto-train failed (will need manual /train): %s", e)

    # 2. Initialize Chatbot Agent
    try:
        logger.info("Initializing chatbot agent...")
        mongo_client = MongoClient(settings.mongo_uri)
        mongo_client.admin.command("ping")  # Test connection

        # Import here to avoid circular import
        from app.chatbot.agent import ChatbotAgent
        from app.api import chatbot_routes

        chatbot_agent_instance = ChatbotAgent(mongo_client)
        chatbot_routes.chatbot_agent = chatbot_agent_instance

        logger.info("Chatbot agent initialized successfully")
        logger.info("To index products, call: POST /api/v1/chatbot/index-products")

    except Exception as e:
        logger.error(f"Failed to initialize chatbot: {e}")
        logger.warning("Chatbot will not be available. Check OPENAI_API_KEY and MONGO_URI")

    yield

    # Cleanup
    logger.info("ML Service shutting down.")
    if mongo_client:
        mongo_client.close()


app = FastAPI(
    title="Cellex ML Service",
    description="SVD++ recommendation + LLM chatbot for Cellex e-commerce platform",
    version="2.0.0",
    lifespan=lifespan,
)

# Include routers
app.include_router(router, prefix="/api/v1/ml")
app.include_router(chatbot_router)


@app.get("/health")
def health_check():
    """Tong the health check."""
    return {
        "status": "ok",
        "recommender_model_loaded": recommender.model is not None,
        "chatbot_agent_initialized": chatbot_agent_instance is not None,
    }
