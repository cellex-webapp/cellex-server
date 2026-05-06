-- ============================================================
-- V10: Inventory ERP module schema
-- ============================================================

CREATE TABLE IF NOT EXISTS suppliers (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    shop_id           UUID NOT NULL REFERENCES shops(id) ON DELETE CASCADE,
    supplier_name     VARCHAR(255) NOT NULL,
    phone_number      VARCHAR(20) NOT NULL,
    email             VARCHAR(255),
    address           TEXT,
    tax_code          VARCHAR(100) NOT NULL,
    debt_amount       DECIMAL(15,2) NOT NULL DEFAULT 0,
    is_active         BOOLEAN NOT NULL DEFAULT true,
    created_at        TIMESTAMP DEFAULT NOW(),
    updated_at        TIMESTAMP DEFAULT NOW(),
    CONSTRAINT uk_suppliers_shop_phone UNIQUE (shop_id, phone_number),
    CONSTRAINT uk_suppliers_shop_tax UNIQUE (shop_id, tax_code)
);

CREATE TABLE IF NOT EXISTS product_skus (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id        VARCHAR(120) NOT NULL,
    shop_id           UUID NOT NULL REFERENCES shops(id) ON DELETE CASCADE,
    sku_code          VARCHAR(120) NOT NULL UNIQUE,
    variation_data    JSONB NOT NULL DEFAULT '{}'::jsonb,
    variation_hash    VARCHAR(128) NOT NULL,
    price             DECIMAL(15,2) NOT NULL,
    on_hand_stock     INTEGER NOT NULL DEFAULT 0,
    reserved_stock    INTEGER NOT NULL DEFAULT 0,
    safety_stock      INTEGER NOT NULL DEFAULT 0,
    is_active         BOOLEAN NOT NULL DEFAULT true,
    created_at        TIMESTAMP DEFAULT NOW(),
    updated_at        TIMESTAMP DEFAULT NOW(),
    CONSTRAINT uk_product_skus_product_variation UNIQUE (product_id, variation_hash)
);

CREATE TABLE IF NOT EXISTS inventory_batches (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sku_id            UUID NOT NULL REFERENCES product_skus(id) ON DELETE CASCADE,
    supplier_id       UUID NOT NULL REFERENCES suppliers(id) ON DELETE RESTRICT,
    import_price      DECIMAL(15,2) NOT NULL,
    quantity          INTEGER NOT NULL,
    remain_quantity   INTEGER NOT NULL,
    import_date       TIMESTAMP NOT NULL,
    reference_id      VARCHAR(120),
    created_at        TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS inventory_transactions (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sku_id            UUID NOT NULL REFERENCES product_skus(id) ON DELETE CASCADE,
    type              VARCHAR(30) NOT NULL,
    quantity_change   INTEGER NOT NULL,
    reference_id      VARCHAR(120) NOT NULL,
    note              TEXT,
    created_at        TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS inventory_checks (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    shop_id           UUID NOT NULL REFERENCES shops(id) ON DELETE CASCADE,
    status            VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    created_by        UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    created_at        TIMESTAMP DEFAULT NOW(),
    updated_at        TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS inventory_check_items (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    check_id          UUID NOT NULL REFERENCES inventory_checks(id) ON DELETE CASCADE,
    sku_id            UUID NOT NULL REFERENCES product_skus(id) ON DELETE CASCADE,
    system_stock      INTEGER NOT NULL,
    actual_stock      INTEGER NOT NULL,
    difference        INTEGER NOT NULL,
    reason            TEXT,
    CONSTRAINT uk_inventory_check_items_check_sku UNIQUE (check_id, sku_id)
);

CREATE INDEX IF NOT EXISTS idx_suppliers_shop_id ON suppliers(shop_id);
CREATE INDEX IF NOT EXISTS idx_suppliers_name ON suppliers(supplier_name);
CREATE INDEX IF NOT EXISTS idx_product_skus_product_id ON product_skus(product_id);
CREATE INDEX IF NOT EXISTS idx_product_skus_shop_id ON product_skus(shop_id);
CREATE INDEX IF NOT EXISTS idx_product_skus_sku_code ON product_skus(sku_code);
CREATE INDEX IF NOT EXISTS idx_inventory_batches_sku_id ON inventory_batches(sku_id);
CREATE INDEX IF NOT EXISTS idx_inventory_batches_supplier_id ON inventory_batches(supplier_id);
CREATE INDEX IF NOT EXISTS idx_inventory_transactions_sku_id ON inventory_transactions(sku_id);
CREATE INDEX IF NOT EXISTS idx_inventory_transactions_reference_id ON inventory_transactions(reference_id);
CREATE INDEX IF NOT EXISTS idx_inventory_checks_shop_id ON inventory_checks(shop_id);
CREATE INDEX IF NOT EXISTS idx_inventory_check_items_check_id ON inventory_check_items(check_id);

CREATE OR REPLACE FUNCTION update_suppliers_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trigger_suppliers_updated_at ON suppliers;
CREATE TRIGGER trigger_suppliers_updated_at
    BEFORE UPDATE ON suppliers
    FOR EACH ROW
    EXECUTE FUNCTION update_suppliers_updated_at();

CREATE OR REPLACE FUNCTION update_product_skus_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trigger_product_skus_updated_at ON product_skus;
CREATE TRIGGER trigger_product_skus_updated_at
    BEFORE UPDATE ON product_skus
    FOR EACH ROW
    EXECUTE FUNCTION update_product_skus_updated_at();

CREATE OR REPLACE FUNCTION update_inventory_checks_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trigger_inventory_checks_updated_at ON inventory_checks;
CREATE TRIGGER trigger_inventory_checks_updated_at
    BEFORE UPDATE ON inventory_checks
    FOR EACH ROW
    EXECUTE FUNCTION update_inventory_checks_updated_at();

ALTER TABLE suppliers ENABLE ROW LEVEL SECURITY;
ALTER TABLE product_skus ENABLE ROW LEVEL SECURITY;
ALTER TABLE inventory_batches ENABLE ROW LEVEL SECURITY;
ALTER TABLE inventory_transactions ENABLE ROW LEVEL SECURITY;
ALTER TABLE inventory_checks ENABLE ROW LEVEL SECURITY;
ALTER TABLE inventory_check_items ENABLE ROW LEVEL SECURITY;
