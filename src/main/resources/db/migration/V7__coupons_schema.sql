-- ============================================================
-- V7: Coupon System Tables
-- ============================================================

-- 1. Coupon Campaigns table
CREATE TABLE IF NOT EXISTS coupon_campaigns (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title           VARCHAR(255) NOT NULL,
    description     TEXT,
    code_template   VARCHAR(100),
    coupon_type     VARCHAR(50) NOT NULL,
    discount_value  DOUBLE PRECISION,
    min_order_amount DOUBLE PRECISION,
    applicable_product_ids  JSONB,
    applicable_category_ids JSONB,
    start_date      TIMESTAMP,
    end_date        TIMESTAMP,
    distribution_type VARCHAR(50),
    max_total_issuance INTEGER,
    per_user_limit  INTEGER,
    current_issuance INTEGER DEFAULT 0,
    status          VARCHAR(50) DEFAULT 'DRAFT',
    scheduled_at    TIMESTAMP,
    distributed_at  TIMESTAMP,
    is_active       BOOLEAN DEFAULT TRUE,
    created_by      VARCHAR(50),
    note            TEXT,
    created_at      TIMESTAMP DEFAULT NOW(),
    updated_at      TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_coupon_campaigns_status ON coupon_campaigns(status);
CREATE INDEX idx_coupon_campaigns_is_active ON coupon_campaigns(is_active);
CREATE INDEX idx_coupon_campaigns_created_by ON coupon_campaigns(created_by);

CREATE TRIGGER set_coupon_campaigns_updated_at
    BEFORE UPDATE ON coupon_campaigns
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- 2. Segment Coupons table
CREATE TABLE IF NOT EXISTS segment_coupons (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    segment_id          VARCHAR(50),
    code_prefix         VARCHAR(50),
    title               VARCHAR(255),
    description         TEXT,
    discount_type       VARCHAR(50),
    discount_value      DOUBLE PRECISION,
    min_order_amount    DOUBLE PRECISION,
    valid_hours         INTEGER,
    start_date          DATE,
    end_date            DATE,
    is_active           BOOLEAN DEFAULT TRUE,
    is_auto_on_upgrade  BOOLEAN DEFAULT FALSE,
    schedule_frequency  VARCHAR(50) DEFAULT 'NONE',
    schedule_day_of_week VARCHAR(20),
    schedule_day_of_month INTEGER,
    schedule_month_day  VARCHAR(10),
    schedule_time       TIME DEFAULT '00:00:00',
    next_scheduled_date TIMESTAMP,
    max_uses_per_user   INTEGER,
    created_at          TIMESTAMP DEFAULT NOW(),
    updated_at          TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_segment_coupons_segment_id ON segment_coupons(segment_id);
CREATE INDEX idx_segment_coupons_is_active ON segment_coupons(is_active);
CREATE INDEX idx_segment_coupons_schedule ON segment_coupons(schedule_frequency, next_scheduled_date);

CREATE TRIGGER set_segment_coupons_updated_at
    BEFORE UPDATE ON segment_coupons
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- 3. User Coupons table
CREATE TABLE IF NOT EXISTS user_coupons (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id             VARCHAR(50),
    segment_coupon_id   VARCHAR(50),
    campaign_id         VARCHAR(50),
    code                VARCHAR(100),
    title               VARCHAR(255),
    description         TEXT,
    coupon_type         VARCHAR(50),
    discount_value      DOUBLE PRECISION,
    min_order_amount    DOUBLE PRECISION,
    applicable_product_ids  JSONB,
    applicable_category_ids JSONB,
    issued_date         TIMESTAMP,
    expires_at          TIMESTAMP,
    status              VARCHAR(50) DEFAULT 'ACTIVE',
    redeemed_order_id   VARCHAR(50),
    redeemed_at         TIMESTAMP,
    issued_via          VARCHAR(50),
    issued_by           VARCHAR(50),
    created_at          TIMESTAMP DEFAULT NOW(),
    updated_at          TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_user_coupons_user_id ON user_coupons(user_id);
CREATE INDEX idx_user_coupons_code ON user_coupons(code);
CREATE INDEX idx_user_coupons_status ON user_coupons(status);
CREATE INDEX idx_user_coupons_user_segment ON user_coupons(user_id, segment_coupon_id);
CREATE INDEX idx_user_coupons_user_campaign ON user_coupons(user_id, campaign_id);
CREATE UNIQUE INDEX idx_user_coupons_user_code ON user_coupons(user_id, code);

CREATE TRIGGER set_user_coupons_updated_at
    BEFORE UPDATE ON user_coupons
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- 4. Campaign Distribution Logs table
CREATE TABLE IF NOT EXISTS campaign_distribution_logs (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    campaign_id         VARCHAR(50),
    admin_id            VARCHAR(50),
    filter_criteria     JSONB,
    recipients_count    INTEGER,
    success_count       INTEGER,
    failed_count        INTEGER,
    error_summary       TEXT,
    execution_time_ms   BIGINT,
    created_at          TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_campaign_dist_logs_campaign_id ON campaign_distribution_logs(campaign_id);
CREATE INDEX idx_campaign_dist_logs_admin_id ON campaign_distribution_logs(admin_id);

-- Enable RLS
ALTER TABLE coupon_campaigns ENABLE ROW LEVEL SECURITY;
ALTER TABLE segment_coupons ENABLE ROW LEVEL SECURITY;
ALTER TABLE user_coupons ENABLE ROW LEVEL SECURITY;
ALTER TABLE campaign_distribution_logs ENABLE ROW LEVEL SECURITY;
