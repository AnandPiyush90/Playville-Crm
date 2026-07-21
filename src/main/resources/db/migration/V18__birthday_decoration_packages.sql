CREATE TABLE pv_birthday_decoration_packages (
    id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    branch_id INT UNSIGNED NOT NULL,
    package_code VARCHAR(80) NOT NULL,
    package_name VARCHAR(150) NOT NULL,
    description TEXT NULL,
    price DECIMAL(12,2) NOT NULL,
    tax_rate DECIMAL(5,2) NOT NULL DEFAULT 0.00,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uq_birthday_decoration_branch_code UNIQUE (branch_id, package_code),
    CONSTRAINT fk_birthday_decoration_branch FOREIGN KEY (branch_id) REFERENCES pv_branches(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
