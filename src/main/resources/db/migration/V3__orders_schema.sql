-- ============================================================
-- V3: Orders Module - PostgreSQL (Supabase) Schema
-- ============================================================

-- 1. Bảng orders
CREATE TABLE IF NOT EXISTS orders (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_code           VARCHAR(50) UNIQUE NOT NULL,
    user_id              UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    shop_id              UUID NOT NULL REFERENCES shops(id) ON DELETE CASCADE,
    shop_name            VARCHAR(255),
    shipping_address_json JSONB,
    subtotal             DECIMAL(15,2) NOT NULL DEFAULT 0,
    shipping_fee         DECIMAL(15,2) DEFAULT 0.00,
    discount_amount      DECIMAL(15,2) DEFAULT 0.00,
    total_amount         DECIMAL(15,2) NOT NULL DEFAULT 0,
    coupon_code          VARCHAR(50),
    user_coupon_id       VARCHAR(50),
    payment_method       VARCHAR(20),
    is_paid              BOOLEAN DEFAULT false,
    paid_at              TIMESTAMP,
    vnpay_transaction_id VARCHAR(100),
    vnpay_response_code  VARCHAR(20),
    vnpay_bank_code      VARCHAR(20),
    vnpay_pay_date       VARCHAR(50),
    status               VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    status_history_json  JSONB DEFAULT '[]'::jsonb,
    note                 TEXT,
    is_from_cart         BOOLEAN DEFAULT false,
    cancel_reason        TEXT,
    cancelled_at         TIMESTAMP,
    confirmed_at         TIMESTAMP,
    shipping_at          TIMESTAMP,
    delivered_at         TIMESTAMP,
    created_at           TIMESTAMP DEFAULT NOW(),
    updated_at           TIMESTAMP DEFAULT NOW()
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_orders_user_id ON orders(user_id);
CREATE INDEX IF NOT EXISTS idx_orders_shop_id ON orders(shop_id);
CREATE INDEX IF NOT EXISTS idx_orders_status ON orders(status);
CREATE INDEX IF NOT EXISTS idx_orders_order_code ON orders(order_code);
CREATE INDEX IF NOT EXISTS idx_orders_created_at ON orders(created_at);
CREATE INDEX IF NOT EXISTS idx_orders_user_status ON orders(user_id, status);
CREATE INDEX IF NOT EXISTS idx_orders_shop_status ON orders(shop_id, status);
CREATE INDEX IF NOT EXISTS idx_orders_is_paid ON orders(is_paid);
CREATE INDEX IF NOT EXISTS idx_orders_status_paid_created ON orders(status, is_paid, created_at);

-- Auto-update updated_at trigger
CREATE OR REPLACE FUNCTION update_orders_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trigger_orders_updated_at ON orders;
CREATE TRIGGER trigger_orders_updated_at
    BEFORE UPDATE ON orders
    FOR EACH ROW
    EXECUTE FUNCTION update_orders_updated_at();

-- 2. Bảng order_items
CREATE TABLE IF NOT EXISTS order_items (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id        UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    product_id      VARCHAR(50) NOT NULL,
    product_name    VARCHAR(255) NOT NULL,
    product_image   TEXT,
    price           DECIMAL(15,2) NOT NULL,
    quantity        INTEGER NOT NULL,
    subtotal        DECIMAL(15,2) NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_order_items_order_id ON order_items(order_id);
CREATE INDEX IF NOT EXISTS idx_order_items_product_id ON order_items(product_id);

-- 3. Bảng order_status_history
CREATE TABLE IF NOT EXISTS order_status_history (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id    UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    status      VARCHAR(20) NOT NULL,
    note        TEXT,
    updated_by  VARCHAR(50),
    updated_at  TIMESTAMP DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_order_status_history_order_id ON order_status_history(order_id);

-- Row Level Security
ALTER TABLE orders ENABLE ROW LEVEL SECURITY;
ALTER TABLE order_items ENABLE ROW LEVEL SECURITY;
ALTER TABLE order_status_history ENABLE ROW LEVEL SECURITY;
