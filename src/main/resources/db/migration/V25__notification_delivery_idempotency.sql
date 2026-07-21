ALTER TABLE pv_notification_deliveries
    ADD COLUMN idempotency_key VARCHAR(100) NULL AFTER error_message;

UPDATE pv_notification_deliveries
SET idempotency_key = CONCAT('legacy-', id)
WHERE idempotency_key IS NULL;

ALTER TABLE pv_notification_deliveries
    MODIFY COLUMN idempotency_key VARCHAR(100) NOT NULL;

CREATE UNIQUE INDEX uk_delivery_branch_idempotency
    ON pv_notification_deliveries(branch_id, idempotency_key);
