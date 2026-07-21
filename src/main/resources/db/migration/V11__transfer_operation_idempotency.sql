ALTER TABLE pv_stock_transfers
    ADD COLUMN dispatch_idempotency_key VARCHAR(100) NULL AFTER idempotency_key,
    ADD COLUMN receive_idempotency_key VARCHAR(100) NULL AFTER dispatch_idempotency_key,
    ADD CONSTRAINT uq_transfer_dispatch_idempotency UNIQUE (dispatch_idempotency_key),
    ADD CONSTRAINT uq_transfer_receive_idempotency UNIQUE (receive_idempotency_key);
