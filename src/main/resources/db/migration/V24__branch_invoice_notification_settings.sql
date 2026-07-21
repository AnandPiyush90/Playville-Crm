ALTER TABLE pv_branches
    ADD COLUMN email_sharing_enabled TINYINT(1) NOT NULL DEFAULT 0 AFTER notification_email,
    ADD COLUMN invoice_from_email VARCHAR(150) NULL AFTER email_sharing_enabled,
    ADD COLUMN invoice_reply_to_email VARCHAR(150) NULL AFTER invoice_from_email,
    ADD COLUMN whatsapp_sharing_enabled TINYINT(1) NOT NULL DEFAULT 0 AFTER invoice_reply_to_email,
    ADD COLUMN whatsapp_phone_number_id VARCHAR(50) NULL AFTER whatsapp_sharing_enabled,
    ADD COLUMN whatsapp_invoice_template_name VARCHAR(100) NULL AFTER whatsapp_phone_number_id,
    ADD COLUMN whatsapp_language_code VARCHAR(10) NOT NULL DEFAULT 'en' AFTER whatsapp_invoice_template_name;

CREATE TABLE pv_notification_deliveries (
    id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    branch_id INT UNSIGNED NOT NULL,
    invoice_id INT UNSIGNED NULL,
    customer_id INT UNSIGNED NULL,
    triggered_by_staff_id INT UNSIGNED NULL,
    channel VARCHAR(20) NOT NULL,
    destination VARCHAR(150) NOT NULL,
    status VARCHAR(20) NOT NULL,
    provider_reference VARCHAR(150) NULL,
    error_message TEXT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    sent_at DATETIME NULL,
    CONSTRAINT fk_delivery_branch FOREIGN KEY (branch_id) REFERENCES pv_branches(id) ON DELETE RESTRICT,
    CONSTRAINT fk_delivery_invoice FOREIGN KEY (invoice_id) REFERENCES pv_invoices(id) ON DELETE SET NULL,
    CONSTRAINT fk_delivery_customer FOREIGN KEY (customer_id) REFERENCES pv_customers(id) ON DELETE SET NULL,
    CONSTRAINT fk_delivery_staff FOREIGN KEY (triggered_by_staff_id) REFERENCES pv_staff(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX idx_delivery_branch_created ON pv_notification_deliveries(branch_id, created_at);
CREATE INDEX idx_delivery_invoice ON pv_notification_deliveries(invoice_id);
