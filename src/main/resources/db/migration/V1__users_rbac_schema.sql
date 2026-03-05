-- ============================================================
-- CELLEX - PostgreSQL (Supabase) Schema Migration V1
-- Module: Users, RBAC, Addresses, OTPs
-- Date: 2026-03-05
-- ============================================================

-- Enable UUID extension (Supabase has this by default)
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ============================================================
-- 1. ROLES TABLE
-- ============================================================
CREATE TABLE IF NOT EXISTS roles (
    id          SERIAL          PRIMARY KEY,
    role_name   VARCHAR(50)     UNIQUE NOT NULL,
    description TEXT
);

-- Seed default roles
INSERT INTO roles (role_name, description) VALUES
    ('ADMIN', 'System administrator with full access'),
    ('VENDOR', 'Shop owner / seller'),
    ('STAFF', 'Shop staff with limited permissions'),
    ('USER', 'Regular customer')
ON CONFLICT (role_name) DO NOTHING;

-- ============================================================
-- 2. PERMISSIONS TABLE
-- ============================================================
CREATE TABLE IF NOT EXISTS permissions (
    id              SERIAL          PRIMARY KEY,
    permission_key  VARCHAR(100)    UNIQUE NOT NULL,
    module          VARCHAR(50)     NOT NULL
);

-- Seed default permissions
INSERT INTO permissions (permission_key, module) VALUES
    ('MANAGE_USERS', 'USER'),
    ('VIEW_USERS', 'USER'),
    ('MANAGE_SHOPS', 'SHOP'),
    ('MANAGE_INVENTORY', 'ERP'),
    ('VIEW_REPORT', 'ANALYTICS'),
    ('MANAGE_ORDERS', 'ORDER'),
    ('VIEW_ORDERS', 'ORDER'),
    ('MANAGE_PRODUCTS', 'PRODUCT'),
    ('VIEW_PRODUCTS', 'PRODUCT'),
    ('MANAGE_COUPONS', 'COUPON'),
    ('MANAGE_REVIEWS', 'REVIEW'),
    ('MANAGE_LIVESTREAM', 'LIVESTREAM'),
    ('MANAGE_CHAT', 'CHAT'),
    ('MANAGE_NOTIFICATIONS', 'NOTIFICATION')
ON CONFLICT (permission_key) DO NOTHING;

-- ============================================================
-- 3. ROLE_PERMISSIONS TABLE (N-N join)
-- ============================================================
CREATE TABLE IF NOT EXISTS role_permissions (
    role_id         INTEGER     NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    permission_id   INTEGER     NOT NULL REFERENCES permissions(id) ON DELETE CASCADE,
    PRIMARY KEY (role_id, permission_id)
);

-- Assign all permissions to ADMIN role
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.role_name = 'ADMIN'
ON CONFLICT DO NOTHING;

-- ============================================================
-- 4. USERS TABLE
-- ============================================================
CREATE TABLE IF NOT EXISTS users (
    id                  UUID            PRIMARY KEY DEFAULT uuid_generate_v4(),
    email               VARCHAR(255)    UNIQUE NOT NULL,
    password            VARCHAR(255)    NOT NULL,
    full_name           VARCHAR(255)    NOT NULL,
    phone_number        VARCHAR(20),
    avatar_url          TEXT,
    role                VARCHAR(20)     DEFAULT 'USER',  -- Backward compat: ADMIN, USER, VENDOR
    customer_segment_id VARCHAR(50),    -- Temporarily VARCHAR for cross-DB compat with MongoDB segments
    total_spend         DECIMAL(15,2)   DEFAULT 0.00,
    is_active           BOOLEAN         DEFAULT true,
    is_banned           BOOLEAN         DEFAULT false,
    ban_reason          TEXT,
    banned_at           TIMESTAMP,
    banned_by           UUID            REFERENCES users(id) ON DELETE SET NULL,
    created_at          TIMESTAMP       DEFAULT NOW(),
    updated_at          TIMESTAMP       DEFAULT NOW()
);

-- Indexes for users
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_users_customer_segment_id ON users(customer_segment_id);
CREATE INDEX IF NOT EXISTS idx_users_is_active ON users(is_active);
CREATE INDEX IF NOT EXISTS idx_users_is_banned ON users(is_banned);
CREATE INDEX IF NOT EXISTS idx_users_created_at ON users(created_at);

-- ============================================================
-- 5. USER_ROLES TABLE (N-N join)
-- ============================================================
CREATE TABLE IF NOT EXISTS user_roles (
    user_id     UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_id     INTEGER     NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    assigned_at TIMESTAMP   DEFAULT NOW(),
    PRIMARY KEY (user_id, role_id)
);

-- ============================================================
-- 6. USER_ADDRESSES TABLE
-- ============================================================
CREATE TABLE IF NOT EXISTS user_addresses (
    id              UUID            PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id         UUID            NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    province_code   VARCHAR(10),
    province_name   VARCHAR(100),
    commune_code    VARCHAR(10),
    commune_name    VARCHAR(100),
    detail_address  TEXT,
    full_address    TEXT,
    is_default      BOOLEAN         DEFAULT false,
    created_at      TIMESTAMP       DEFAULT NOW(),
    updated_at      TIMESTAMP       DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_user_addresses_user_id ON user_addresses(user_id);

-- ============================================================
-- 7. OTPS TABLE
-- ============================================================
CREATE TABLE IF NOT EXISTS otps (
    id              UUID            PRIMARY KEY DEFAULT uuid_generate_v4(),
    code            VARCHAR(6)      NOT NULL,
    email           VARCHAR(255)    NOT NULL,
    is_used         BOOLEAN         DEFAULT false,
    created_at      TIMESTAMP       DEFAULT NOW(),
    expired_at      TIMESTAMP       NOT NULL,
    full_name       VARCHAR(255),
    hashed_password VARCHAR(255),
    phone_number    VARCHAR(20)
);

CREATE INDEX IF NOT EXISTS idx_otps_email ON otps(email);
CREATE INDEX IF NOT EXISTS idx_otps_code_email ON otps(code, email);

-- ============================================================
-- AUTO-UPDATE updated_at TRIGGER
-- ============================================================
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
   NEW.updated_at = NOW();
   RETURN NEW;
END;
$$ language 'plpgsql';

-- Apply trigger to users table
DROP TRIGGER IF EXISTS trigger_users_updated_at ON users;
CREATE TRIGGER trigger_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Apply trigger to user_addresses table
DROP TRIGGER IF EXISTS trigger_user_addresses_updated_at ON user_addresses;
CREATE TRIGGER trigger_user_addresses_updated_at
    BEFORE UPDATE ON user_addresses
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- ============================================================
-- ROW LEVEL SECURITY (Supabase)
-- ============================================================
-- Enable RLS on users table
ALTER TABLE users ENABLE ROW LEVEL SECURITY;

-- Policy: Service role can do everything (for backend use)
CREATE POLICY "Service role full access on users"
    ON users
    FOR ALL
    USING (true)
    WITH CHECK (true);

-- Enable RLS on user_addresses table
ALTER TABLE user_addresses ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Service role full access on user_addresses"
    ON user_addresses
    FOR ALL
    USING (true)
    WITH CHECK (true);

-- Enable RLS on otps table
ALTER TABLE otps ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Service role full access on otps"
    ON otps
    FOR ALL
    USING (true)
    WITH CHECK (true);
