ALTER TABLE pv_customer_entitlements
 ADD COLUMN trial_customer_id INT UNSIGNED
     GENERATED ALWAYS AS (CASE WHEN type = 'COMPLIMENTARY_TRIAL' THEN customer_id ELSE NULL END) STORED,
 ADD UNIQUE KEY uq_one_trial_per_customer (trial_customer_id);

ALTER TABLE pv_checkins
 MODIFY COLUMN status ENUM('Active', 'Completed', 'Auto_Closed', 'Cancelled') DEFAULT 'Active',
 ADD COLUMN active_customer_id INT UNSIGNED
     GENERATED ALWAYS AS (CASE WHEN status = 'Active' THEN customer_id ELSE NULL END) STORED,
 ADD UNIQUE KEY uq_one_active_checkin_per_customer (active_customer_id);

ALTER TABLE pv_purchases
 ADD COLUMN idempotency_key VARCHAR(100) NULL,
 ADD UNIQUE KEY uq_purchase_idempotency_key (idempotency_key);

ALTER TABLE pv_onboarding_idempotency
 MODIFY COLUMN customer_id INT UNSIGNED NULL,
 MODIFY COLUMN next_action VARCHAR(30) NULL,
 ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'COMPLETED';

CREATE INDEX idx_trial_reporting_staff ON pv_checkins(staff_id, checkin_time);
