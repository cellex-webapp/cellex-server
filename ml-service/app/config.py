"""
Application configuration — reads from environment / .env file.
"""

from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    # Database
    mongo_uri: str = "mongodb://admin:admin123@localhost:27017/cellex?authSource=admin"
    mongo_db: str = "cellex"

    # Service
    port: int = 8000
    model_dir: str = "./trained_models"
    retrain_on_startup: bool = False

    # SVD++ hyperparameters (tunable)
    svd_n_factors: int = 50
    svd_n_epochs: int = 30
    svd_lr_all: float = 0.005
    svd_reg_all: float = 0.02

    # Hyperparameter tuning config
    enable_auto_tuning: bool = False
    tuning_cv_folds: int = 3
    tuning_n_factors_range: str = "20,50,100"
    tuning_n_epochs_range: str = "20,30,50"
    tuning_lr_range: str = "0.002,0.005,0.01"
    tuning_reg_range: str = "0.01,0.02,0.05"

    # Hybrid recommender config
    hybrid_ml_weight: float = 0.7
    hybrid_popularity_weight: float = 0.2
    hybrid_recency_weight: float = 0.1

    # Popularity/Trending config
    trending_window_days: int = 30
    trending_decay_factor: float = 0.95
    min_interactions_for_trending: int = 5

    # Fallback config
    fallback_to_popularity: bool = True
    fallback_to_latest: bool = True
    cold_start_min_interactions: int = 3

    # Evaluation
    eval_k_values: str = "5,10,20"
    eval_n_folds: int = 3

    # Model versioning
    max_model_versions: int = 5

    class Config:
        env_file = ".env"
        env_file_encoding = "utf-8"

    def get_tuning_n_factors(self) -> list[int]:
        return [int(x.strip()) for x in self.tuning_n_factors_range.split(",")]

    def get_tuning_n_epochs(self) -> list[int]:
        return [int(x.strip()) for x in self.tuning_n_epochs_range.split(",")]

    def get_tuning_lr(self) -> list[float]:
        return [float(x.strip()) for x in self.tuning_lr_range.split(",")]

    def get_tuning_reg(self) -> list[float]:
        return [float(x.strip()) for x in self.tuning_reg_range.split(",")]

    def get_eval_k_values(self) -> list[int]:
        return [int(x.strip()) for x in self.eval_k_values.split(",")]


settings = Settings()
