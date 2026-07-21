ALTER TABLE pv_birthday_bookings
    ADD COLUMN actual_kids INT NULL AFTER expected_guests,
    ADD COLUMN actual_adults INT NULL AFTER actual_kids,
    ADD COLUMN actual_extra_minutes INT NOT NULL DEFAULT 0 AFTER actual_adults,
    ADD COLUMN completion_notes TEXT NULL AFTER notes;
