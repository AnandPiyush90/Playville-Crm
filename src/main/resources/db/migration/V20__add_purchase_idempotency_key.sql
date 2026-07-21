-- This database was baselined at V8 before the V8 purchase idempotency column
-- existed. Add the omitted column without changing existing purchase records.
ALTER TABLE pv_purchases
    ADD COLUMN idempotency_key VARCHAR(100) NULL AFTER notes,
    ADD UNIQUE KEY uq_purchase_idempotency_key (idempotency_key);
