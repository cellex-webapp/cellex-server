-- ============================================================
-- CELLEX - PostgreSQL (Supabase) Schema Migration V9
-- Module: Address Book Enhancement - Add tag column
-- Date: 2026-03-08
-- ============================================================

-- Add tag column to user_addresses table (nullable)
ALTER TABLE user_addresses ADD COLUMN IF NOT EXISTS tag VARCHAR(50);

-- Comment
COMMENT ON COLUMN user_addresses.tag IS 'Optional label for the address, e.g. Nhà riêng, Công ty';
