ALTER TABLE pv_enquiries
    ADD COLUMN child_name VARCHAR(100) NULL AFTER visit_scheduled_at,
    ADD COLUMN child_dob DATE NULL AFTER child_name;
