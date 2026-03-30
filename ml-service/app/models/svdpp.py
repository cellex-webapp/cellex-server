"""
svdpp.py
~~~~~~~~
Pure Python/NumPy implementation of SVD++ algorithm for recommendation.

SVD++ (Koren, 2008) extends SVD by incorporating implicit feedback:
    r̂_ui = μ + b_u + b_i + q_i^T(p_u + |N(u)|^{-0.5} * Σ y_j)

where:
    - μ: global mean rating
    - b_u, b_i: user and item biases
    - p_u: user latent factor vector
    - q_i: item latent factor vector
    - y_j: implicit feedback vectors for items user has interacted with
    - N(u): set of items user has rated/interacted with

Optimized using Stochastic Gradient Descent (SGD).
"""

import logging
import time
from dataclasses import dataclass
from typing import Optional, Tuple, List, Dict, Set

import numpy as np

logger = logging.getLogger(__name__)


@dataclass
class SVDppConfig:
    """Configuration for SVD++ model."""
    n_factors: int = 50
    n_epochs: int = 30
    lr: float = 0.005
    reg: float = 0.02
    lr_bias: float = 0.005
    reg_bias: float = 0.02
    min_rating: float = 1.0
    max_rating: float = 5.0
    verbose: bool = True


class SVDpp:
    """
    SVD++ implementation with implicit feedback.

    This implementation follows Koren's SVD++ paper:
    "Factorization Meets the Neighborhood: a Multifaceted Collaborative Filtering Model"
    """

    def __init__(self, config: Optional[SVDppConfig] = None):
        self.config = config or SVDppConfig()

        # Global mean
        self.global_mean: float = 0.0

        # Biases
        self.bu: Optional[np.ndarray] = None  # User biases
        self.bi: Optional[np.ndarray] = None  # Item biases

        # Latent factors
        self.pu: Optional[np.ndarray] = None  # User factors (n_users x n_factors)
        self.qi: Optional[np.ndarray] = None  # Item factors (n_items x n_factors)
        self.yj: Optional[np.ndarray] = None  # Implicit factors (n_items x n_factors)

        # Mappings
        self.user_to_idx: Dict[str, int] = {}
        self.idx_to_user: Dict[int, str] = {}
        self.item_to_idx: Dict[str, int] = {}
        self.idx_to_item: Dict[int, str] = {}

        # User's implicit feedback (items they've interacted with)
        self.user_items: Dict[int, Set[int]] = {}

        # Training info
        self.n_users: int = 0
        self.n_items: int = 0
        self.is_fitted: bool = False

    def fit(
        self,
        user_ids: List[str],
        item_ids: List[str],
        ratings: List[float]
    ) -> "SVDpp":
        """
        Train the SVD++ model.

        Args:
            user_ids: List of user identifiers
            item_ids: List of item identifiers
            ratings: List of rating values

        Returns:
            self
        """
        if len(user_ids) != len(item_ids) or len(user_ids) != len(ratings):
            raise ValueError("user_ids, item_ids, and ratings must have same length")

        if len(user_ids) == 0:
            raise ValueError("No training data provided")

        logger.info("Starting SVD++ training with %d interactions", len(user_ids))
        t0 = time.time()

        # Build mappings
        unique_users = sorted(set(user_ids))
        unique_items = sorted(set(item_ids))

        self.user_to_idx = {u: i for i, u in enumerate(unique_users)}
        self.idx_to_user = {i: u for u, i in self.user_to_idx.items()}
        self.item_to_idx = {it: i for i, it in enumerate(unique_items)}
        self.idx_to_item = {i: it for it, i in self.item_to_idx.items()}

        self.n_users = len(unique_users)
        self.n_items = len(unique_items)

        # Convert to numpy arrays with indices
        u_indices = np.array([self.user_to_idx[u] for u in user_ids], dtype=np.int32)
        i_indices = np.array([self.item_to_idx[i] for i in item_ids], dtype=np.int32)
        r_values = np.array(ratings, dtype=np.float32)

        # Compute global mean
        self.global_mean = float(np.mean(r_values))

        # Build user-items sets (implicit feedback)
        self.user_items = {u: set() for u in range(self.n_users)}
        for u, i in zip(u_indices, i_indices):
            self.user_items[u].add(i)

        # Initialize parameters
        self._init_parameters()

        # Training data as list of tuples for shuffling
        train_data = list(zip(u_indices, i_indices, r_values))

        # SGD training
        for epoch in range(self.config.n_epochs):
            np.random.shuffle(train_data)
            epoch_loss = self._train_epoch(train_data)

            if self.config.verbose and (epoch + 1) % 5 == 0:
                rmse = np.sqrt(epoch_loss / len(train_data))
                logger.info("Epoch %d/%d - RMSE: %.4f",
                           epoch + 1, self.config.n_epochs, rmse)

        self.is_fitted = True
        elapsed = time.time() - t0
        logger.info("SVD++ training completed in %.2fs", elapsed)

        return self

    def _init_parameters(self):
        """Initialize model parameters with small random values."""
        scale = 0.1 / np.sqrt(self.config.n_factors)

        self.bu = np.zeros(self.n_users, dtype=np.float32)
        self.bi = np.zeros(self.n_items, dtype=np.float32)

        self.pu = np.random.normal(0, scale, (self.n_users, self.config.n_factors)).astype(np.float32)
        self.qi = np.random.normal(0, scale, (self.n_items, self.config.n_factors)).astype(np.float32)
        self.yj = np.random.normal(0, scale, (self.n_items, self.config.n_factors)).astype(np.float32)

    def _train_epoch(self, train_data: List[Tuple[int, int, float]]) -> float:
        """
        Train one epoch using SGD.

        Returns:
            Total squared error for the epoch
        """
        total_loss = 0.0
        lr = self.config.lr
        lr_bias = self.config.lr_bias
        reg = self.config.reg
        reg_bias = self.config.reg_bias

        for u, i, r in train_data:
            # Get user's implicit feedback items
            Nu = self.user_items[u]
            sqrt_Nu = 1.0 / np.sqrt(len(Nu)) if len(Nu) > 0 else 0.0

            # Compute implicit feedback sum
            sum_yj = np.zeros(self.config.n_factors, dtype=np.float32)
            if len(Nu) > 0:
                sum_yj = np.sum(self.yj[list(Nu)], axis=0)

            # Compute prediction
            implicit_term = sqrt_Nu * sum_yj
            pred = (self.global_mean +
                   self.bu[u] +
                   self.bi[i] +
                   np.dot(self.qi[i], self.pu[u] + implicit_term))

            # Clip prediction to rating range
            pred = np.clip(pred, self.config.min_rating, self.config.max_rating)

            # Compute error
            err = r - pred
            total_loss += err ** 2

            # Update biases
            self.bu[u] += lr_bias * (err - reg_bias * self.bu[u])
            self.bi[i] += lr_bias * (err - reg_bias * self.bi[i])

            # Save old values for update
            old_pu = self.pu[u].copy()
            old_qi = self.qi[i].copy()

            # Update latent factors
            self.pu[u] += lr * (err * old_qi - reg * old_pu)
            self.qi[i] += lr * (err * (old_pu + implicit_term) - reg * old_qi)

            # Update implicit factors
            if len(Nu) > 0:
                yj_update = lr * (err * sqrt_Nu * old_qi)
                for j in Nu:
                    self.yj[j] += yj_update - lr * reg * self.yj[j]

        return total_loss

    def predict(self, user_id: str, item_id: str) -> float:
        """
        Predict rating for a user-item pair.

        Args:
            user_id: User identifier
            item_id: Item identifier

        Returns:
            Predicted rating
        """
        if not self.is_fitted:
            raise RuntimeError("Model not fitted. Call fit() first.")

        # Handle unknown users/items
        if user_id not in self.user_to_idx:
            return self.global_mean
        if item_id not in self.item_to_idx:
            return self.global_mean + self.bu[self.user_to_idx[user_id]]

        u = self.user_to_idx[user_id]
        i = self.item_to_idx[item_id]

        return self._predict_idx(u, i)

    def _predict_idx(self, u: int, i: int) -> float:
        """Predict rating using internal indices."""
        Nu = self.user_items.get(u, set())
        sqrt_Nu = 1.0 / np.sqrt(len(Nu)) if len(Nu) > 0 else 0.0

        sum_yj = np.zeros(self.config.n_factors, dtype=np.float32)
        if len(Nu) > 0:
            sum_yj = np.sum(self.yj[list(Nu)], axis=0)

        implicit_term = sqrt_Nu * sum_yj
        pred = (self.global_mean +
               self.bu[u] +
               self.bi[i] +
               np.dot(self.qi[i], self.pu[u] + implicit_term))

        return float(np.clip(pred, self.config.min_rating, self.config.max_rating))

    def recommend(
        self,
        user_id: str,
        n: int = 10,
        exclude_known: bool = True
    ) -> List[Tuple[str, float]]:
        """
        Generate top-N recommendations for a user.

        Args:
            user_id: User identifier
            n: Number of recommendations
            exclude_known: Whether to exclude items user has already interacted with

        Returns:
            List of (item_id, predicted_score) tuples
        """
        if not self.is_fitted:
            raise RuntimeError("Model not fitted. Call fit() first.")

        if user_id not in self.user_to_idx:
            return []

        u = self.user_to_idx[user_id]
        known_items = self.user_items.get(u, set()) if exclude_known else set()

        # Precompute user's implicit feedback term
        Nu = self.user_items.get(u, set())
        sqrt_Nu = 1.0 / np.sqrt(len(Nu)) if len(Nu) > 0 else 0.0
        sum_yj = np.zeros(self.config.n_factors, dtype=np.float32)
        if len(Nu) > 0:
            sum_yj = np.sum(self.yj[list(Nu)], axis=0)
        user_factor = self.pu[u] + sqrt_Nu * sum_yj

        # Score all items
        scores = (self.global_mean +
                 self.bu[u] +
                 self.bi +
                 np.dot(self.qi, user_factor))

        # Filter known items
        candidate_indices = [i for i in range(self.n_items) if i not in known_items]
        candidate_scores = [(i, scores[i]) for i in candidate_indices]

        # Sort by score descending
        candidate_scores.sort(key=lambda x: x[1], reverse=True)

        # Return top-N
        return [
            (self.idx_to_item[i], float(score))
            for i, score in candidate_scores[:n]
        ]

    def similar_items(
        self,
        item_id: str,
        n: int = 10
    ) -> List[Tuple[str, float]]:
        """
        Find similar items based on item latent factors.

        Uses cosine similarity on the item factor vectors (qi).

        Args:
            item_id: Item identifier
            n: Number of similar items to return

        Returns:
            List of (item_id, similarity) tuples
        """
        if not self.is_fitted:
            raise RuntimeError("Model not fitted. Call fit() first.")

        if item_id not in self.item_to_idx:
            return []

        i = self.item_to_idx[item_id]
        target_vec = self.qi[i]
        target_norm = np.linalg.norm(target_vec)

        if target_norm == 0:
            return []

        # Compute cosine similarities with all items
        norms = np.linalg.norm(self.qi, axis=1)
        norms[norms == 0] = 1e-10  # Avoid division by zero

        similarities = np.dot(self.qi, target_vec) / (norms * target_norm)

        # Get top-N (excluding the item itself)
        indices = np.argsort(similarities)[::-1]

        results = []
        for idx in indices:
            if idx != i and len(results) < n:
                results.append((self.idx_to_item[idx], float(similarities[idx])))

        return results

    def get_user_factors(self, user_id: str) -> Optional[np.ndarray]:
        """Get the latent factor vector for a user."""
        if not self.is_fitted or user_id not in self.user_to_idx:
            return None
        u = self.user_to_idx[user_id]

        # Include implicit feedback in user representation
        Nu = self.user_items.get(u, set())
        sqrt_Nu = 1.0 / np.sqrt(len(Nu)) if len(Nu) > 0 else 0.0
        sum_yj = np.zeros(self.config.n_factors, dtype=np.float32)
        if len(Nu) > 0:
            sum_yj = np.sum(self.yj[list(Nu)], axis=0)

        return self.pu[u] + sqrt_Nu * sum_yj

    def get_item_factors(self, item_id: str) -> Optional[np.ndarray]:
        """Get the latent factor vector for an item."""
        if not self.is_fitted or item_id not in self.item_to_idx:
            return None
        return self.qi[self.item_to_idx[item_id]]

    def get_params(self) -> dict:
        """Get model parameters for persistence."""
        return {
            "config": self.config,
            "global_mean": self.global_mean,
            "bu": self.bu,
            "bi": self.bi,
            "pu": self.pu,
            "qi": self.qi,
            "yj": self.yj,
            "user_to_idx": self.user_to_idx,
            "idx_to_user": self.idx_to_user,
            "item_to_idx": self.item_to_idx,
            "idx_to_item": self.idx_to_item,
            "user_items": self.user_items,
            "n_users": self.n_users,
            "n_items": self.n_items,
            "is_fitted": self.is_fitted
        }

    def set_params(self, params: dict) -> "SVDpp":
        """Load model parameters."""
        self.config = params["config"]
        self.global_mean = params["global_mean"]
        self.bu = params["bu"]
        self.bi = params["bi"]
        self.pu = params["pu"]
        self.qi = params["qi"]
        self.yj = params["yj"]
        self.user_to_idx = params["user_to_idx"]
        self.idx_to_user = params["idx_to_user"]
        self.item_to_idx = params["item_to_idx"]
        self.idx_to_item = params["idx_to_item"]
        self.user_items = params["user_items"]
        self.n_users = params["n_users"]
        self.n_items = params["n_items"]
        self.is_fitted = params["is_fitted"]
        return self
