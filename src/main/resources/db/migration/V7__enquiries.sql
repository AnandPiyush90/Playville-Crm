CREATE TABLE pv_enquiries (
 id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
 branch_id INT UNSIGNED NOT NULL,
 converted_customer_id INT UNSIGNED NULL,
 parent_name VARCHAR(150) NOT NULL,
 phone_number VARCHAR(15) NOT NULL,
 email VARCHAR(150) NULL,
 lead_source VARCHAR(30) NULL,
 status VARCHAR(30) NOT NULL DEFAULT 'NEW',
 visit_scheduled_at DATETIME NULL,
 notes TEXT NULL,
 created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
 CONSTRAINT fk_enquiry_branch FOREIGN KEY (branch_id) REFERENCES pv_branches(id),
 CONSTRAINT fk_enquiry_customer FOREIGN KEY (converted_customer_id) REFERENCES pv_customers(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_enquiry_branch_status_date ON pv_enquiries(branch_id, status, created_at);
CREATE INDEX idx_enquiry_branch_phone ON pv_enquiries(branch_id, phone_number);
