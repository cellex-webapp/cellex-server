-- V6: Notifications migration
-- Migrate from MongoDB collections to PostgreSQL tables

-- ========== NOTIFICATIONS TABLE ==========
CREATE TABLE IF NOT EXISTS notifications (
    id           UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id      UUID            REFERENCES users(id),   -- NULL for broadcast
    title        VARCHAR(500)    NOT NULL,
    message      TEXT            NOT NULL,
    type         VARCHAR(50)     NOT NULL,
    is_read      BOOLEAN         DEFAULT false,
    read_at      TIMESTAMP,
    is_broadcast BOOLEAN         DEFAULT false,
    metadata     TEXT,
    action_url   TEXT,
    image_url    TEXT,
    created_at   TIMESTAMP       DEFAULT NOW(),
    expires_at   TIMESTAMP
);

-- ========== USER DEVICES TABLE ==========
CREATE TABLE IF NOT EXISTS user_devices (
    id           UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id      UUID            NOT NULL REFERENCES users(id),
    fcm_token    VARCHAR(500)    UNIQUE NOT NULL,
    device_type  VARCHAR(20),
    device_name  VARCHAR(100),
    is_active    BOOLEAN         DEFAULT true,
    created_at   TIMESTAMP       DEFAULT NOW(),
    updated_at   TIMESTAMP       DEFAULT NOW(),
    last_used_at TIMESTAMP
);

-- ========== USER NOTIFICATION READS TABLE ==========
CREATE TABLE IF NOT EXISTS user_notification_reads (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID        NOT NULL REFERENCES users(id),
    notification_id UUID        NOT NULL REFERENCES notifications(id) ON DELETE CASCADE,
    read_at         TIMESTAMP   NOT NULL,
    UNIQUE(user_id, notification_id)
);

-- ========== INDEXES ==========
CREATE INDEX IF NOT EXISTS idx_notifications_user_id ON notifications(user_id);
CREATE INDEX IF NOT EXISTS idx_notifications_type ON notifications(type);
CREATE INDEX IF NOT EXISTS idx_notifications_is_broadcast ON notifications(is_broadcast);
CREATE INDEX IF NOT EXISTS idx_notifications_created_at ON notifications(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_notifications_expires_at ON notifications(expires_at);
CREATE INDEX IF NOT EXISTS idx_notifications_user_read ON notifications(user_id, is_read);

CREATE INDEX IF NOT EXISTS idx_user_devices_user_id ON user_devices(user_id);
CREATE INDEX IF NOT EXISTS idx_user_devices_active ON user_devices(user_id, is_active);

CREATE INDEX IF NOT EXISTS idx_user_notif_reads_user ON user_notification_reads(user_id);
CREATE INDEX IF NOT EXISTS idx_user_notif_reads_notif ON user_notification_reads(notification_id);

-- ========== AUTO-UPDATE TRIGGERS ==========
CREATE TRIGGER update_user_devices_updated_at
    BEFORE UPDATE ON user_devices
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ========== ROW LEVEL SECURITY ==========
ALTER TABLE notifications ENABLE ROW LEVEL SECURITY;
ALTER TABLE user_devices ENABLE ROW LEVEL SECURITY;
ALTER TABLE user_notification_reads ENABLE ROW LEVEL SECURITY;
