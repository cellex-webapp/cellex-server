-- ============================================================
-- V8: Reviews Table
-- ============================================================

CREATE TABLE IF NOT EXISTS reviews (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id          VARCHAR(50),
    user_id             VARCHAR(50),
    user_name           VARCHAR(255),
    user_avatar         TEXT,
    order_id            VARCHAR(50),
    order_item_id       VARCHAR(50),
    shop_id             VARCHAR(50),
    rating              INTEGER,
    comment             TEXT,
    images              JSONB,
    videos              JSONB,
    vendor_response     JSONB,
    is_verified_purchase BOOLEAN DEFAULT TRUE,
    helpful_count       INTEGER DEFAULT 0,
    helpful_voted_user_ids JSONB,
    status              VARCHAR(50) DEFAULT 'PENDING_MODERATION',
    moderation_result   JSONB,
    admin_decision      JSONB,
    created_at          TIMESTAMP DEFAULT NOW(),
    updated_at          TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_reviews_product_id ON reviews(product_id);
CREATE INDEX idx_reviews_user_id ON reviews(user_id);
CREATE INDEX idx_reviews_order_id ON reviews(order_id);
CREATE INDEX idx_reviews_shop_id ON reviews(shop_id);
CREATE INDEX idx_reviews_status ON reviews(status);
CREATE INDEX idx_reviews_product_status ON reviews(product_id, status);
CREATE INDEX idx_reviews_user_status ON reviews(user_id, status);
CREATE INDEX idx_reviews_shop_status ON reviews(shop_id, status);
CREATE INDEX idx_reviews_created_at ON reviews(created_at);
CREATE INDEX idx_reviews_user_name ON reviews(user_name);

CREATE TRIGGER set_reviews_updated_at
    BEFORE UPDATE ON reviews
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Enable RLS
ALTER TABLE reviews ENABLE ROW LEVEL SECURITY;
