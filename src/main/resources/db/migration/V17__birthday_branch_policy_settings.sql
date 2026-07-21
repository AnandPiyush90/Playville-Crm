CREATE TABLE pv_birthday_branch_policy_settings (
    id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    branch_id INT UNSIGNED NOT NULL,
    extra_kid_price DECIMAL(12,2) NOT NULL DEFAULT 400.00,
    extra_adult_price DECIMAL(12,2) NOT NULL DEFAULT 200.00,
    extra_time_30_min_price DECIMAL(12,2) NOT NULL DEFAULT 1000.00,
    enquiry_calendar_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    cancellation_policy_json TEXT NULL,
    refund_policy_json TEXT NULL,
    share_channel VARCHAR(30) NULL,
    share_settings_json TEXT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uq_birthday_branch_policy_branch UNIQUE (branch_id),
    CONSTRAINT fk_birthday_branch_policy_branch FOREIGN KEY (branch_id) REFERENCES pv_branches(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
