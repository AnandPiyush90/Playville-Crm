ALTER TABLE pv_email_provider_configs
    ADD COLUMN last_error_message VARCHAR(500) NULL AFTER last_error_code;
