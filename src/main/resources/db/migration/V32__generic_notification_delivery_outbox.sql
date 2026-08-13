ALTER TABLE pv_notification_deliveries
    ADD COLUMN purpose VARCHAR(40) NOT NULL DEFAULT 'INVOICE',
    ADD COLUMN reference_type VARCHAR(40) NULL,
    ADD COLUMN reference_id VARCHAR(80) NULL,
    ADD COLUMN provider_type VARCHAR(30) NULL,
    ADD COLUMN attempt_count INT NOT NULL DEFAULT 0,
    ADD COLUMN next_retry_at DATETIME(6) NULL,
    ADD COLUMN last_attempt_at DATETIME(6) NULL;

CREATE INDEX idx_delivery_branch_status ON pv_notification_deliveries(branch_id, status, created_at);

CREATE TABLE pv_email_outbox (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    delivery_id INT UNSIGNED NOT NULL,
    encrypted_subject TEXT NOT NULL,
    encrypted_body LONGTEXT NOT NULL,
    attachment_filename VARCHAR(255) NULL,
    attachment_bytes MEDIUMBLOB NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    next_attempt_at DATETIME(6) NOT NULL,
    locked_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    UNIQUE KEY uq_email_outbox_delivery(delivery_id),
    KEY idx_email_outbox_due(status, next_attempt_at),
    CONSTRAINT fk_email_outbox_delivery FOREIGN KEY(delivery_id) REFERENCES pv_notification_deliveries(id) ON DELETE RESTRICT
);
