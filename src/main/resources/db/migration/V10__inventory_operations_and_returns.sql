CREATE TABLE pv_suppliers (
    id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    supplier_name VARCHAR(150) NOT NULL,
    phone VARCHAR(20) NULL,
    email VARCHAR(150) NULL,
    tax_number VARCHAR(50) NULL,
    is_active TINYINT(1) NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uq_supplier_name UNIQUE (supplier_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE pv_stock_receipts (
    id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    branch_id INT UNSIGNED NOT NULL,
    supplier_id INT UNSIGNED NULL,
    supplier_invoice_reference VARCHAR(100) NULL,
    received_at DATETIME NOT NULL,
    notes TEXT NULL,
    received_by_staff_id INT UNSIGNED NULL,
    idempotency_key VARCHAR(100) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_stock_receipt_idempotency UNIQUE (idempotency_key),
    CONSTRAINT fk_stock_receipt_branch FOREIGN KEY (branch_id) REFERENCES pv_branches(id) ON DELETE RESTRICT,
    CONSTRAINT fk_stock_receipt_supplier FOREIGN KEY (supplier_id) REFERENCES pv_suppliers(id) ON DELETE RESTRICT,
    CONSTRAINT fk_stock_receipt_staff FOREIGN KEY (received_by_staff_id) REFERENCES pv_staff(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE pv_inventory_batches (
    id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    branch_id INT UNSIGNED NOT NULL,
    sku_id INT UNSIGNED NOT NULL,
    supplier_id INT UNSIGNED NULL,
    stock_receipt_id INT UNSIGNED NULL,
    batch_number VARCHAR(100) NULL,
    received_at DATETIME NOT NULL,
    manufactured_on DATE NULL,
    expires_on DATE NULL,
    unit_cost DECIMAL(10,2) NULL,
    quantity_received DECIMAL(12,3) NOT NULL,
    quantity_remaining DECIMAL(12,3) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_inventory_batch_branch FOREIGN KEY (branch_id) REFERENCES pv_branches(id) ON DELETE RESTRICT,
    CONSTRAINT fk_inventory_batch_sku FOREIGN KEY (sku_id) REFERENCES pv_product_skus(id) ON DELETE RESTRICT,
    CONSTRAINT fk_inventory_batch_supplier FOREIGN KEY (supplier_id) REFERENCES pv_suppliers(id) ON DELETE RESTRICT,
    CONSTRAINT fk_inventory_batch_receipt FOREIGN KEY (stock_receipt_id) REFERENCES pv_stock_receipts(id) ON DELETE RESTRICT,
    CONSTRAINT chk_inventory_batch_quantity CHECK (quantity_received > 0 AND quantity_remaining >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX idx_inventory_batch_fefo ON pv_inventory_batches(branch_id, sku_id, status, expires_on, received_at);

ALTER TABLE pv_inventory_movements
    ADD COLUMN batch_id INT UNSIGNED NULL AFTER sku_id,
    ADD CONSTRAINT fk_inventory_movement_batch FOREIGN KEY (batch_id) REFERENCES pv_inventory_batches(id) ON DELETE RESTRICT;

CREATE TABLE pv_stock_transfers (
    id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    transfer_number VARCHAR(50) NOT NULL,
    source_branch_id INT UNSIGNED NOT NULL,
    destination_branch_id INT UNSIGNED NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    notes TEXT NULL,
    created_by_staff_id INT UNSIGNED NULL,
    dispatched_by_staff_id INT UNSIGNED NULL,
    received_by_staff_id INT UNSIGNED NULL,
    dispatched_at DATETIME NULL,
    received_at DATETIME NULL,
    idempotency_key VARCHAR(100) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uq_transfer_number UNIQUE (transfer_number),
    CONSTRAINT uq_transfer_idempotency UNIQUE (idempotency_key),
    CONSTRAINT fk_transfer_source FOREIGN KEY (source_branch_id) REFERENCES pv_branches(id) ON DELETE RESTRICT,
    CONSTRAINT fk_transfer_destination FOREIGN KEY (destination_branch_id) REFERENCES pv_branches(id) ON DELETE RESTRICT,
    CONSTRAINT fk_transfer_creator FOREIGN KEY (created_by_staff_id) REFERENCES pv_staff(id) ON DELETE SET NULL,
    CONSTRAINT fk_transfer_dispatcher FOREIGN KEY (dispatched_by_staff_id) REFERENCES pv_staff(id) ON DELETE SET NULL,
    CONSTRAINT fk_transfer_receiver FOREIGN KEY (received_by_staff_id) REFERENCES pv_staff(id) ON DELETE SET NULL,
    CONSTRAINT chk_transfer_branches CHECK (source_branch_id <> destination_branch_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE pv_stock_transfer_items (
    id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    transfer_id INT UNSIGNED NOT NULL,
    sku_id INT UNSIGNED NOT NULL,
    batch_id INT UNSIGNED NULL,
    requested_quantity DECIMAL(12,3) NOT NULL,
    dispatched_quantity DECIMAL(12,3) NOT NULL DEFAULT 0.000,
    received_quantity DECIMAL(12,3) NOT NULL DEFAULT 0.000,
    variance_reason VARCHAR(255) NULL,
    CONSTRAINT fk_transfer_item_transfer FOREIGN KEY (transfer_id) REFERENCES pv_stock_transfers(id) ON DELETE RESTRICT,
    CONSTRAINT fk_transfer_item_sku FOREIGN KEY (sku_id) REFERENCES pv_product_skus(id) ON DELETE RESTRICT,
    CONSTRAINT fk_transfer_item_batch FOREIGN KEY (batch_id) REFERENCES pv_inventory_batches(id) ON DELETE RESTRICT,
    CONSTRAINT chk_transfer_item_requested CHECK (requested_quantity > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE pv_sales_returns (
    id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    original_invoice_id INT UNSIGNED NOT NULL,
    credit_invoice_id INT UNSIGNED NOT NULL,
    branch_id INT UNSIGNED NOT NULL,
    reason VARCHAR(255) NOT NULL,
    notes TEXT NULL,
    returned_by_staff_id INT UNSIGNED NULL,
    idempotency_key VARCHAR(100) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_sales_return_credit_invoice UNIQUE (credit_invoice_id),
    CONSTRAINT uq_sales_return_idempotency UNIQUE (idempotency_key),
    CONSTRAINT fk_sales_return_original FOREIGN KEY (original_invoice_id) REFERENCES pv_invoices(id) ON DELETE RESTRICT,
    CONSTRAINT fk_sales_return_credit FOREIGN KEY (credit_invoice_id) REFERENCES pv_invoices(id) ON DELETE RESTRICT,
    CONSTRAINT fk_sales_return_branch FOREIGN KEY (branch_id) REFERENCES pv_branches(id) ON DELETE RESTRICT,
    CONSTRAINT fk_sales_return_staff FOREIGN KEY (returned_by_staff_id) REFERENCES pv_staff(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE pv_sales_return_items (
    id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    sales_return_id INT UNSIGNED NOT NULL,
    original_invoice_item_id INT UNSIGNED NOT NULL,
    quantity DECIMAL(12,3) NOT NULL,
    resellable TINYINT(1) NOT NULL DEFAULT 0,
    CONSTRAINT fk_sales_return_item_return FOREIGN KEY (sales_return_id) REFERENCES pv_sales_returns(id) ON DELETE RESTRICT,
    CONSTRAINT fk_sales_return_item_original_line FOREIGN KEY (original_invoice_item_id) REFERENCES pv_invoice_items(id) ON DELETE RESTRICT,
    CONSTRAINT chk_sales_return_item_quantity CHECK (quantity > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
