"""
Application configuration — reads from environment / .env file.
"""

from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    mongo_uri: str = "mongodb://admin:admin123@localhost:27017/cellex?authSource=admin"
    mongo_db: str = "cellex"
    port: int = 8000
    model_dir: str = "./trained_models"
    retrain_on_startup: bool = False

    # SVD++ hyper-parameters (tunable)
    svd_n_factors: int = 50
    svd_n_epochs: int = 30
    svd_lr_all: float = 0.005
    svd_reg_all: float = 0.02

    class Config:
        env_file = ".env"
        env_file_encoding = "utf-8"


settings = Settings()
