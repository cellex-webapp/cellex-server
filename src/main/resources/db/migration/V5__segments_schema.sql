-- V5: Customer Segments migration
-- Migrate from MongoDB collection to PostgreSQL tables

-- ========== CUSTOMER SEGMENTS TABLE ==========
CREATE TABLE IF NOT EXISTS customer_segments (
    id          UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(100)    NOT NULL,
    min_spend   DECIMAL(15,2)   NOT NULL,
    max_spend   DECIMAL(15,2),
    level       INTEGER         NOT NULL,
    description TEXT,
    created_at  TIMESTAMP       DEFAULT NOW(),
    updated_at  TIMESTAMP       DEFAULT NOW()
);

-- ========== USER SEGMENT HISTORY TABLE ==========
CREATE TABLE IF NOT EXISTS user_segment_history (
    id           UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id      UUID            NOT NULL REFERENCES users(id),
    segment_id   UUID            NOT NULL REFERENCES customer_segments(id),
    segment_name VARCHAR(100)    NOT NULL,
    from_date    TIMESTAMP       NOT NULL,
    to_date      TIMESTAMP,
    note         TEXT,
    created_at   TIMESTAMP       DEFAULT NOW()
);

-- ========== INDEXES ==========
CREATE INDEX IF NOT EXISTS idx_segments_level ON customer_segments(level);
CREATE INDEX IF NOT EXISTS idx_user_seg_hist_user ON user_segment_history(user_id);
CREATE INDEX IF NOT EXISTS idx_user_seg_hist_segment ON user_segment_history(segment_id);

-- ========== AUTO-UPDATE TRIGGERS ==========
CREATE TRIGGER update_customer_segments_updated_at
    BEFORE UPDATE ON customer_segments
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ========== ROW LEVEL SECURITY ==========
ALTER TABLE customer_segments ENABLE ROW LEVEL SECURITY;
ALTER TABLE user_segment_history ENABLE ROW LEVEL SECURITY;
