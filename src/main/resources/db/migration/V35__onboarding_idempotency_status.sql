-- Some databases were baselined from V5/V8 without the onboarding idempotency status column.
-- Onboarding then fails with: Unknown column 'oi1_0.status' in 'field list'.

SET @col := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'pv_onboarding_idempotency'
      AND COLUMN_NAME = 'status'
);
SET @sql := IF(@col = 0,
    'ALTER TABLE pv_onboarding_idempotency ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT ''COMPLETED'' AFTER next_action',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

ALTER TABLE pv_onboarding_idempotency
    MODIFY COLUMN customer_id INT UNSIGNED NULL,
    MODIFY COLUMN next_action VARCHAR(30) NULL;
