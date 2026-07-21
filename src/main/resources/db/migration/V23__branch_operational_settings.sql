ALTER TABLE pv_branches
    ADD COLUMN alternate_phone VARCHAR(15) NULL AFTER phone,
    ADD COLUMN timezone VARCHAR(50) NOT NULL DEFAULT 'Asia/Kolkata' AFTER alternate_phone,
    ADD COLUMN invoice_prefix VARCHAR(20) NULL AFTER tax_state_code;
