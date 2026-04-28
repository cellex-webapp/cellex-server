import os
import sys
import logging

# Thêm thư mục hiện tại vào sys.path để import được app
sys.path.append(os.path.dirname(os.path.abspath(__file__)))

from app.models.svd_model import recommender
from app.config import settings

logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(name)s - %(levelname)s - %(message)s')
logger = logging.getLogger("ManualTrain")

def run_training():
    """
    Script để train model bằng tay và lưu vào thư mục trained_models.
    Sử dụng: python train_model.py
    """
    logger.info(f"Bat dau train model SVD++ ...")
    logger.info(f"Model se duoc luu vao: {os.path.abspath(settings.model_dir)}")
    
    try:
        # Kích hoạt training
        meta = recommender.train()
        
        logger.info("=========================================")
        logger.info("TRAIN THANH CONG!")
        logger.info(f"Thoi gian train: {meta['training_seconds']}s")
        logger.info(f"So luong tuong tac: {meta['n_interactions']}")
        logger.info(f"File model: {os.path.join(settings.model_dir, 'svdpp_model.joblib')}")
        logger.info("=========================================")
        
    except Exception as e:
        logger.error(f"Loi trong qua trinh train: {e}")
        sys.exit(1)

if __name__ == "__main__":
    run_training()
