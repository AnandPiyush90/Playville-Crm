ALTER TABLE pv_branches
    ADD COLUMN invoice_legal_name VARCHAR(150) NULL AFTER branch_name,
    ADD COLUMN gstin VARCHAR(15) NULL AFTER notification_email,
    ADD COLUMN pan_number VARCHAR(10) NULL AFTER gstin,
    ADD COLUMN tax_state VARCHAR(60) NULL AFTER pan_number,
    ADD COLUMN tax_state_code VARCHAR(2) NULL AFTER tax_state,
    ADD COLUMN invoice_terms TEXT NULL AFTER tax_state_code,
    ADD COLUMN invoice_footer VARCHAR(500) NULL AFTER invoice_terms;

ALTER TABLE pv_invoices
    ADD COLUMN seller_legal_name_snapshot VARCHAR(150) NULL AFTER invoice_date,
    ADD COLUMN seller_address_snapshot TEXT NULL AFTER seller_legal_name_snapshot,
    ADD COLUMN seller_phone_snapshot VARCHAR(20) NULL AFTER seller_address_snapshot,
    ADD COLUMN seller_email_snapshot VARCHAR(150) NULL AFTER seller_phone_snapshot,
    ADD COLUMN seller_gstin_snapshot VARCHAR(15) NULL AFTER seller_email_snapshot,
    ADD COLUMN seller_pan_snapshot VARCHAR(10) NULL AFTER seller_gstin_snapshot,
    ADD COLUMN seller_tax_state_snapshot VARCHAR(60) NULL AFTER seller_pan_snapshot,
    ADD COLUMN seller_tax_state_code_snapshot VARCHAR(2) NULL AFTER seller_tax_state_snapshot,
    ADD COLUMN terms_snapshot TEXT NULL AFTER seller_tax_state_code_snapshot,
    ADD COLUMN footer_snapshot VARCHAR(500) NULL AFTER terms_snapshot;

CREATE UNIQUE INDEX uq_branch_gstin ON pv_branches (gstin);
