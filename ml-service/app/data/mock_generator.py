"""
mock_generator.py
~~~~~~~~~~~~~~~~~
Generate realistic mock user_interactions and insert into MongoDB.

Usage (standalone):
    python -m app.data.mock_generator          # uses defaults
    python -m app.data.mock_generator --users 300 --products 0 --density 0.08

When ``--products 0`` (default) the script reads the existing published
products from MongoDB instead of creating synthetic ones.

Design principles:
    * Power-law distribution for product popularity
      (few hot products, long tail of niche ones)
    * Users are grouped into preference clusters based on product categories
    * Interaction funnel: views >> carts > purchases > reviews
"""

import argparse
import logging
import math
import random
from datetime import datetime, timedelta
from typing import Optional

from pymongo import MongoClient

from app.config import settings

logger = logging.getLogger(__name__)

# ── Interaction funnel probabilities ─────────────────────────
# Given a "view" event happened, probability of next steps:
P_CART_GIVEN_VIEW = 0.30
P_PURCHASE_GIVEN_CART = 0.45
P_REVIEW_GIVEN_PURCHASE = 0.25

# Weights (same as Java UserInteraction.calculateTotalScore)
W_VIEW = 1.0
W_CART = 3.0
W_PURCHASE = 5.0
W_REVIEW = 4.0


def _get_db():
    client = MongoClient(settings.mongo_uri)
    return client[settings.mongo_db]


def _fetch_existing_products(db) -> list[dict]:
    """Read published products from MongoDB."""
    products = []
    for doc in db["products"].find({"isPublished": True}):
        products.append(
            {
                "product_id": str(doc["_id"]),
                "category_id": doc.get("categoryId", ""),
                "price": doc.get("finalPrice") or doc.get("price", 0),
            }
        )
    logger.info("Fetched %d published products from MongoDB.", len(products))
    return products


def _fetch_existing_user_ids(db) -> list[str]:
    """Read user IDs from the users collection (PostgreSQL-backed users are
    not in Mongo, so we read from user_interactions to find known IDs, or
    generate synthetic ones)."""
    existing = db["user_interactions"].distinct("user_id")
    return list(existing) if existing else []


def _power_law_weights(n: int, alpha: float = 1.2) -> list[float]:
    """Return *n* weights following a Zipf-like distribution."""
    weights = [1.0 / (i + 1) ** alpha for i in range(n)]
    total = sum(weights)
    return [w / total for w in weights]


def generate(
    n_users: int = 200,
    n_synthetic_products: int = 0,
    density: float = 0.08,
    seed: int = 42,
    clear_existing: bool = False,
) -> dict:
    """
    Generate mock interactions and write to MongoDB ``user_interactions``.

    Parameters
    ----------
    n_users : int
        Number of synthetic user IDs to create (prefixed ``mock_user_``).
    n_synthetic_products : int
        If 0, use real products from MongoDB.  Otherwise create fake IDs.
    density : float
        Fraction of the user-product matrix that will have interactions (0-1).
    seed : int
        Random seed for reproducibility.
    clear_existing : bool
        If True, drop the ``user_interactions`` collection first.

    Returns
    -------
    dict with summary statistics.
    """
    random.seed(seed)
    db = _get_db()

    # ── Products ───────────────────────────────────────────
    if n_synthetic_products > 0:
        products = [
            {"product_id": f"mock_product_{i:04d}", "category_id": f"cat_{i % 8}", "price": random.uniform(500_000, 50_000_000)}
            for i in range(n_synthetic_products)
        ]
    else:
        products = _fetch_existing_products(db)
        if not products:
            raise RuntimeError(
                "No published products in MongoDB. Pass --products N to generate synthetic ones."
            )

    # ── Users ──────────────────────────────────────────────
    existing_user_ids = _fetch_existing_user_ids(db)
    user_ids = existing_user_ids[:n_users] if len(existing_user_ids) >= n_users else existing_user_ids.copy()

    # Fill remaining with mock IDs
    remaining = n_users - len(user_ids)
    if remaining > 0:
        user_ids.extend([f"mock_user_{i:04d}" for i in range(remaining)])

    logger.info("Using %d users  |  %d products", len(user_ids), len(products))

    # ── Build category → product index ─────────────────────
    cat_products: dict[str, list[dict]] = {}
    for p in products:
        cat_products.setdefault(p["category_id"], []).append(p)

    categories = list(cat_products.keys())

    # ── Assign each user a preferred category cluster ──────
    user_preferred_cats: dict[str, list[str]] = {}
    for uid in user_ids:
        # Each user likes 1-3 categories
        k = random.randint(1, min(3, len(categories)))
        user_preferred_cats[uid] = random.sample(categories, k)

    # ── Popularity weights per product (power-law) ─────────
    pop_weights = _power_law_weights(len(products))

    # ── Generate interactions ──────────────────────────────
    interactions: list[dict] = []
    n_target = int(len(user_ids) * len(products) * density)

    for _ in range(n_target):
        uid = random.choice(user_ids)

        # 70 % chance to pick from preferred categories
        if random.random() < 0.70 and user_preferred_cats[uid]:
            cat = random.choice(user_preferred_cats[uid])
            pool = cat_products.get(cat, products)
            product = random.choice(pool)
        else:
            # Weighted random from all products (popular products more likely)
            product = random.choices(products, weights=pop_weights, k=1)[0]

        # ── Simulate funnel ────────────────────────────────
        view_count = random.randint(1, 8)
        cart_count = 0
        purchase_count = 0
        review_count = 0

        if random.random() < P_CART_GIVEN_VIEW:
            cart_count = random.randint(1, 3)
            if random.random() < P_PURCHASE_GIVEN_CART:
                purchase_count = random.randint(1, 2)
                if random.random() < P_REVIEW_GIVEN_PURCHASE:
                    review_count = 1

        total_score = (
            view_count * W_VIEW
            + cart_count * W_CART
            + purchase_count * W_PURCHASE
            + review_count * W_REVIEW
        )

        days_ago = random.randint(0, 90)
        ts = datetime.utcnow() - timedelta(days=days_ago)

        interactions.append(
            {
                "user_id": uid,
                "product_id": product["product_id"],
                "category_id": product["category_id"],
                "view_count": view_count,
                "cart_count": cart_count,
                "purchase_count": purchase_count,
                "review_count": review_count,
                "total_score": total_score,
                "created_at": ts,
                "updated_at": ts,
            }
        )

    # ── Deduplicate (same user+product → merge) ───────────
    merged: dict[tuple, dict] = {}
    for row in interactions:
        key = (row["user_id"], row["product_id"])
        if key in merged:
            existing = merged[key]
            existing["view_count"] += row["view_count"]
            existing["cart_count"] += row["cart_count"]
            existing["purchase_count"] += row["purchase_count"]
            existing["review_count"] += row["review_count"]
            existing["total_score"] = (
                existing["view_count"] * W_VIEW
                + existing["cart_count"] * W_CART
                + existing["purchase_count"] * W_PURCHASE
                + existing["review_count"] * W_REVIEW
            )
            existing["updated_at"] = max(existing["updated_at"], row["updated_at"])
        else:
            merged[key] = row

    final_interactions = list(merged.values())

    # ── Write to MongoDB ───────────────────────────────────
    col = db["user_interactions"]
    if clear_existing:
        col.delete_many({})
        logger.info("Cleared existing user_interactions.")

    if final_interactions:
        col.insert_many(final_interactions)

    summary = {
        "total_interactions": len(final_interactions),
        "unique_users": len(set(r["user_id"] for r in final_interactions)),
        "unique_products": len(set(r["product_id"] for r in final_interactions)),
        "density": len(final_interactions) / (len(user_ids) * len(products)) if products else 0,
        "clear_existing": clear_existing,
    }
    logger.info("Mock data generation complete: %s", summary)
    return summary


# ── CLI entrypoint ─────────────────────────────────────────
if __name__ == "__main__":
    logging.basicConfig(level=logging.INFO, format="%(asctime)s [%(levelname)s] %(message)s")

    parser = argparse.ArgumentParser(description="Generate mock user interactions")
    parser.add_argument("--users", type=int, default=200, help="Number of users")
    parser.add_argument("--products", type=int, default=0, help="0 = use real products from MongoDB")
    parser.add_argument("--density", type=float, default=0.08, help="Interaction density (0-1)")
    parser.add_argument("--seed", type=int, default=42, help="Random seed")
    parser.add_argument("--clear", action="store_true", help="Clear existing interactions first")
    args = parser.parse_args()

    generate(
        n_users=args.users,
        n_synthetic_products=args.products,
        density=args.density,
        seed=args.seed,
        clear_existing=args.clear,
    )
