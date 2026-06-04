"""
CLIP Model Manager
------------------
Load và cache CLIP model khi khởi động để tránh cold start latency.
Sử dụng sentence-transformers với model clip-ViT-B-32.
"""
from __future__ import annotations

import io
from typing import Optional

import numpy as np
import requests
from loguru import logger
from PIL import Image
from sentence_transformers import SentenceTransformer


class CLIPModelManager:
    """Singleton manager cho CLIP model."""

    _instance: Optional[CLIPModelManager] = None
    _model: Optional[SentenceTransformer] = None

    def __new__(cls) -> CLIPModelManager:
        if cls._instance is None:
            cls._instance = super().__new__(cls)
        return cls._instance

    def load(self, model_name: str = "clip-ViT-B-32") -> None:
        """
        Load CLIP model vào bộ nhớ.
        Gọi một lần duy nhất khi startup (trong lifespan của FastAPI).
        """
        if self._model is not None:
            logger.info("CLIP model đã được load sẵn, bỏ qua.")
            return

        logger.info(f"Đang load CLIP model: {model_name} ...")
        try:
            self._model = SentenceTransformer(model_name)
            # Warm up model với ảnh giả để tránh cold start lần đầu
            dummy = Image.new("RGB", (224, 224), color=(128, 128, 128))
            self._model.encode(dummy)
            logger.info(f"CLIP model '{model_name}' loaded thành công.")
        except Exception as e:
            logger.error(f"Không thể load CLIP model '{model_name}': {e}")
            raise

    @property
    def model(self) -> SentenceTransformer:
        if self._model is None:
            raise RuntimeError(
                "CLIP model chưa được load. "
                "Gọi CLIPModelManager().load() trong startup lifespan."
            )
        return self._model

    def encode_image(self, image: Image.Image) -> np.ndarray:
        """Encode PIL Image thành vector embedding 512 chiều."""
        try:
            embedding = self.model.encode(image, convert_to_numpy=True)
            # Normalize L2 để cosine similarity = dot product
            norm = np.linalg.norm(embedding)
            if norm > 0:
                embedding = embedding / norm
            return embedding.astype(np.float32)
        except Exception as e:
            logger.error(f"Lỗi khi encode ảnh: {e}")
            raise

    def encode_image_from_url(self, url: str, timeout: int = 10) -> np.ndarray:
        """Tải ảnh từ URL (hoặc decode từ base64) và encode thành embedding."""
        try:
            if url.startswith('data:image/'):
                import base64
                # Lấy phần data sau dấu phẩy
                b64_data = url.split(',', 1)[1]
                image_data = base64.b64decode(b64_data)
                image = Image.open(io.BytesIO(image_data)).convert("RGB")
                return self.encode_image(image)

            headers = {
                'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36'
            }
            response = requests.get(url, headers=headers, timeout=timeout)
            response.raise_for_status()
            image = Image.open(io.BytesIO(response.content)).convert("RGB")
            return self.encode_image(image)
        except requests.RequestException as e:
            logger.error(f"Lỗi tải ảnh từ URL {url[:50]}...: {e}")
            raise
        except Exception as e:
            logger.error(f"Lỗi xử lý ảnh từ URL {url[:50]}...: {e}")
            raise


# Singleton instance
clip_manager = CLIPModelManager()
