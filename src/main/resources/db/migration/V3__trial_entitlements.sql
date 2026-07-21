CREATE TABLE pv_customer_entitlements (
 id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY, customer_id INT UNSIGNED NOT NULL, branch_id INT UNSIGNED NOT NULL,
 type VARCHAR(40) NOT NULL, status VARCHAR(20) NOT NULL, sessions_granted INT NOT NULL, sessions_reserved INT NOT NULL DEFAULT 0,
 campaign_code VARCHAR(60), reason_text TEXT, expires_at DATETIME NOT NULL, version BIGINT NOT NULL DEFAULT 0,
 created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
 CONSTRAINT fk_entitlement_customer FOREIGN KEY (customer_id) REFERENCES pv_customers(id), CONSTRAINT fk_entitlement_branch FOREIGN KEY (branch_id) REFERENCES pv_branches(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_entitlement_customer_status ON pv_customer_entitlements(customer_id, type, status);
CREATE INDEX idx_entitlement_expiry ON pv_customer_entitlements(status, expires_at);
ALTER TABLE pv_checkins ADD COLUMN visit_type VARCHAR(40) NOT NULL DEFAULT 'PAID', ADD COLUMN entitlement_id INT UNSIGNED NULL, ADD CONSTRAINT fk_checkin_entitlement FOREIGN KEY (entitlement_id) REFERENCES pv_customer_entitlements(id);
