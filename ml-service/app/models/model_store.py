"""
model_store.py
~~~~~~~~~~~~~~
Persistence helpers — save / load trained Surprise models to disk via joblib.

Supports model versioning:
    - Keeps up to N versions of the model
    - Each version is timestamped
    - Can rollback to previous versions
"""

import logging
import os
import shutil
from datetime import datetime
from pathlib import Path
from typing import Optional

import joblib

from app.config import settings

logger = logging.getLogger(__name__)

MODEL_FILE = "svdpp_model.joblib"
META_FILE = "svdpp_meta.joblib"
VERSIONS_DIR = "versions"


def _model_dir() -> Path:
    p = Path(settings.model_dir)
    p.mkdir(parents=True, exist_ok=True)
    return p


def _versions_dir() -> Path:
    d = _model_dir() / VERSIONS_DIR
    d.mkdir(parents=True, exist_ok=True)
    return d


def save_model(algo, meta: dict) -> str:
    """
    Persist a trained Surprise algorithm + metadata.

    Also creates a versioned backup.
    Returns the file path.
    """
    d = _model_dir()
    model_path = d / MODEL_FILE
    meta_path = d / META_FILE

    # Version the current model before overwriting
    if model_path.exists():
        _archive_current_model()

    joblib.dump(algo, model_path)
    joblib.dump(meta, meta_path)

    # Cleanup old versions
    _cleanup_old_versions()

    logger.info("Model saved to %s", model_path)
    return str(model_path)


def _archive_current_model():
    """Archive the current model to versions directory."""
    d = _model_dir()
    model_path = d / MODEL_FILE
    meta_path = d / META_FILE

    if not model_path.exists():
        return

    timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
    version_dir = _versions_dir() / timestamp
    version_dir.mkdir(parents=True, exist_ok=True)

    shutil.copy2(model_path, version_dir / MODEL_FILE)
    if meta_path.exists():
        shutil.copy2(meta_path, version_dir / META_FILE)

    logger.info("Archived model to version %s", timestamp)


def _cleanup_old_versions():
    """Remove old versions beyond max_model_versions."""
    versions_dir = _versions_dir()
    versions = sorted(versions_dir.iterdir(), reverse=True)

    if len(versions) > settings.max_model_versions:
        for old_version in versions[settings.max_model_versions:]:
            if old_version.is_dir():
                shutil.rmtree(old_version)
                logger.info("Removed old model version: %s", old_version.name)


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


def list_versions() -> list[dict]:
    """
    List all available model versions.

    Returns:
        List of version info dicts with timestamp, size, and metadata
    """
    versions_dir = _versions_dir()
    versions = []

    for version_path in sorted(versions_dir.iterdir(), reverse=True):
        if not version_path.is_dir():
            continue

        model_path = version_path / MODEL_FILE
        meta_path = version_path / META_FILE

        if not model_path.exists():
            continue

        # Load metadata if available
        meta = {}
        if meta_path.exists():
            try:
                meta = joblib.load(meta_path)
            except Exception:
                pass

        # Get file size
        size_mb = model_path.stat().st_size / (1024 * 1024)

        versions.append({
            "version": version_path.name,
            "timestamp": version_path.name,
            "size_mb": round(size_mb, 2),
            "meta": meta
        })

    # Also include current model
    d = _model_dir()
    model_path = d / MODEL_FILE
    meta_path = d / META_FILE

    if model_path.exists():
        meta = {}
        if meta_path.exists():
            try:
                meta = joblib.load(meta_path)
            except Exception:
                pass

        size_mb = model_path.stat().st_size / (1024 * 1024)

        versions.insert(0, {
            "version": "current",
            "timestamp": meta.get("trained_at", "unknown"),
            "size_mb": round(size_mb, 2),
            "meta": meta
        })

    return versions


def rollback_to_version(version: str) -> bool:
    """
    Rollback to a specific model version.

    Args:
        version: Version timestamp (e.g., "20240101_120000")

    Returns:
        True if rollback successful, False otherwise
    """
    if version == "current":
        logger.warning("Cannot rollback to current version")
        return False

    version_dir = _versions_dir() / version
    if not version_dir.exists():
        logger.error("Version not found: %s", version)
        return False

    model_path = version_dir / MODEL_FILE
    meta_path = version_dir / META_FILE

    if not model_path.exists():
        logger.error("Model file not found in version: %s", version)
        return False

    # Archive current before rollback
    _archive_current_model()

    # Copy version to current
    d = _model_dir()
    shutil.copy2(model_path, d / MODEL_FILE)
    if meta_path.exists():
        shutil.copy2(meta_path, d / META_FILE)

    logger.info("Rolled back to version %s", version)
    return True


def delete_version(version: str) -> bool:
    """
    Delete a specific model version.

    Args:
        version: Version timestamp

    Returns:
        True if deleted successfully
    """
    if version == "current":
        logger.warning("Cannot delete current version")
        return False

    version_dir = _versions_dir() / version
    if not version_dir.exists():
        logger.error("Version not found: %s", version)
        return False

    shutil.rmtree(version_dir)
    logger.info("Deleted version %s", version)
    return True


def get_model_info() -> dict:
    """Get information about the current model."""
    d = _model_dir()
    model_path = d / MODEL_FILE
    meta_path = d / META_FILE

    if not model_path.exists():
        return {
            "status": "no_model",
            "versions_available": len(list_versions())
        }

    meta = {}
    if meta_path.exists():
        try:
            meta = joblib.load(meta_path)
        except Exception:
            pass

    size_mb = model_path.stat().st_size / (1024 * 1024)

    return {
        "status": "loaded",
        "size_mb": round(size_mb, 2),
        "meta": meta,
        "versions_available": len(list_versions())
    }
