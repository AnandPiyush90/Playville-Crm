ALTER TABLE pv_enquiries
    ADD COLUMN idempotency_key VARCHAR(100) NULL AFTER notes,
    ADD UNIQUE KEY uq_enquiry_idempotency_key (idempotency_key);
