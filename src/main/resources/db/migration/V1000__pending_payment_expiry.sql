-- Thêm cột lưu thời điểm hết hạn thanh toán
-- NULL = không cần đếm ngược (COD đã confirm, hoặc đã thanh toán)
ALTER TABLE orders
    ADD COLUMN IF NOT EXISTS payment_expires_at TIMESTAMPTZ DEFAULT NULL;

-- Index để scheduler query hiệu quả
CREATE INDEX IF NOT EXISTS idx_orders_payment_expires_at
    ON orders (payment_expires_at)
    WHERE payment_expires_at IS NOT NULL;

-- Index composite để FE query pending orders của user
CREATE INDEX IF NOT EXISTS idx_orders_user_pending
    ON orders (user_id, status, is_paid)
    WHERE is_paid = false;
