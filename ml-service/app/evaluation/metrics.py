"""
metrics.py
~~~~~~~~~~
Offline evaluation metrics for the recommendation model.

Implements:
    * Precision@K
    * Recall@K
    * NDCG@K  (Normalized Discounted Cumulative Gain)

Uses a leave-one-out strategy:
    For each user, hold out the interaction with the highest score as the
    "ground truth" item, train/predict on the rest.
"""

import logging
import math
from collections import defaultdict

import pandas as pd
from surprise import SVDpp, Dataset, Reader, accuracy
from surprise.model_selection import cross_validate, KFold

from app.config import settings
from app.data.data_loader import load_interactions_df, load_surprise_dataset

logger = logging.getLogger(__name__)

DEFAULT_K = 10


def _precision_recall_at_k(
    predictions: list, k: int = DEFAULT_K, threshold: float = None
) -> dict:
    """
    Compute Precision@K and Recall@K from a list of Surprise prediction objects.

    A prediction is considered "relevant" if its true rating >= threshold.
    If threshold is None, uses the median rating as the threshold.
    """
    # Group predictions by user
    user_est: dict[str, list] = defaultdict(list)
    for pred in predictions:
        user_est[pred.uid].append((pred.iid, pred.est, pred.r_ui))

    if threshold is None:
        all_true = [p.r_ui for p in predictions if p.r_ui is not None]
        threshold = sorted(all_true)[len(all_true) // 2] if all_true else 3.0

    precisions = []
    recalls = []

    for uid, preds in user_est.items():
        # Sort by estimated score descending, take top-K
        preds.sort(key=lambda x: x[1], reverse=True)
        top_k = preds[:k]

        n_relevant = sum(1 for _, _, true_r in preds if true_r is not None and true_r >= threshold)
        n_relevant_in_k = sum(1 for _, _, true_r in top_k if true_r is not None and true_r >= threshold)

        precisions.append(n_relevant_in_k / k if k > 0 else 0)
        recalls.append(n_relevant_in_k / n_relevant if n_relevant > 0 else 0)

    avg_precision = sum(precisions) / len(precisions) if precisions else 0
    avg_recall = sum(recalls) / len(recalls) if recalls else 0

    return {
        "precision_at_k": round(avg_precision, 4),
        "recall_at_k": round(avg_recall, 4),
        "k": k,
        "threshold": round(threshold, 2),
    }


def _ndcg_at_k(predictions: list, k: int = DEFAULT_K) -> float:
    """
    Compute NDCG@K.

    Uses the true rating as the relevance score.
    """
    user_est: dict[str, list] = defaultdict(list)
    for pred in predictions:
        user_est[pred.uid].append((pred.iid, pred.est, pred.r_ui))

    ndcgs = []

    for uid, preds in user_est.items():
        # Predicted ranking
        preds.sort(key=lambda x: x[1], reverse=True)
        top_k = preds[:k]

        # DCG
        dcg = 0.0
        for i, (_, _, true_r) in enumerate(top_k):
            rel = true_r if true_r is not None and true_r > 0 else 0
            dcg += rel / math.log2(i + 2)  # i+2 because log2(1)=0

        # Ideal DCG (sort by true rating)
        preds.sort(key=lambda x: (x[2] if x[2] is not None else 0), reverse=True)
        ideal_k = preds[:k]
        idcg = 0.0
        for i, (_, _, true_r) in enumerate(ideal_k):
            rel = true_r if true_r is not None and true_r > 0 else 0
            idcg += rel / math.log2(i + 2)

        ndcgs.append(dcg / idcg if idcg > 0 else 0)

    return round(sum(ndcgs) / len(ndcgs), 4) if ndcgs else 0.0


def evaluate_model(k: int = DEFAULT_K, n_folds: int = 3) -> dict:
    """
    Run k-fold cross-validation and compute Precision@K, Recall@K, NDCG@K,
    as well as RMSE and MAE.

    Returns a summary dict.
    """
    logger.info("Starting model evaluation (k=%d, folds=%d) ...", k, n_folds)

    df = load_interactions_df()
    if df.empty:
        raise ValueError("No data for evaluation.")

    dataset = load_surprise_dataset(df)

    algo = SVDpp(
        n_factors=settings.svd_n_factors,
        n_epochs=settings.svd_n_epochs,
        lr_all=settings.svd_lr_all,
        reg_all=settings.svd_reg_all,
        verbose=False,
    )

    # Cross-validate for RMSE / MAE
    cv_results = cross_validate(algo, dataset, measures=["RMSE", "MAE"], cv=n_folds, verbose=False)

    # Collect predictions for ranking metrics
    kf = KFold(n_splits=n_folds)
    all_predictions = []

    for trainset, testset in kf.split(dataset):
        fold_algo = SVDpp(
            n_factors=settings.svd_n_factors,
            n_epochs=settings.svd_n_epochs,
            lr_all=settings.svd_lr_all,
            reg_all=settings.svd_reg_all,
            verbose=False,
        )
        fold_algo.fit(trainset)
        preds = fold_algo.test(testset)
        all_predictions.extend(preds)

    pr = _precision_recall_at_k(all_predictions, k=k)
    ndcg = _ndcg_at_k(all_predictions, k=k)

    metrics = {
        "rmse": round(float(cv_results["test_rmse"].mean()), 4),
        "mae": round(float(cv_results["test_mae"].mean()), 4),
        "precision_at_k": pr["precision_at_k"],
        "recall_at_k": pr["recall_at_k"],
        "ndcg_at_k": ndcg,
        "k": k,
        "n_folds": n_folds,
        "n_interactions": len(df),
        "n_users": df["user_id"].nunique(),
        "n_products": df["product_id"].nunique(),
    }

    logger.info("Evaluation complete: %s", metrics)
    return metrics
