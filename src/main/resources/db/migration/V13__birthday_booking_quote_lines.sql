CREATE TABLE pv_birthday_quote_lines (
    id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    birthday_booking_id INT UNSIGNED NOT NULL,
    line_number INT NOT NULL,
    category VARCHAR(30) NOT NULL,
    description_snapshot VARCHAR(200) NOT NULL,
    quantity DECIMAL(12,3) NOT NULL,
    unit_price DECIMAL(12,2) NOT NULL,
    tax_rate DECIMAL(5,2) NOT NULL DEFAULT 0.00,
    line_total DECIMAL(12,2) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_birthday_quote_line UNIQUE (birthday_booking_id, line_number),
    CONSTRAINT fk_birthday_quote_line_booking FOREIGN KEY (birthday_booking_id) REFERENCES pv_birthday_bookings(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX idx_birthday_quote_line_booking ON pv_birthday_quote_lines(birthday_booking_id);
