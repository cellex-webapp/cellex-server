"""
Application configuration — reads from environment / .env file.
"""

from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    # Database - MongoDB
    mongo_uri: str = "mongodb://admin:admin123@localhost:27017/cellex?authSource=admin"
    mongo_db: str = "cellex"

    # Internal automation
    internal_train_token: str = ""

    # Database - PostgreSQL (Supabase)
    # Format: postgresql+psycopg2://user:pass@host:port/dbname
    # Converted from Spring Boot jdbc:postgresql:// format automatically
    postgres_url: str = ""
    postgres_pool_size: int = 5
    postgres_max_overflow: int = 10

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

    # ML Heads
    ml_heads_model_dir: str = "./ml_heads_models"

    # Chatbot - LLM Configuration (Gemini)
    gemini_api_key: str = ""
    gemini_model: str = "gemini-2.5-flash-lite"
    gemini_temperature: float = 0.8
    gemini_max_tokens: int = 16384
    gemini_top_k: int = 40
    gemini_top_p: float = 0.95

    # Chatbot - RAG Configuration
    embedding_model: str = "sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2"
    vector_store_path: str = "./vector_store"
    chunk_size: int = 500
    chunk_overlap: int = 50
    top_k_retrieval: int = 5

    # Chatbot - Agent Configuration
    max_tool_iterations: int = 5
    enable_streaming: bool = True
    conversation_memory_size: int = 10

    # Chatbot - Security & Guardrails
    enable_rbac: bool = True
    enable_pii_masking: bool = True
    max_response_length: int = 2000
    rate_limit_per_minute: int = 60

    class Config:
        env_file = ".env"
        env_file_encoding = "utf-8"
        extra = "ignore"  # Ignore unknown env vars

    def get_postgres_sqlalchemy_url(self) -> str:
        """
        Convert JDBC-style URL or plain URL to SQLAlchemy-compatible URL.
        Handles:
          jdbc:postgresql://host:port/db  -> postgresql+psycopg2://user:pass@host:port/db
          postgresql://...               -> postgresql+psycopg2://...
          postgresql+psycopg2://...      -> unchanged
        """
        if not self.postgres_url:
            return ""
        url = self.postgres_url
        # Strip JDBC prefix
        if url.startswith("jdbc:"):
            url = url[5:]
        # Ensure psycopg2 driver
        if url.startswith("postgresql://"):
            url = "postgresql+psycopg2://" + url[len("postgresql://"):]
        return url

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
