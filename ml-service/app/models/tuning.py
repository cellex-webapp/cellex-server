"""
tuning.py
~~~~~~~~~
Hyperparameter tuning for SVD++ recommendation model.

Supports grid search with cross-validation to find optimal parameters.
"""

import logging
import time
from collections import defaultdict
from itertools import product
from typing import Optional, List, Tuple

import numpy as np
import pandas as pd

from app.config import settings
from app.data.data_loader import load_interactions_df
from app.models.svdpp import SVDpp, SVDppConfig

logger = logging.getLogger(__name__)


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
    """
    np.random.seed(random_state)

    user_interactions = defaultdict(list)
    for u, i, r in zip(user_ids, item_ids, ratings):
        user_interactions[u].append((i, r))

    train_users, train_items, train_ratings = [], [], []
    test_users, test_items, test_ratings = [], [], []

    for user, interactions in user_interactions.items():
        if len(interactions) <= 1:
            for item, rating in interactions:
                train_users.append(user)
                train_items.append(item)
                train_ratings.append(rating)
        else:
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


def _compute_rmse(
    model: SVDpp,
    test_users: List[str],
    test_items: List[str],
    test_ratings: List[float]
) -> float:
    """Compute RMSE for rating prediction."""
    errors = []
    for u, i, r in zip(test_users, test_items, test_ratings):
        if u in model.user_to_idx and i in model.item_to_idx:
            pred = model.predict(u, i)
            errors.append((r - pred) ** 2)

    return np.sqrt(np.mean(errors)) if errors else float('inf')


def _compute_precision_at_k(
    model: SVDpp,
    test_users: List[str],
    test_items: List[str],
    k: int = 10
) -> float:
    """Compute Precision@K."""
    user_test_items = defaultdict(set)
    for u, i in zip(test_users, test_items):
        user_test_items[u].add(i)

    precisions = []
    for user, relevant_items in user_test_items.items():
        if user not in model.user_to_idx:
            continue

        recommendations = model.recommend(user, n=k)
        rec_items = {item_id for item_id, _ in recommendations}
        hits = len(rec_items & relevant_items)
        precisions.append(hits / k)

    return np.mean(precisions) if precisions else 0.0


class HyperparameterTuner:
    """
    Grid search hyperparameter tuning for SVD++.
    """

    def __init__(self):
        self.best_params: Optional[dict] = None
        self.best_score: float = float('inf')
        self.tuning_results: list[dict] = []

    def tune(
        self,
        n_factors: Optional[list[int]] = None,
        n_epochs: Optional[list[int]] = None,
        lr_all: Optional[list[float]] = None,
        reg_all: Optional[list[float]] = None,
        cv_folds: int = 3,
        optimize_metric: str = "rmse"
    ) -> dict:
        """
        Run grid search to find optimal hyperparameters.

        Args:
            n_factors: List of n_factors values to try
            n_epochs: List of n_epochs values to try
            lr_all: List of learning rates to try
            reg_all: List of regularization values to try
            cv_folds: Number of cross-validation folds
            optimize_metric: Metric to optimize ('rmse' or 'precision')

        Returns:
            Dict with best parameters and all results
        """
        # Use config defaults if not specified
        if n_factors is None:
            n_factors = settings.get_tuning_n_factors()
        if n_epochs is None:
            n_epochs = settings.get_tuning_n_epochs()
        if lr_all is None:
            lr_all = settings.get_tuning_lr()
        if reg_all is None:
            reg_all = settings.get_tuning_reg()

        logger.info("Starting SVD++ hyperparameter tuning...")
        logger.info("  n_factors: %s", n_factors)
        logger.info("  n_epochs: %s", n_epochs)
        logger.info("  lr_all: %s", lr_all)
        logger.info("  reg_all: %s", reg_all)
        logger.info("  cv_folds: %d", cv_folds)

        # Load data
        df = load_interactions_df()
        if df.empty:
            raise ValueError("No interaction data available for tuning.")

        user_ids = df["user_id"].tolist()
        item_ids = df["product_id"].tolist()
        ratings = df["rating"].tolist()

        min_rating = float(df["rating"].min())
        max_rating = float(df["rating"].max())
        if min_rating == max_rating:
            max_rating = min_rating + 1.0

        # Grid search
        total_combinations = len(n_factors) * len(n_epochs) * len(lr_all) * len(reg_all)
        logger.info("Total combinations to try: %d", total_combinations)

        self.tuning_results = []
        self.best_score = float('inf') if optimize_metric == "rmse" else 0.0
        self.best_params = None

        t0 = time.time()
        combination_idx = 0

        for nf, ne, lr, reg in product(n_factors, n_epochs, lr_all, reg_all):
            combination_idx += 1
            logger.info(
                "Testing %d/%d: n_factors=%d, n_epochs=%d, lr=%.4f, reg=%.4f",
                combination_idx, total_combinations, nf, ne, lr, reg
            )

            try:
                fold_rmses = []
                fold_precisions = []

                for fold in range(cv_folds):
                    # Split data
                    (train_users, train_items, train_ratings,
                     test_users, test_items, test_ratings) = train_test_split(
                        user_ids, item_ids, ratings,
                        test_ratio=0.2,
                        random_state=42 + fold
                    )

                    # Train model
                    config = SVDppConfig(
                        n_factors=nf,
                        n_epochs=ne,
                        lr=lr,
                        reg=reg,
                        min_rating=min_rating,
                        max_rating=max_rating,
                        verbose=False
                    )

                    model = SVDpp(config)
                    model.fit(train_users, train_items, train_ratings)

                    # Evaluate
                    rmse = _compute_rmse(model, test_users, test_items, test_ratings)
                    precision = _compute_precision_at_k(model, test_users, test_items, k=10)

                    fold_rmses.append(rmse)
                    fold_precisions.append(precision)

                mean_rmse = np.mean(fold_rmses)
                mean_precision = np.mean(fold_precisions)
                std_rmse = np.std(fold_rmses)

                result = {
                    "n_factors": nf,
                    "n_epochs": ne,
                    "lr_all": lr,
                    "reg_all": reg,
                    "rmse": round(mean_rmse, 4),
                    "precision_at_10": round(mean_precision, 4),
                    "rmse_std": round(std_rmse, 4)
                }
                self.tuning_results.append(result)

                # Check if best
                if optimize_metric == "rmse":
                    is_better = mean_rmse < self.best_score
                    metric_value = mean_rmse
                else:
                    is_better = mean_precision > self.best_score
                    metric_value = mean_precision

                if is_better:
                    self.best_score = metric_value
                    self.best_params = {
                        "n_factors": nf,
                        "n_epochs": ne,
                        "lr_all": lr,
                        "reg_all": reg
                    }
                    logger.info("  New best! %s=%.4f", optimize_metric, metric_value)
                else:
                    logger.info("  %s=%.4f", optimize_metric, metric_value)

            except Exception as e:
                logger.warning("  Failed: %s", e)
                self.tuning_results.append({
                    "n_factors": nf,
                    "n_epochs": ne,
                    "lr_all": lr,
                    "reg_all": reg,
                    "error": str(e)
                })

        elapsed = time.time() - t0

        summary = {
            "best_params": self.best_params,
            "best_score": round(self.best_score, 4),
            "optimize_metric": optimize_metric,
            "total_combinations": total_combinations,
            "successful_combinations": len([r for r in self.tuning_results if "error" not in r]),
            "elapsed_seconds": round(elapsed, 2),
            "all_results": self.tuning_results
        }

        logger.info("Tuning complete in %.1fs", elapsed)
        logger.info("Best params: %s", self.best_params)
        logger.info("Best score: %.4f", self.best_score)

        return summary

    def get_best_params(self) -> Optional[dict]:
        """Return the best parameters found during tuning."""
        return self.best_params

    def get_results_df(self) -> pd.DataFrame:
        """Return tuning results as a DataFrame for analysis."""
        return pd.DataFrame(self.tuning_results)


# Module-level singleton
tuner = HyperparameterTuner()
