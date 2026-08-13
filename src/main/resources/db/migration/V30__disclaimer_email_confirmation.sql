ALTER TABLE pv_branches
    ADD COLUMN email_confirmation_enabled TINYINT(1) NOT NULL DEFAULT 0,
    ADD COLUMN disclaimer_email_link_ttl_hours INT NOT NULL DEFAULT 24;

ALTER TABLE pv_disclaimer_signing_requests
    ADD COLUMN token_sha256 CHAR(64) NULL,
    ADD COLUMN sent_at DATETIME(6) NULL,
    ADD UNIQUE KEY uq_disclaimer_request_token (token_sha256);
