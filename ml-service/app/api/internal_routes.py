"""Internal automation routes for model retraining and vector store refresh."""

from __future__ import annotations

import asyncio
import logging
from datetime import datetime, timezone
from typing import Any, Optional

from fastapi import APIRouter, BackgroundTasks, Header, HTTPException, status
from pydantic import BaseModel

from app.api import chatbot_routes
from app.config import settings
from app.models.svd_model import recommender

logger = logging.getLogger(__name__)
router = APIRouter(prefix="/api/v1/internal", tags=["internal"])


class TrainAllTriggerResponse(BaseModel):
    success: bool
    message: str
    queued: bool
    latest_rmse: Optional[float] = None
    last_result: Optional[dict[str, Any]] = None


class TrainAllStatusResponse(BaseModel):
    running: bool
    started_at: Optional[str] = None
    finished_at: Optional[str] = None
    last_result: Optional[dict[str, Any]] = None


train_all_state: dict[str, Any] = {
    "running": False,
    "started_at": None,
    "finished_at": None,
    "last_result": None,
}


def _get_bearer_token(authorization: str = Header(default="")) -> str:
    if not settings.internal_train_token:
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail="INTERNAL_TRAIN_TOKEN is not configured",
        )

    if not authorization.startswith("Bearer "):
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Missing Bearer token",
        )

    token = authorization.removeprefix("Bearer ").strip()
    if token != settings.internal_train_token:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Invalid internal training token",
        )

    return token


async def _run_train_all_job() -> None:
    started_at = datetime.now(timezone.utc).isoformat()
    train_all_state.update(
        {
            "running": True,
            "started_at": started_at,
            "finished_at": None,
            "last_result": None,
        }
    )

    logger.info("Internal train-all job started at %s", started_at)

    try:
        logger.info("Starting SVD++ retraining...")
        meta = await asyncio.to_thread(recommender.train)
        rmse = meta.get("rmse")
        logger.info("SVD++ retraining completed with RMSE=%s", rmse)

        chatbot_agent = chatbot_routes.chatbot_agent
        if chatbot_agent is None:
            raise RuntimeError("Chatbot agent is not initialized")

        logger.info("Refreshing chatbot vector store index...")
        indexed_count = await chatbot_agent.index_products()
        logger.info("Vector store refresh completed. Indexed %s products.", indexed_count)

        finished_at = datetime.now(timezone.utc).isoformat()
        result = {
            "success": True,
            "message": "SVD++ retrained and vector store refreshed successfully.",
            "rmse": rmse,
            "indexed_count": indexed_count,
            "trained_at": meta.get("trained_at"),
            "finished_at": finished_at,
        }
        train_all_state.update(
            {
                "running": False,
                "finished_at": finished_at,
                "last_result": result,
            }
        )
        logger.info("Internal train-all job finished successfully: %s", result)

    except Exception as exc:
        finished_at = datetime.now(timezone.utc).isoformat()
        result = {
            "success": False,
            "message": "Internal train-all job failed.",
            "error": str(exc),
            "finished_at": finished_at,
        }
        train_all_state.update(
            {
                "running": False,
                "finished_at": finished_at,
                "last_result": result,
            }
        )
        logger.exception("Internal train-all job failed")


@router.post("/train-all", response_model=TrainAllTriggerResponse, status_code=status.HTTP_202_ACCEPTED)
async def train_all(
    background_tasks: BackgroundTasks,
    _: str = Header(default="", alias="Authorization"),
) -> TrainAllTriggerResponse:
    _get_bearer_token(_)

    background_tasks.add_task(_run_train_all_job)

    last_result = train_all_state.get("last_result")
    latest_rmse = None
    if isinstance(last_result, dict):
        latest_rmse = last_result.get("rmse")

    return TrainAllTriggerResponse(
        success=True,
        message="Internal training job queued.",
        queued=True,
        latest_rmse=latest_rmse,
        last_result=last_result,
    )


@router.get("/train-all/status", response_model=TrainAllStatusResponse)
def train_all_status() -> TrainAllStatusResponse:
    return TrainAllStatusResponse(
        running=bool(train_all_state.get("running")),
        started_at=train_all_state.get("started_at"),
        finished_at=train_all_state.get("finished_at"),
        last_result=train_all_state.get("last_result"),
    )