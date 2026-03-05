-- ============================================================
-- V2: Shops Module - PostgreSQL (Supabase) Schema
-- ============================================================

-- 1. Bảng shops
CREATE TABLE IF NOT EXISTS shops (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_id        UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    shop_name       VARCHAR(255) NOT NULL,
    description     TEXT,
    logo_url        TEXT,
    address_json    JSONB DEFAULT '{}'::jsonb,
    phone_number    VARCHAR(20),
    email           VARCHAR(255),
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    rating          DECIMAL(3,2) DEFAULT 0.00,
    rejection_reason TEXT,
    created_at      TIMESTAMP DEFAULT NOW(),
    updated_at      TIMESTAMP DEFAULT NOW()
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_shops_owner_id ON shops(owner_id);
CREATE INDEX IF NOT EXISTS idx_shops_status ON shops(status);
CREATE INDEX IF NOT EXISTS idx_shops_created_at ON shops(created_at);
CREATE INDEX IF NOT EXISTS idx_shops_status_created_at ON shops(status, created_at);

-- Auto-update updated_at trigger
CREATE OR REPLACE FUNCTION update_shops_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trigger_shops_updated_at ON shops;
CREATE TRIGGER trigger_shops_updated_at
    BEFORE UPDATE ON shops
    FOR EACH ROW
    EXECUTE FUNCTION update_shops_updated_at();

-- 2. Bảng staff_profiles (cho tương lai)
CREATE TABLE IF NOT EXISTS staff_profiles (
    id       UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id  UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    shop_id  UUID NOT NULL REFERENCES shops(id) ON DELETE CASCADE,
    role_id  INTEGER REFERENCES roles(id),
    UNIQUE(user_id, shop_id)
);

CREATE INDEX IF NOT EXISTS idx_staff_profiles_user_id ON staff_profiles(user_id);
CREATE INDEX IF NOT EXISTS idx_staff_profiles_shop_id ON staff_profiles(shop_id);

-- Row Level Security
ALTER TABLE shops ENABLE ROW LEVEL SECURITY;
ALTER TABLE staff_profiles ENABLE ROW LEVEL SECURITY;
