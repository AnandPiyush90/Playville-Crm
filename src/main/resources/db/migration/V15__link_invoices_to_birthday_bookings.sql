ALTER TABLE pv_invoices
    ADD COLUMN birthday_booking_id INT UNSIGNED NULL AFTER checkin_id,
    ADD CONSTRAINT fk_invoice_birthday_booking
        FOREIGN KEY (birthday_booking_id) REFERENCES pv_birthday_bookings(id),
    ADD UNIQUE INDEX uq_invoice_birthday_booking (birthday_booking_id);
