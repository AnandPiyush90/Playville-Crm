ALTER TABLE pv_customers
 ADD COLUMN emergency_contact_name VARCHAR(150) NULL,
 ADD COLUMN emergency_contact_phone VARCHAR(15) NULL,
 ADD COLUMN marketing_consent TINYINT(1) NOT NULL DEFAULT 0,
 ADD COLUMN disclaimer_version VARCHAR(30) NULL;

CREATE TABLE pv_onboarding_idempotency (
 idempotency_key VARCHAR(100) PRIMARY KEY,
 customer_id INT UNSIGNED NOT NULL,
 entitlement_id INT UNSIGNED NULL,
 next_action VARCHAR(30) NOT NULL,
 created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 CONSTRAINT fk_onboarding_idempotency_customer FOREIGN KEY (customer_id) REFERENCES pv_customers(id),
 CONSTRAINT fk_onboarding_idempotency_entitlement FOREIGN KEY (entitlement_id) REFERENCES pv_customer_entitlements(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
