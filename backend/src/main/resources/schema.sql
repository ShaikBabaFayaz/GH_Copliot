CREATE UNIQUE INDEX IF NOT EXISTS uq_products_sku_ci
    ON products (sku);