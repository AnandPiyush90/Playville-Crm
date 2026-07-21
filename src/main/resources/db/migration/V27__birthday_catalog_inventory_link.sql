ALTER TABLE pv_birthday_catalog_items
    ADD COLUMN sku_id INT UNSIGNED NULL AFTER category,
    ADD COLUMN description VARCHAR(500) NULL AFTER item_name,
    ADD COLUMN inventory_mode VARCHAR(20) NOT NULL DEFAULT 'NONE' AFTER tax_rate,
    ADD COLUMN minimum_order_quantity DECIMAL(12,3) NOT NULL DEFAULT 1.000 AFTER inventory_mode,
    ADD COLUMN lead_time_days INT NOT NULL DEFAULT 0 AFTER minimum_order_quantity,
    ADD CONSTRAINT fk_birthday_catalog_sku FOREIGN KEY (sku_id) REFERENCES pv_product_skus(id) ON DELETE RESTRICT;

CREATE TABLE pv_birthday_branch_catalog_items (
    id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    branch_id INT UNSIGNED NOT NULL,
    catalog_item_id INT UNSIGNED NOT NULL,
    display_name_override VARCHAR(150) NULL,
    unit_price_override DECIMAL(12,2) NULL,
    is_available TINYINT(1) NOT NULL DEFAULT 1,
    reserve_inventory TINYINT(1) NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uq_birthday_branch_catalog UNIQUE (branch_id, catalog_item_id),
    CONSTRAINT fk_birthday_branch_catalog_branch FOREIGN KEY (branch_id) REFERENCES pv_branches(id) ON DELETE RESTRICT,
    CONSTRAINT fk_birthday_branch_catalog_item FOREIGN KEY (catalog_item_id) REFERENCES pv_birthday_catalog_items(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT INTO pv_birthday_branch_catalog_items (branch_id, catalog_item_id, is_available, reserve_inventory)
SELECT b.id, c.id, 1, 0 FROM pv_branches b CROSS JOIN pv_birthday_catalog_items c;

ALTER TABLE pv_birthday_quote_lines
    ADD COLUMN catalog_item_id INT UNSIGNED NULL AFTER category,
    ADD COLUMN sku_id INT UNSIGNED NULL AFTER catalog_item_id,
    ADD COLUMN inventory_reservation_required TINYINT(1) NOT NULL DEFAULT 0 AFTER sku_id,
    ADD CONSTRAINT fk_birthday_quote_catalog_item FOREIGN KEY (catalog_item_id) REFERENCES pv_birthday_catalog_items(id) ON DELETE RESTRICT,
    ADD CONSTRAINT fk_birthday_quote_sku FOREIGN KEY (sku_id) REFERENCES pv_product_skus(id) ON DELETE RESTRICT;

CREATE TABLE pv_birthday_inventory_reservations (
    id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    birthday_booking_id INT UNSIGNED NOT NULL,
    birthday_quote_line_id INT UNSIGNED NOT NULL,
    branch_id INT UNSIGNED NOT NULL,
    sku_id INT UNSIGNED NOT NULL,
    quantity DECIMAL(12,3) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'RESERVED',
    reserved_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    released_at DATETIME NULL,
    consumed_at DATETIME NULL,
    CONSTRAINT uq_birthday_reservation_line UNIQUE (birthday_quote_line_id),
    CONSTRAINT fk_birthday_reservation_booking FOREIGN KEY (birthday_booking_id) REFERENCES pv_birthday_bookings(id) ON DELETE RESTRICT,
    CONSTRAINT fk_birthday_reservation_line FOREIGN KEY (birthday_quote_line_id) REFERENCES pv_birthday_quote_lines(id) ON DELETE RESTRICT,
    CONSTRAINT fk_birthday_reservation_branch FOREIGN KEY (branch_id) REFERENCES pv_branches(id) ON DELETE RESTRICT,
    CONSTRAINT fk_birthday_reservation_sku FOREIGN KEY (sku_id) REFERENCES pv_product_skus(id) ON DELETE RESTRICT,
    INDEX idx_birthday_reservation_booking_status (birthday_booking_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
