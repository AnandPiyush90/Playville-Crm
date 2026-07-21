-- InvoicePayment records creation time through Hibernate's @CreationTimestamp.
-- V9 omitted the physical column, causing invoice list/payment-ledger reads to fail.
ALTER TABLE pv_payments
    ADD COLUMN created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP AFTER notes;
