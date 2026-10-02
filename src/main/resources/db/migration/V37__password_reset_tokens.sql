CREATE TABLE IF NOT EXISTS pv_password_reset_tokens (
    id BIGINT NOT NULL AUTO_INCREMENT,
    token_hash VARCHAR(128) NOT NULL,
    email VARCHAR(150) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at DATETIME NOT NULL,
    used_at DATETIME NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_password_reset_token_hash (token_hash),
    KEY idx_password_reset_email (email),
    KEY idx_password_reset_expires_at (expires_at),
    KEY idx_password_reset_used_at (used_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SET @pv_staff_token_version_exists := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'pv_staff'
      AND column_name = 'token_version'
);

SET @pv_staff_token_version_sql := IF(
    @pv_staff_token_version_exists = 0,
    'ALTER TABLE pv_staff ADD COLUMN token_version INT NOT NULL DEFAULT 0',
    'SELECT 1'
);

PREPARE pv_staff_token_version_stmt FROM @pv_staff_token_version_sql;
EXECUTE pv_staff_token_version_stmt;
DEALLOCATE PREPARE pv_staff_token_version_stmt;
