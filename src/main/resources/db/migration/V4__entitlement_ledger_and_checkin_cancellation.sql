CREATE TABLE pv_entitlement_transactions (
 id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
 entitlement_id INT UNSIGNED NOT NULL,
 checkin_id INT UNSIGNED NULL,
 staff_id INT UNSIGNED NULL,
 transaction_type VARCHAR(30) NOT NULL,
 sessions_delta INT NOT NULL,
 reserved_delta INT NOT NULL,
 notes TEXT NULL,
 occurred_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 CONSTRAINT fk_entitlement_tx_entitlement FOREIGN KEY (entitlement_id) REFERENCES pv_customer_entitlements(id),
 CONSTRAINT fk_entitlement_tx_checkin FOREIGN KEY (checkin_id) REFERENCES pv_checkins(id),
 CONSTRAINT fk_entitlement_tx_staff FOREIGN KEY (staff_id) REFERENCES pv_staff(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_entitlement_tx_entitlement_date ON pv_entitlement_transactions(entitlement_id, occurred_at);
CREATE INDEX idx_entitlement_tx_checkin ON pv_entitlement_transactions(checkin_id);

ALTER TABLE pv_checkins
 ADD COLUMN cancellation_reason TEXT NULL,
 MODIFY COLUMN status ENUM('Active', 'Completed', 'Auto-Closed', 'Cancelled') DEFAULT 'Active';
