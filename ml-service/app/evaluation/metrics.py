"""
metrics.py
~~~~~~~~~~
Offline evaluation metrics for the SVD++ recommendation model.

Implements:
    * Precision@K
    * Recall@K
    * NDCG@K (Normalized Discounted Cumulative Gain)
    * MAP@K (Mean Average Precision)
    * RMSE / MAE (Rating prediction accuracy)

Uses train-test split strategy for evaluation.
"""

import logging
import math
from typing import List, Tuple, Set
from collections import defaultdict

import numpy as np

from app.config import settings
from app.data.data_loader import load_interactions_df
from app.models.svdpp import SVDpp, SVDppConfig

logger = logging.getLogger(__name__)

DEFAULT_K = 10


def train_test_split(
    user_ids: List[str],
    item_ids: List[str],
    ratings: List[float],
    test_ratio: float = 0.2,
    random_state: int = 42
) -> Tuple[List, List, List, List, List, List]:
    """
    Split data into train and test sets.

    For each user, randomly hold out test_ratio of their interactions.

    Returns:
        train_users, train_items, train_ratings, test_users, test_items, test_ratings
    """
    np.random.seed(random_state)

    # Group by user
    user_interactions = defaultdict(list)
    for u, i, r in zip(user_ids, item_ids, ratings):
        user_interactions[u].append((i, r))

    train_users, train_items, train_ratings = [], [], []
    test_users, test_items, test_ratings = [], [], []

    for user, interactions in user_interactions.items():
        if len(interactions) <= 1:
            # Keep single interactions in train
            for item, rating in interactions:
                train_users.append(user)
                train_items.append(item)
                train_ratings.append(rating)
        else:
            # Split interactions
            n_test = max(1, int(len(interactions) * test_ratio))
            indices = np.random.permutation(len(interactions))

            for idx in indices[:n_test]:
                item, rating = interactions[idx]
                test_users.append(user)
                test_items.append(item)
                test_ratings.append(rating)

            for idx in indices[n_test:]:
                item, rating = interactions[idx]
                train_users.append(user)
                train_items.append(item)
                train_ratings.append(rating)

    return (train_users, train_items, train_ratings,
            test_users, test_items, test_ratings)


def _compute_precision_at_k(
    model: SVDpp,
    test_users: List[str],
    test_items: List[str],
    k: int = DEFAULT_K
) -> float:
    """
    Compute Precision@K.

    Precision = (relevant items in top-K) / K
    """
    # Group test items by user
    user_test_items = defaultdict(set)
    for u, i in zip(test_users, test_items):
        user_test_items[u].add(i)

    precisions = []

    for user, relevant_items in user_test_items.items():
        if user not in model.user_to_idx:
            continue

        # Get top-K recommendations
        recommendations = model.recommend(user, n=k)
        rec_items = {item_id for item_id, _ in recommendations}

        # Compute precision
        hits = len(rec_items & relevant_items)
        precisions.append(hits / k)

    return np.mean(precisions) if precisions else 0.0


def _compute_recall_at_k(
    model: SVDpp,
    test_users: List[str],
    test_items: List[str],
    k: int = DEFAULT_K
) -> float:
    """
    Compute Recall@K.

    Recall = (relevant items in top-K) / (total relevant items)
    """
    user_test_items = defaultdict(set)
    for u, i in zip(test_users, test_items):
        user_test_items[u].add(i)

    recalls = []

    for user, relevant_items in user_test_items.items():
        if user not in model.user_to_idx:
            continue

        recommendations = model.recommend(user, n=k)
        rec_items = {item_id for item_id, _ in recommendations}

        hits = len(rec_items & relevant_items)
        recalls.append(hits / len(relevant_items))

    return np.mean(recalls) if recalls else 0.0


def _compute_ndcg_at_k(
    model: SVDpp,
    test_users: List[str],
    test_items: List[str],
    test_ratings: List[float],
    k: int = DEFAULT_K
) -> float:
    """
    Compute NDCG@K.

    NDCG measures ranking quality with position discounting.
    Uses actual ratings as relevance scores.
    """
    # Group test data by user
    user_test_data = defaultdict(list)
    for u, i, r in zip(test_users, test_items, test_ratings):
        user_test_data[u].append((i, r))

    ndcgs = []

    for user, items_ratings in user_test_data.items():
        if user not in model.user_to_idx:
            continue

        test_items_set = {item for item, _ in items_ratings}
        item_to_rating = {item: rating for item, rating in items_ratings}

        # Get recommendations
        recommendations = model.recommend(user, n=k)

        # DCG: relevance of recommended items at their positions
        dcg = 0.0
        for i, (item_id, _) in enumerate(recommendations):
            if item_id in test_items_set:
                rel = item_to_rating[item_id]
                dcg += rel / math.log2(i + 2)  # i+2 because log2(1)=0

        # IDCG: ideal ranking (sort by actual ratings)
        sorted_ratings = sorted([r for _, r in items_ratings], reverse=True)[:k]
        idcg = sum(rel / math.log2(i + 2) for i, rel in enumerate(sorted_ratings))

        if idcg > 0:
            ndcgs.append(dcg / idcg)

    return np.mean(ndcgs) if ndcgs else 0.0


def _compute_map_at_k(
    model: SVDpp,
    test_users: List[str],
    test_items: List[str],
    k: int = DEFAULT_K
) -> float:
    """
    Compute Mean Average Precision @K.

    AP = (1/min(k, R)) * Σ P(i) * rel(i)
    where R is the number of relevant items
    """
    user_test_items = defaultdict(set)
    for u, i in zip(test_users, test_items):
        user_test_items[u].add(i)

    aps = []

    for user, relevant_items in user_test_items.items():
        if user not in model.user_to_idx:
            continue

        recommendations = model.recommend(user, n=k)

        hits = 0
        sum_precisions = 0.0

        for i, (item_id, _) in enumerate(recommendations):
            if item_id in relevant_items:
                hits += 1
                sum_precisions += hits / (i + 1)

        if hits > 0:
            aps.append(sum_precisions / min(k, len(relevant_items)))
        else:
            aps.append(0.0)

    return np.mean(aps) if aps else 0.0


def _compute_rmse_mae(
    model: SVDpp,
    test_users: List[str],
    test_items: List[str],
    test_ratings: List[float]
) -> Tuple[float, float]:
    """
    Compute RMSE and MAE for rating prediction.
    """
    errors = []

    for u, i, r in zip(test_users, test_items, test_ratings):
        if u not in model.user_to_idx or i not in model.item_to_idx:
            continue

        pred = model.predict(u, i)
        errors.append(r - pred)

    if not errors:
        return 0.0, 0.0

    errors = np.array(errors)
    rmse = np.sqrt(np.mean(errors ** 2))
    mae = np.mean(np.abs(errors))

    return float(rmse), float(mae)


def evaluate_model(k: int = DEFAULT_K, n_folds: int = 3) -> dict:
    """
    Evaluate the SVD++ model with multiple ranking metrics.

    Uses cross-validation (multiple train-test splits).

    Returns a summary dict with:
        - precision_at_k
        - recall_at_k
        - ndcg_at_k
        - map_at_k
        - rmse, mae
        - data statistics
    """
    logger.info("Starting SVD++ model evaluation (k=%d, folds=%d) ...", k, n_folds)

    df = load_interactions_df()
    if df.empty:
        raise ValueError("No data for evaluation.")

    user_ids = df["user_id"].tolist()
    item_ids = df["product_id"].tolist()
    ratings = df["rating"].tolist()

    # Determine rating scale
    min_rating = float(df["rating"].min())
    max_rating = float(df["rating"].max())
    if min_rating == max_rating:
        max_rating = min_rating + 1.0

    all_precisions = []
    all_recalls = []
    all_ndcgs = []
    all_maps = []
    all_rmses = []
    all_maes = []

    for fold in range(n_folds):
        logger.info("Evaluating fold %d/%d...", fold + 1, n_folds)

        # Split data
        (train_users, train_items, train_ratings,
         test_users, test_items, test_ratings) = train_test_split(
            user_ids, item_ids, ratings,
            test_ratio=0.2,
            random_state=42 + fold
        )

        # Train model
        config = SVDppConfig(
            n_factors=settings.svd_n_factors,
            n_epochs=settings.svd_n_epochs,
            lr=settings.svd_lr_all,
            reg=settings.svd_reg_all,
            min_rating=min_rating,
            max_rating=max_rating,
            verbose=False
        )

        model = SVDpp(config)
        model.fit(train_users, train_items, train_ratings)

        # Compute metrics
        precision = _compute_precision_at_k(model, test_users, test_items, k)
        recall = _compute_recall_at_k(model, test_users, test_items, k)
        ndcg = _compute_ndcg_at_k(model, test_users, test_items, test_ratings, k)
        map_score = _compute_map_at_k(model, test_users, test_items, k)
        rmse, mae = _compute_rmse_mae(model, test_users, test_items, test_ratings)

        all_precisions.append(precision)
        all_recalls.append(recall)
        all_ndcgs.append(ndcg)
        all_maps.append(map_score)
        all_rmses.append(rmse)
        all_maes.append(mae)

        logger.info(
            "  Fold %d: P@%d=%.4f, R@%d=%.4f, NDCG@%d=%.4f, MAP@%d=%.4f, RMSE=%.4f, MAE=%.4f",
            fold + 1, k, precision, k, recall, k, ndcg, k, map_score, rmse, mae
        )

    metrics = {
        "precision_at_k": round(float(np.mean(all_precisions)), 4),
        "recall_at_k": round(float(np.mean(all_recalls)), 4),
        "ndcg_at_k": round(float(np.mean(all_ndcgs)), 4),
        "map_at_k": round(float(np.mean(all_maps)), 4),
        "rmse": round(float(np.mean(all_rmses)), 4),
        "mae": round(float(np.mean(all_maes)), 4),
        "precision_std": round(float(np.std(all_precisions)), 4),
        "recall_std": round(float(np.std(all_recalls)), 4),
        "k": k,
        "n_folds": n_folds,
        "n_interactions": len(df),
        "n_users": df["user_id"].nunique(),
        "n_products": df["product_id"].nunique(),
        "algorithm": "SVD++"
    }

    logger.info("Evaluation complete: %s", metrics)
    return metrics
