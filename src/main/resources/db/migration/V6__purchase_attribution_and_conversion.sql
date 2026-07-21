ALTER TABLE pv_purchases
 ADD COLUMN source_checkin_id INT UNSIGNED NULL,
 ADD COLUMN source_trial_entitlement_id INT UNSIGNED NULL,
 ADD COLUMN purchase_context VARCHAR(30) NOT NULL DEFAULT 'STANDARD',
 ADD CONSTRAINT fk_purchase_source_checkin FOREIGN KEY (source_checkin_id) REFERENCES pv_checkins(id),
 ADD CONSTRAINT fk_purchase_source_trial FOREIGN KEY (source_trial_entitlement_id) REFERENCES pv_customer_entitlements(id),
 ADD UNIQUE KEY uq_purchase_source_checkin (source_checkin_id);
ALTER TABLE pv_checkins
 ADD COLUMN conversion_outcome VARCHAR(30) NULL,
 ADD COLUMN conversion_reason VARCHAR(30) NULL,
 ADD COLUMN conversion_purchase_id INT UNSIGNED NULL,
 ADD COLUMN follow_up_at DATETIME NULL,
 ADD CONSTRAINT fk_checkin_conversion_purchase FOREIGN KEY (conversion_purchase_id) REFERENCES pv_purchases(id);
