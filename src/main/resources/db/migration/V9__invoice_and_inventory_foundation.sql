-- Invoice and inventory foundation. Monetary values are stored as DECIMAL.

CREATE TABLE pv_product_categories (
    id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    parent_id INT UNSIGNED NULL,
    category_code VARCHAR(50) NOT NULL,
    category_name VARCHAR(100) NOT NULL,
    display_order INT NOT NULL DEFAULT 0,
    is_active TINYINT(1) NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uq_product_category_code UNIQUE (category_code),
    CONSTRAINT fk_product_category_parent FOREIGN KEY (parent_id) REFERENCES pv_product_categories(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE pv_tax_profiles (
    id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    tax_code VARCHAR(30) NOT NULL,
    tax_name VARCHAR(100) NOT NULL,
    hsn_sac_code VARCHAR(30) NULL,
    rate_percent DECIMAL(5,2) NOT NULL DEFAULT 0.00,
    price_includes_tax TINYINT(1) NOT NULL DEFAULT 1,
    effective_from DATE NOT NULL,
    effective_to DATE NULL,
    is_active TINYINT(1) NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uq_tax_profile_code_from UNIQUE (tax_code, effective_from)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE pv_products (
    id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    product_code VARCHAR(60) NOT NULL,
    product_name VARCHAR(150) NOT NULL,
    description TEXT NULL,
    category_id INT UNSIGNED NOT NULL,
    product_type VARCHAR(30) NOT NULL,
    package_id INT UNSIGNED NULL,
    track_inventory TINYINT(1) NOT NULL DEFAULT 1,
    is_active TINYINT(1) NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uq_product_code UNIQUE (product_code),
    CONSTRAINT fk_product_category FOREIGN KEY (category_id) REFERENCES pv_product_categories(id) ON DELETE RESTRICT,
    CONSTRAINT fk_product_package FOREIGN KEY (package_id) REFERENCES pv_packages(id) ON DELETE RESTRICT,
    CONSTRAINT chk_product_type CHECK (product_type IN ('RETAIL', 'MEMBERSHIP', 'SERVICE'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE pv_product_skus (
    id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    product_id INT UNSIGNED NOT NULL,
    sku_code VARCHAR(80) NOT NULL,
    barcode VARCHAR(100) NULL,
    variant_attributes_json JSON NULL,
    unit_of_measure VARCHAR(20) NOT NULL DEFAULT 'PIECE',
    default_sale_price DECIMAL(10,2) NOT NULL,
    default_cost_price DECIMAL(10,2) NULL,
    tax_profile_id INT UNSIGNED NULL,
    has_expiry TINYINT(1) NOT NULL DEFAULT 0,
    is_active TINYINT(1) NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uq_product_sku_code UNIQUE (sku_code),
    CONSTRAINT uq_product_sku_barcode UNIQUE (barcode),
    CONSTRAINT fk_sku_product FOREIGN KEY (product_id) REFERENCES pv_products(id) ON DELETE RESTRICT,
    CONSTRAINT fk_sku_tax_profile FOREIGN KEY (tax_profile_id) REFERENCES pv_tax_profiles(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE pv_branch_skus (
    id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    branch_id INT UNSIGNED NOT NULL,
    sku_id INT UNSIGNED NOT NULL,
    sale_price_override DECIMAL(10,2) NULL,
    reorder_level DECIMAL(12,3) NOT NULL DEFAULT 0.000,
    reorder_quantity DECIMAL(12,3) NOT NULL DEFAULT 0.000,
    allow_negative_stock TINYINT(1) NOT NULL DEFAULT 0,
    is_available TINYINT(1) NOT NULL DEFAULT 1,
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uq_branch_sku UNIQUE (branch_id, sku_id),
    CONSTRAINT fk_branch_sku_branch FOREIGN KEY (branch_id) REFERENCES pv_branches(id) ON DELETE RESTRICT,
    CONSTRAINT fk_branch_sku_sku FOREIGN KEY (sku_id) REFERENCES pv_product_skus(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE pv_inventory_balances (
    id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    branch_id INT UNSIGNED NOT NULL,
    sku_id INT UNSIGNED NOT NULL,
    quantity_on_hand DECIMAL(12,3) NOT NULL DEFAULT 0.000,
    quantity_reserved DECIMAL(12,3) NOT NULL DEFAULT 0.000,
    version BIGINT NOT NULL DEFAULT 0,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uq_inventory_balance UNIQUE (branch_id, sku_id),
    CONSTRAINT fk_inventory_balance_branch FOREIGN KEY (branch_id) REFERENCES pv_branches(id) ON DELETE RESTRICT,
    CONSTRAINT fk_inventory_balance_sku FOREIGN KEY (sku_id) REFERENCES pv_product_skus(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE pv_inventory_movements (
    id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    branch_id INT UNSIGNED NOT NULL,
    sku_id INT UNSIGNED NOT NULL,
    movement_type VARCHAR(30) NOT NULL,
    quantity_delta DECIMAL(12,3) NOT NULL,
    unit_cost_snapshot DECIMAL(10,2) NULL,
    reference_type VARCHAR(40) NOT NULL,
    reference_id INT UNSIGNED NULL,
    reason_code VARCHAR(60) NULL,
    notes TEXT NULL,
    performed_by_staff_id INT UNSIGNED NULL,
    approved_by_staff_id INT UNSIGNED NULL,
    idempotency_key VARCHAR(100) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_inventory_movement_idempotency UNIQUE (idempotency_key),
    CONSTRAINT fk_inventory_movement_branch FOREIGN KEY (branch_id) REFERENCES pv_branches(id) ON DELETE RESTRICT,
    CONSTRAINT fk_inventory_movement_sku FOREIGN KEY (sku_id) REFERENCES pv_product_skus(id) ON DELETE RESTRICT,
    CONSTRAINT fk_inventory_movement_staff FOREIGN KEY (performed_by_staff_id) REFERENCES pv_staff(id) ON DELETE SET NULL,
    CONSTRAINT fk_inventory_movement_approver FOREIGN KEY (approved_by_staff_id) REFERENCES pv_staff(id) ON DELETE SET NULL,
    CONSTRAINT chk_inventory_movement_quantity CHECK (quantity_delta <> 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX idx_inventory_movement_branch_sku_date ON pv_inventory_movements(branch_id, sku_id, created_at);
CREATE INDEX idx_inventory_movement_reference ON pv_inventory_movements(reference_type, reference_id);

CREATE TABLE pv_invoices (
    id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    invoice_number VARCHAR(50) NULL,
    branch_id INT UNSIGNED NOT NULL,
    customer_id INT UNSIGNED NULL,
    checkin_id INT UNSIGNED NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    invoice_type VARCHAR(30) NOT NULL DEFAULT 'SALE',
    invoice_date DATETIME NULL,
    customer_name_snapshot VARCHAR(150) NULL,
    customer_phone_snapshot VARCHAR(15) NULL,
    customer_email_snapshot VARCHAR(150) NULL,
    subtotal DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    discount_total DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    tax_total DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    rounding_adjustment DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    grand_total DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    amount_paid DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    balance_due DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    notes TEXT NULL,
    issued_by_staff_id INT UNSIGNED NULL,
    voided_by_staff_id INT UNSIGNED NULL,
    void_reason TEXT NULL,
    idempotency_key VARCHAR(100) NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uq_invoice_number UNIQUE (invoice_number),
    CONSTRAINT uq_invoice_idempotency UNIQUE (idempotency_key),
    CONSTRAINT fk_invoice_branch FOREIGN KEY (branch_id) REFERENCES pv_branches(id) ON DELETE RESTRICT,
    CONSTRAINT fk_invoice_customer FOREIGN KEY (customer_id) REFERENCES pv_customers(id) ON DELETE RESTRICT,
    CONSTRAINT fk_invoice_checkin FOREIGN KEY (checkin_id) REFERENCES pv_checkins(id) ON DELETE RESTRICT,
    CONSTRAINT fk_invoice_issuer FOREIGN KEY (issued_by_staff_id) REFERENCES pv_staff(id) ON DELETE SET NULL,
    CONSTRAINT fk_invoice_voider FOREIGN KEY (voided_by_staff_id) REFERENCES pv_staff(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX idx_invoice_branch_date ON pv_invoices(branch_id, created_at);
CREATE INDEX idx_invoice_customer ON pv_invoices(customer_id, created_at);

CREATE TABLE pv_invoice_items (
    id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    invoice_id INT UNSIGNED NOT NULL,
    line_number INT NOT NULL,
    line_type VARCHAR(30) NOT NULL,
    product_id INT UNSIGNED NULL,
    sku_id INT UNSIGNED NULL,
    package_id INT UNSIGNED NULL,
    purchase_id INT UNSIGNED NULL,
    description_snapshot VARCHAR(200) NOT NULL,
    sku_snapshot VARCHAR(100) NULL,
    quantity DECIMAL(12,3) NOT NULL,
    unit_price DECIMAL(12,2) NOT NULL,
    gross_amount DECIMAL(12,2) NOT NULL,
    discount_amount DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    taxable_amount DECIMAL(12,2) NOT NULL,
    tax_rate_snapshot DECIMAL(5,2) NOT NULL DEFAULT 0.00,
    tax_amount DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    line_total DECIMAL(12,2) NOT NULL,
    returned_quantity DECIMAL(12,3) NOT NULL DEFAULT 0.000,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_invoice_line UNIQUE (invoice_id, line_number),
    CONSTRAINT fk_invoice_item_invoice FOREIGN KEY (invoice_id) REFERENCES pv_invoices(id) ON DELETE RESTRICT,
    CONSTRAINT fk_invoice_item_product FOREIGN KEY (product_id) REFERENCES pv_products(id) ON DELETE RESTRICT,
    CONSTRAINT fk_invoice_item_sku FOREIGN KEY (sku_id) REFERENCES pv_product_skus(id) ON DELETE RESTRICT,
    CONSTRAINT fk_invoice_item_package FOREIGN KEY (package_id) REFERENCES pv_packages(id) ON DELETE RESTRICT,
    CONSTRAINT fk_invoice_item_purchase FOREIGN KEY (purchase_id) REFERENCES pv_purchases(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE pv_payments (
    id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    invoice_id INT UNSIGNED NOT NULL,
    branch_id INT UNSIGNED NOT NULL,
    payment_type VARCHAR(20) NOT NULL DEFAULT 'PAYMENT',
    payment_mode VARCHAR(20) NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    provider_reference VARCHAR(100) NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'COMPLETED',
    received_by_staff_id INT UNSIGNED NULL,
    idempotency_key VARCHAR(100) NULL,
    paid_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    notes TEXT NULL,
    CONSTRAINT uq_payment_idempotency UNIQUE (idempotency_key),
    CONSTRAINT fk_payment_invoice FOREIGN KEY (invoice_id) REFERENCES pv_invoices(id) ON DELETE RESTRICT,
    CONSTRAINT fk_payment_branch FOREIGN KEY (branch_id) REFERENCES pv_branches(id) ON DELETE RESTRICT,
    CONSTRAINT fk_payment_staff FOREIGN KEY (received_by_staff_id) REFERENCES pv_staff(id) ON DELETE SET NULL,
    CONSTRAINT chk_payment_amount CHECK (amount > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE pv_invoice_sequences (
    id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    branch_id INT UNSIGNED NOT NULL,
    financial_year VARCHAR(9) NOT NULL,
    next_number INT NOT NULL DEFAULT 1,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_invoice_sequence UNIQUE (branch_id, financial_year),
    CONSTRAINT fk_invoice_sequence_branch FOREIGN KEY (branch_id) REFERENCES pv_branches(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
