"""
model_store.py
~~~~~~~~~~~~~~
Persistence helpers — save / load trained Surprise models to disk via joblib.
"""

import logging
import os
from pathlib import Path
from typing import Optional

import joblib

from app.config import settings

logger = logging.getLogger(__name__)

MODEL_FILE = "svdpp_model.joblib"
META_FILE = "svdpp_meta.joblib"


def _model_dir() -> Path:
    p = Path(settings.model_dir)
    p.mkdir(parents=True, exist_ok=True)
    return p


def save_model(algo, meta: dict) -> str:
    """Persist a trained Surprise algorithm + metadata. Returns the file path."""
    d = _model_dir()
    model_path = d / MODEL_FILE
    meta_path = d / META_FILE
    joblib.dump(algo, model_path)
    joblib.dump(meta, meta_path)
    logger.info("Model saved to %s", model_path)
    return str(model_path)


def load_model() -> tuple[Optional[object], Optional[dict]]:
    """Load a previously saved model. Returns (algo, meta) or (None, None)."""
    d = _model_dir()
    model_path = d / MODEL_FILE
    meta_path = d / META_FILE

    if not model_path.exists():
        return None, None

    algo = joblib.load(model_path)
    meta = joblib.load(meta_path) if meta_path.exists() else {}
    logger.info("Model loaded from %s", model_path)
    return algo, meta
