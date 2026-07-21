ALTER TABLE pv_birthday_bookings DROP INDEX uq_bday_slot;
CREATE INDEX idx_bday_branch_interval
    ON pv_birthday_bookings(branch_id, party_date, party_slot_start, party_slot_end, status);
