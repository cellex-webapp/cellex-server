-- Allow staff accounts to persist their role in the legacy users table.
-- The application already writes Role.STAFF during invitation acceptance.

ALTER TABLE users DROP CONSTRAINT IF EXISTS users_role_check;

ALTER TABLE users
    ADD CONSTRAINT users_role_check
    CHECK (role IN ('ADMIN', 'USER', 'VENDOR', 'STAFF'));