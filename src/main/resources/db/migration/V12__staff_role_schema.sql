CREATE TABLE IF NOT EXISTS shop_roles (
    id UUID PRIMARY KEY,
    shop_id UUID NOT NULL REFERENCES shops(id),
    name VARCHAR(100) NOT NULL,
    description TEXT,
    permissions JSONB,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT uk_shop_roles_shop_name UNIQUE (shop_id, name)
);

CREATE TABLE IF NOT EXISTS shop_staff_invitations (
    id UUID PRIMARY KEY,
    shop_id UUID NOT NULL REFERENCES shops(id),
    shop_role_id UUID NOT NULL REFERENCES shop_roles(id),
    invited_user_id UUID NOT NULL REFERENCES users(id),
    invited_by UUID NOT NULL REFERENCES users(id),
    status VARCHAR(20) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    responded_at TIMESTAMP,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS shop_staff_members (
    id UUID PRIMARY KEY,
    shop_id UUID NOT NULL REFERENCES shops(id),
    user_id UUID NOT NULL REFERENCES users(id),
    shop_role_id UUID NOT NULL REFERENCES shop_roles(id),
    joined_at TIMESTAMP NOT NULL,
    left_at TIMESTAMP,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_shop_staff_member_active
    ON shop_staff_members(shop_id, user_id)
    WHERE is_active = TRUE;

