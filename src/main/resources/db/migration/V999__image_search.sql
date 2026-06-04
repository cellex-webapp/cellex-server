-- Kích hoạt pgvector extension
CREATE EXTENSION IF NOT EXISTS vector;

-- Bảng lưu embeddings ảnh sản phẩm
CREATE TABLE IF NOT EXISTS product_image_embeddings (
    id BIGSERIAL PRIMARY KEY,
    product_id VARCHAR(50) NOT NULL,
    image_url TEXT NOT NULL,
    embedding vector(512) NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Thêm UNIQUE constraint theo lưu ý quan trọng số 2
ALTER TABLE product_image_embeddings
ADD CONSTRAINT uq_pie_product_image UNIQUE (product_id, image_url);

-- Index cosine similarity (IVFFlat cho tập dữ liệu vừa)
CREATE INDEX IF NOT EXISTS idx_pie_embedding
    ON product_image_embeddings
    USING ivfflat (embedding vector_cosine_ops)
    WITH (lists = 100);

-- Index tìm theo product_id (để xóa khi product bị delete)
CREATE INDEX IF NOT EXISTS idx_pie_product_id
    ON product_image_embeddings (product_id);
