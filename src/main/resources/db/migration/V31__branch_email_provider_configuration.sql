CREATE TABLE pv_email_provider_configs (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    branch_id INT UNSIGNED NOT NULL,
    provider_type VARCHAR(30) NOT NULL,
    host VARCHAR(150) NOT NULL,
    port INT NOT NULL,
    username VARCHAR(150) NOT NULL,
    encrypted_password TEXT NOT NULL,
    tls_mode VARCHAR(20) NOT NULL,
    from_email VARCHAR(150) NOT NULL,
    from_name VARCHAR(150) NOT NULL,
    reply_to_email VARCHAR(150) NULL,
    enabled TINYINT(1) NOT NULL DEFAULT 0,
    last_test_status VARCHAR(20) NULL,
    last_tested_at DATETIME(6) NULL,
    last_error_code VARCHAR(100) NULL,
    UNIQUE KEY uq_email_provider_branch (branch_id),
    CONSTRAINT fk_email_provider_branch FOREIGN KEY (branch_id) REFERENCES pv_branches(id) ON DELETE RESTRICT
);
