-- ============================================================
--  V2 - Migrate notification channel: Google Chat -> Email
-- ============================================================

ALTER TABLE pv_branches
    CHANGE COLUMN google_chat_webhook notification_email VARCHAR(150)
    COMMENT 'Branch manager email - receives all operational notifications';

ALTER TABLE pv_notification_log
    CHANGE COLUMN webhook_url recipient_email VARCHAR(150)
    COMMENT 'Email address the notification was sent to';

ALTER TABLE pv_notification_log
    ADD COLUMN subject VARCHAR(255) NULL AFTER event_type,
    ADD COLUMN cc_emails VARCHAR(500) NULL COMMENT 'Comma-separated CC list (e.g. admin email)' AFTER recipient_email;

-- Seed branch notification emails (update with real addresses before going live)
UPDATE pv_branches SET notification_email = 'nallurahalli@playville.in' WHERE branch_code = 'NAL';
UPDATE pv_branches SET notification_email = 'kadugodi@playville.in' WHERE branch_code = 'KAD';
UPDATE pv_branches SET notification_email = 'akshayanagar@playville.in' WHERE branch_code = 'AKS';
