-- V4: Categories & Category Attributes migration
-- Migrate from MongoDB collections to PostgreSQL tables

-- ========== CATEGORIES TABLE ==========
CREATE TABLE IF NOT EXISTS categories (
    id              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    name            VARCHAR(255)    NOT NULL,
    slug            VARCHAR(255)    UNIQUE NOT NULL,
    parent_id       UUID            REFERENCES categories(id),
    image_url       TEXT,
    description     TEXT,
    is_active       BOOLEAN         DEFAULT true,
    sort_order      INTEGER         DEFAULT 0,
    created_at      TIMESTAMP       DEFAULT NOW(),
    updated_at      TIMESTAMP       DEFAULT NOW()
);

-- ========== CATEGORY ATTRIBUTES TABLE ==========
CREATE TABLE IF NOT EXISTS category_attributes (
    id                  UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    category_id         UUID            NOT NULL REFERENCES categories(id),
    attribute_name      VARCHAR(255)    NOT NULL,
    attribute_key       VARCHAR(100)    NOT NULL,
    data_type           VARCHAR(20)     NOT NULL,
    unit                VARCHAR(20),
    is_required         BOOLEAN         DEFAULT false,
    is_highlight        BOOLEAN         DEFAULT false,
    select_options      JSONB,
    validation_pattern  VARCHAR(255),
    sort_order          INTEGER         DEFAULT 0,
    description         TEXT,
    is_active           BOOLEAN         DEFAULT true,
    created_at          TIMESTAMP       DEFAULT NOW(),
    updated_at          TIMESTAMP       DEFAULT NOW()
);

-- ========== INDEXES ==========
CREATE INDEX IF NOT EXISTS idx_categories_parent_id ON categories(parent_id);
CREATE INDEX IF NOT EXISTS idx_categories_slug ON categories(slug);
CREATE INDEX IF NOT EXISTS idx_categories_is_active ON categories(is_active);

CREATE INDEX IF NOT EXISTS idx_cat_attr_category_id ON category_attributes(category_id);
CREATE INDEX IF NOT EXISTS idx_cat_attr_category_active ON category_attributes(category_id, is_active);
CREATE UNIQUE INDEX IF NOT EXISTS idx_cat_attr_unique_key ON category_attributes(category_id, attribute_key);

-- ========== AUTO-UPDATE TRIGGERS ==========
CREATE TRIGGER update_categories_updated_at
    BEFORE UPDATE ON categories
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_category_attributes_updated_at
    BEFORE UPDATE ON category_attributes
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ========== ROW LEVEL SECURITY ==========
ALTER TABLE categories ENABLE ROW LEVEL SECURITY;
ALTER TABLE category_attributes ENABLE ROW LEVEL SECURITY;
