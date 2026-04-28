-- ============================================================
-- V11: Add SKU snapshot fields for order_items
-- ============================================================

ALTER TABLE order_items
    ADD COLUMN IF NOT EXISTS sku_id VARCHAR(50),
    ADD COLUMN IF NOT EXISTS sku_code VARCHAR(120),
    ADD COLUMN IF NOT EXISTS variation_data JSONB;

CREATE INDEX IF NOT EXISTS idx_order_items_sku_id ON order_items(sku_id);
