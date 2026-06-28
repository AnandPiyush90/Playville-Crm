-- ============================================================
--  PLAYVILLE CRM - Complete MySQL Schema
--  Database : playvilledev
--  Collation : utf8mb4_0900_ai_ci
--  Version   : V1 (Flyway baseline)
--  Generated : 2026-04-25
-- ============================================================

SET FOREIGN_KEY_CHECKS = 0;
SET time_zone = '+05:30';   -- IST

-- ============================================================
-- 1. BRANCHES
--    One row per physical location.
--    settlement_rate = % used in inter-branch pass settlements.
-- ============================================================
CREATE TABLE pv_branches (
    id                  INT UNSIGNED    AUTO_INCREMENT PRIMARY KEY,
    branch_code         VARCHAR(10)     NOT NULL UNIQUE COMMENT 'e.g. NAL, KAD, AKS',
    branch_name         VARCHAR(100)    NOT NULL,
    address             TEXT,
    city                VARCHAR(60)     DEFAULT 'Bangalore',
    phone               VARCHAR(15),
    open_time           TIME            DEFAULT '11:00:00',
    close_time          TIME            DEFAULT '21:00:00',
    closed_day          VARCHAR(20)     DEFAULT 'Tuesday' COMMENT 'Weekly closed day',
    settlement_rate     DECIMAL(5,2)    DEFAULT 100.00 COMMENT 'Inter-branch revenue share %',
    google_chat_webhook VARCHAR(500)    COMMENT 'Branch-specific Google Chat webhook URL',
    is_active           TINYINT(1)      DEFAULT 1,
    created_at          DATETIME        DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT INTO pv_branches (branch_code, branch_name, address, closed_day) VALUES
('NAL', 'Nallurahalli, Whitefield', '2nd Floor, S R Square, Nallurahalli Main Rd, Siddapura, Whitefield, Bangalore 560066', 'Tuesday'),
('KAD', 'Kadugodi, Whitefield',     'Kadugodi, Whitefield, Bangalore', 'Tuesday'),
('AKS', 'Akshayanagar',             'Akshayanagar, Bangalore', NULL);

-- ============================================================
-- 2. STAFF / USERS
--    Tablet login accounts for staff. Role-based.
-- ============================================================
CREATE TABLE pv_staff (
    id              INT UNSIGNED    AUTO_INCREMENT PRIMARY KEY,
    branch_id       INT UNSIGNED    NOT NULL,
    full_name       VARCHAR(100)    NOT NULL,
    email           VARCHAR(150)    UNIQUE,
    phone           VARCHAR(15),
    username        VARCHAR(60)     NOT NULL UNIQUE,
    password_hash   VARCHAR(255)    NOT NULL,
    role            ENUM('admin','manager','staff') DEFAULT 'staff',
    is_active       TINYINT(1)      DEFAULT 1,
    created_at      DATETIME        DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_staff_branch FOREIGN KEY (branch_id) REFERENCES pv_branches(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ============================================================
-- 3. PACKAGES / PRICING PLANS
--    Matches the 5 pricing tiers on playville.in/pricing
-- ============================================================
CREATE TABLE pv_packages (
    id                      INT UNSIGNED    AUTO_INCREMENT PRIMARY KEY,
    package_name            VARCHAR(100)    NOT NULL,
    sessions_purchased      INT             NOT NULL COMMENT 'Sessions customer pays for',
    sessions_bonus          INT             DEFAULT 0 COMMENT 'Free sessions added on top',
    sessions_total          INT             AS (sessions_purchased + sessions_bonus) STORED COMMENT 'Total usable sessions',
    price_per_session       DECIMAL(8,2)    NOT NULL COMMENT 'Effective price per session (incl. bonus)',
    total_price             DECIMAL(8,2)    NOT NULL COMMENT 'Amount customer pays',
    validity_days           INT             DEFAULT NULL COMMENT 'NULL = unlimited validity',
    birthday_discount_pct   DECIMAL(5,2)    DEFAULT 0.00 COMMENT '% discount on birthday bookings',
    recharge_discount_pct   DECIMAL(5,2)    DEFAULT 0.00 COMMENT '% discount on next recharge',
    can_upgrade             TINYINT(1)      DEFAULT 1 COMMENT 'Allow upgrade by paying difference',
    is_active               TINYINT(1)      DEFAULT 1,
    display_order           INT             DEFAULT 0,
    created_at              DATETIME        DEFAULT CURRENT_TIMESTAMP,
    updated_at              DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Seed: exact pricing from website
INSERT INTO pv_packages (package_name, sessions_purchased, sessions_bonus, price_per_session, total_price, validity_days, birthday_discount_pct, recharge_discount_pct, display_order) VALUES
('Single Session',          1,  0,  499.00,  499.00, NULL, 0.00,  0.00,  1),
('Pack of 6 + 1 Free',      6,  1,  428.00, 2999.00, NULL, 0.00,  0.00,  2),
('Pack of 8 + 2 Free',      8,  2,  400.00, 3999.00, NULL, 10.00, 10.00, 3),
('Pack of 10 + 6 Free',    10,  6,  312.00, 4999.00, NULL, 10.00, 15.00, 4),
('Pack of 15 + 15 Free',   15, 15,  249.00, 7499.00, NULL, 15.00, 15.00, 5);

-- ============================================================
-- 4. CUSTOMERS (Parents)
--    phone_number is the primary search/unique key.
--    global_session_balance is the single balance across all kids.
--    portal_pin is 4-digit PIN for customer self-portal login.
-- ============================================================
CREATE TABLE pv_customers (
    id                      INT UNSIGNED    AUTO_INCREMENT PRIMARY KEY,
    phone_number            VARCHAR(15)     NOT NULL UNIQUE COMMENT 'Primary search key – always unique',
    parent_name             VARCHAR(150)    NOT NULL,
    email                   VARCHAR(150),
    lead_source             ENUM(
                                'Walk-in',
                                'Google',
                                'Instagram',
                                'Facebook',
                                'Friend Referral',
                                'School',
                                'Other'
                            )               DEFAULT 'Walk-in',
    -- Session balance (global across all kids under this parent)
    global_session_balance  INT             DEFAULT 0 COMMENT 'Deducted 1 per kid per check-out',
    -- Active package reference (latest purchased package)
    current_package_id      INT UNSIGNED    DEFAULT NULL,
    purchase_branch_id      INT UNSIGNED    DEFAULT NULL COMMENT 'Branch where pack was purchased',
    -- Waiver / disclaimer
    disclaimer_accepted     TINYINT(1)      DEFAULT 0,
    acceptance_timestamp    DATETIME        DEFAULT NULL,
    -- Customer portal access
    portal_pin              CHAR(4)         DEFAULT NULL COMMENT '4-digit PIN for self-portal login',
    -- Metadata
    first_visit_branch_id   INT UNSIGNED    DEFAULT NULL,
    total_visits            INT             DEFAULT 0,
    notes                   TEXT,
    is_active               TINYINT(1)      DEFAULT 1,
    created_at              DATETIME        DEFAULT CURRENT_TIMESTAMP,
    updated_at              DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_customer_package  FOREIGN KEY (current_package_id)   REFERENCES pv_packages(id),
    CONSTRAINT fk_customer_pbranch  FOREIGN KEY (purchase_branch_id)   REFERENCES pv_branches(id),
    CONSTRAINT fk_customer_fbranch  FOREIGN KEY (first_visit_branch_id) REFERENCES pv_branches(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX idx_customer_phone  ON pv_customers(phone_number);
CREATE INDEX idx_customer_email  ON pv_customers(email);
CREATE INDEX idx_customer_name   ON pv_customers(parent_name);

-- ============================================================
-- 5. KIDS
--    Multiple kids per parent. Age check enforced via dob.
--    Max age = 8 years (enforced at app level + DB check).
-- ============================================================
-- ============================================================
-- 5. KIDS
--    Multiple kids per parent.
--    branch_id = branch where kid was registered (for isolation).
--    Age check enforced at application level.
-- ============================================================
CREATE TABLE pv_kids (
    id              INT UNSIGNED    AUTO_INCREMENT PRIMARY KEY,
    branch_id       INT UNSIGNED    NOT NULL COMMENT 'Branch where kid was registered – drives data isolation',
    customer_id     INT UNSIGNED    NOT NULL,
    kid_name        VARCHAR(100)    NOT NULL,
    dob             DATE            NOT NULL COMMENT 'Used for age validation – must be <= 8 yrs (app-enforced)',
    gender          ENUM('Male','Female','Other') DEFAULT NULL,
    special_notes   TEXT            COMMENT 'Allergies, special needs, staff notes',
    is_active       TINYINT(1)      DEFAULT 1,
    created_at      DATETIME        DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_kid_branch    FOREIGN KEY (branch_id)   REFERENCES pv_branches(id),
    CONSTRAINT fk_kid_customer  FOREIGN KEY (customer_id) REFERENCES pv_customers(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX idx_kid_branch    ON pv_kids(branch_id);
CREATE INDEX idx_kid_customer  ON pv_kids(customer_id);
CREATE INDEX idx_kid_branch_customer ON pv_kids(branch_id, customer_id); -- composite for branch-scoped lookups
-- ============================================================
-- 6. CHECK-INS
--    One row per visit session (per parent, not per kid).
--    Kids in that visit are tracked in pv_checkin_kids.
-- ============================================================
CREATE TABLE pv_checkins (
    id                  INT UNSIGNED    AUTO_INCREMENT PRIMARY KEY,
    branch_id           INT UNSIGNED    NOT NULL,
    customer_id         INT UNSIGNED    NOT NULL,
    staff_id            INT UNSIGNED    DEFAULT NULL COMMENT 'Staff who did the check-in',
    checkin_time        DATETIME        DEFAULT CURRENT_TIMESTAMP,
    checkout_time       DATETIME        DEFAULT NULL,
    status              ENUM(
                            'Active',
                            'Completed',
                            'Auto-Closed'
                        )               DEFAULT 'Active',
    kids_count          INT             DEFAULT 0 COMMENT 'Number of kids checked in (for quick reference)',
    sessions_deducted   INT             DEFAULT 0 COMMENT 'Total sessions deducted at checkout',
    extra_charges       DECIMAL(8,2)    DEFAULT 0.00 COMMENT 'Any extra charges (food, etc.)',
    gst_amount          DECIMAL(8,2)    DEFAULT 0.00 COMMENT '18% GST on extra_charges',
    total_charged       DECIMAL(8,2)    DEFAULT 0.00,
    checkout_notes      TEXT,
    auto_closed         TINYINT(1)      DEFAULT 0 COMMENT '1 = closed by 9PM cron job',
    google_chat_sent    TINYINT(1)      DEFAULT 0 COMMENT 'Notification sent flag',
    created_at          DATETIME        DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_checkin_branch   FOREIGN KEY (branch_id)   REFERENCES pv_branches(id),
    CONSTRAINT fk_checkin_customer FOREIGN KEY (customer_id) REFERENCES pv_customers(id),
    CONSTRAINT fk_checkin_staff    FOREIGN KEY (staff_id)    REFERENCES pv_staff(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX idx_checkin_customer    ON pv_checkins(customer_id);
CREATE INDEX idx_checkin_branch_date ON pv_checkins(branch_id, checkin_time);
CREATE INDEX idx_checkin_status      ON pv_checkins(status);

-- ============================================================
-- 7. CHECK-IN KIDS (junction)
--    Which kids were present in a specific check-in.
--    Enables per-kid session deduction tracking.
--    Branch isolation inherited via pv_checkins.branch_id.
-- ============================================================
CREATE TABLE pv_checkin_kids (
    id              INT UNSIGNED    AUTO_INCREMENT PRIMARY KEY,
    checkin_id      INT UNSIGNED    NOT NULL,
    kid_id          INT UNSIGNED    NOT NULL,
    session_used    TINYINT(1)      DEFAULT 0 COMMENT '1 = session deducted for this kid at checkout',

    CONSTRAINT fk_ck_checkin FOREIGN KEY (checkin_id) REFERENCES pv_checkins(id) ON DELETE CASCADE,
    CONSTRAINT fk_ck_kid     FOREIGN KEY (kid_id)     REFERENCES pv_kids(id)     ON DELETE RESTRICT,

    UNIQUE KEY uq_checkin_kid (checkin_id, kid_id)

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Reverse lookup: all check-ins for a given kid (history, reports)
CREATE INDEX idx_ck_kid      ON pv_checkin_kids(kid_id);

-- Fast deduction audit: find all undeducted kids in a checkin
CREATE INDEX idx_ck_session  ON pv_checkin_kids(checkin_id, session_used);

-- ============================================================
-- 8. PURCHASES / RECHARGES
--    Every time a customer buys or upgrades a package.
--    branch_id drives isolation — staff see only their branch.
-- ============================================================
CREATE TABLE pv_purchases (
    id                      INT UNSIGNED    AUTO_INCREMENT PRIMARY KEY,
    customer_id             INT UNSIGNED    NOT NULL,
    branch_id               INT UNSIGNED    NOT NULL COMMENT 'Branch where purchase happened – isolation key',
    staff_id                INT UNSIGNED    DEFAULT NULL COMMENT 'NULL if imported/system-generated',
    package_id              INT UNSIGNED    NOT NULL,

    -- Snapshot at time of purchase (in case package prices change later)
    sessions_added          INT             NOT NULL COMMENT 'Total sessions credited (purchased + bonus)',
    amount_paid             DECIMAL(8,2)    NOT NULL,
    gst_amount              DECIMAL(8,2)    DEFAULT 0.00,
    discount_applied        DECIMAL(8,2)    DEFAULT 0.00 COMMENT 'Recharge discount used',

    -- Balance tracking
    balance_before          INT             NOT NULL COMMENT 'global_session_balance before this purchase',
    balance_after           INT             NOT NULL COMMENT 'global_session_balance after this purchase',

    -- Upgrade tracking
    is_upgrade              TINYINT(1)      DEFAULT 0,
    upgraded_from_package   INT UNSIGNED    DEFAULT NULL,

    -- Payment
    payment_mode            ENUM('Cash','UPI','Card','Online') DEFAULT 'Cash',
    payment_reference       VARCHAR(100)    DEFAULT NULL COMMENT 'UPI txn ID, card last4, etc.',
    notes                   TEXT,

    created_at              DATETIME        DEFAULT CURRENT_TIMESTAMP,
    updated_at              DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_purchase_customer FOREIGN KEY (customer_id)           REFERENCES pv_customers(id)  ON DELETE RESTRICT,
    CONSTRAINT fk_purchase_branch   FOREIGN KEY (branch_id)             REFERENCES pv_branches(id)   ON DELETE RESTRICT,
    CONSTRAINT fk_purchase_staff    FOREIGN KEY (staff_id)              REFERENCES pv_staff(id)      ON DELETE SET NULL,
    CONSTRAINT fk_purchase_package  FOREIGN KEY (package_id)            REFERENCES pv_packages(id)   ON DELETE RESTRICT,
    CONSTRAINT fk_purchase_upgfrom  FOREIGN KEY (upgraded_from_package) REFERENCES pv_packages(id)   ON DELETE SET NULL

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Branch-scoped purchase list (primary dashboard query)
CREATE INDEX idx_purchase_branch_date     ON pv_purchases(branch_id, created_at);

-- Customer purchase history (global — cross-branch)
CREATE INDEX idx_purchase_customer        ON pv_purchases(customer_id);

-- Customer history scoped to a branch (most frequent app query)
CREATE INDEX idx_purchase_customer_branch ON pv_purchases(customer_id, branch_id);

-- Upgrade audit queries
CREATE INDEX idx_purchase_upgrade         ON pv_purchases(is_upgrade, branch_id);

-- ============================================================
-- 9. SESSION DEDUCTION LOG
--    Immutable audit trail of every session deduction.
--    Source = checkout or auto_close.
--    branch_id drives isolation — all FKs are RESTRICT.
-- ============================================================
CREATE TABLE pv_session_deductions (
    id                  INT UNSIGNED    AUTO_INCREMENT PRIMARY KEY,
    customer_id         INT UNSIGNED    NOT NULL,
    checkin_id          INT UNSIGNED    NOT NULL,
    kid_id              INT UNSIGNED    NOT NULL,
    branch_id           INT UNSIGNED    NOT NULL              COMMENT 'Branch where deduction occurred – isolation key',
    staff_id            INT UNSIGNED    DEFAULT NULL          COMMENT 'Staff who triggered checkout; NULL = auto-close',
    deducted_at         DATETIME        DEFAULT CURRENT_TIMESTAMP,
    deduction_source    ENUM('Checkout','Auto-Close')         DEFAULT 'Checkout',
    balance_before      INT             NOT NULL              COMMENT 'global_session_balance before deduction',
    balance_after       INT             NOT NULL              COMMENT 'global_session_balance after deduction',

    CONSTRAINT fk_deduct_customer FOREIGN KEY (customer_id) REFERENCES pv_customers(id) ON DELETE RESTRICT,
    CONSTRAINT fk_deduct_checkin  FOREIGN KEY (checkin_id)  REFERENCES pv_checkins(id)  ON DELETE RESTRICT,
    CONSTRAINT fk_deduct_kid      FOREIGN KEY (kid_id)      REFERENCES pv_kids(id)      ON DELETE RESTRICT,
    CONSTRAINT fk_deduct_branch   FOREIGN KEY (branch_id)   REFERENCES pv_branches(id)  ON DELETE RESTRICT,
    CONSTRAINT fk_deduct_staff    FOREIGN KEY (staff_id)    REFERENCES pv_staff(id)     ON DELETE SET NULL

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Branch daily deduction report (primary isolation query)
CREATE INDEX idx_deduct_branch_date       ON pv_session_deductions(branch_id, deducted_at);

-- Customer full deduction history (cross-branch)
CREATE INDEX idx_deduct_customer          ON pv_session_deductions(customer_id);

-- Customer deduction history scoped to branch
CREATE INDEX idx_deduct_customer_branch   ON pv_session_deductions(customer_id, branch_id);

-- Per-kid session history (kid profile page)
CREATE INDEX idx_deduct_kid               ON pv_session_deductions(kid_id);

-- Checkin-level audit (how many deductions per checkin)
CREATE INDEX idx_deduct_checkin           ON pv_session_deductions(checkin_id);

-- ============================================================
-- 10. BIRTHDAY PARTY BOOKINGS
--     Separate booking flow with 2.5-hour slot, food boxes,
--     gourmet cake, and discount from customer's pack.
--     branch_id drives isolation.
-- ============================================================
CREATE TABLE pv_birthday_bookings (
    id                  INT UNSIGNED    AUTO_INCREMENT PRIMARY KEY,
    branch_id           INT UNSIGNED    NOT NULL                    COMMENT 'Branch where party is hosted – isolation key',
    customer_id         INT UNSIGNED    NOT NULL,
    kid_id              INT UNSIGNED    NOT NULL                    COMMENT 'The birthday kid',
    staff_id            INT UNSIGNED    DEFAULT NULL                COMMENT 'Staff who handled the booking',

    -- Slot
    party_date          DATE            NOT NULL,
    party_slot_start    TIME            NOT NULL                    COMMENT 'Mon-Fri 11AM-8PM, Sat-Sun 11AM-1:30PM',
    party_slot_end      TIME            NOT NULL                    COMMENT 'party_slot_start + 2.5 hours',
    expected_guests     INT             DEFAULT 10,

    -- Party details
    cake_option         VARCHAR(100)    DEFAULT NULL,
    food_boxes_count    INT             DEFAULT 0,

    -- Financials
    base_amount         DECIMAL(8,2)    DEFAULT 0.00,
    discount_pct        DECIMAL(5,2)    DEFAULT 0.00               COMMENT 'From customer pack benefits',
    discount_amount     DECIMAL(8,2)    DEFAULT 0.00,
    gst_amount          DECIMAL(8,2)    DEFAULT 0.00,
    total_amount        DECIMAL(8,2)    DEFAULT 0.00,
    advance_paid        DECIMAL(8,2)    DEFAULT 0.00,
    -- balance_due removed: derive as (total_amount - advance_paid) in query

    -- Payment
    payment_mode        ENUM('Cash','UPI','Card','Online')          DEFAULT 'Cash',
    payment_reference   VARCHAR(100)    DEFAULT NULL                COMMENT 'UPI txn ID, card last4 for advance payment',

    -- Status & notes
    status              ENUM('Enquiry','Confirmed','Completed','Cancelled') DEFAULT 'Enquiry',
    notes               TEXT,

    created_at          DATETIME        DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    -- Slot conflict guard: same branch can't have two bookings at exact same start time
    UNIQUE KEY uq_bday_slot (branch_id, party_date, party_slot_start),

    CONSTRAINT fk_bday_branch   FOREIGN KEY (branch_id)   REFERENCES pv_branches(id)   ON DELETE RESTRICT,
    CONSTRAINT fk_bday_customer FOREIGN KEY (customer_id) REFERENCES pv_customers(id)  ON DELETE RESTRICT,
    CONSTRAINT fk_bday_kid      FOREIGN KEY (kid_id)      REFERENCES pv_kids(id)       ON DELETE RESTRICT,
    CONSTRAINT fk_bday_staff    FOREIGN KEY (staff_id)    REFERENCES pv_staff(id)      ON DELETE SET NULL

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Branch calendar view (primary query)
CREATE INDEX idx_bday_branch_date       ON pv_birthday_bookings(branch_id, party_date);

-- Upcoming confirmed bookings per branch
CREATE INDEX idx_bday_status_branch     ON pv_birthday_bookings(status, branch_id);

-- Customer booking history (cross-branch)
CREATE INDEX idx_bday_customer          ON pv_birthday_bookings(customer_id);

-- Customer bookings scoped to branch
CREATE INDEX idx_bday_customer_branch   ON pv_birthday_bookings(customer_id, branch_id);

-- All parties for a specific kid
CREATE INDEX idx_bday_kid               ON pv_birthday_bookings(kid_id);

-- ============================================================
-- 11. SCHOOL FIELD TRIP BOOKINGS
--     Group bookings from schools. Different pricing model.
--     branch_id drives isolation.
-- ============================================================
CREATE TABLE pv_school_trips (
    id                  INT UNSIGNED    AUTO_INCREMENT PRIMARY KEY,
    branch_id           INT UNSIGNED    NOT NULL                    COMMENT 'Branch hosting the trip – isolation key',
    staff_id            INT UNSIGNED    DEFAULT NULL                COMMENT 'Staff who handled the booking',

    -- School details
    school_name         VARCHAR(200)    NOT NULL,
    contact_person      VARCHAR(150)    DEFAULT NULL,
    contact_phone       VARCHAR(15)     DEFAULT NULL,
    contact_email       VARCHAR(150)    DEFAULT NULL,

    -- Trip slot
    trip_date           DATE            NOT NULL,
    slot_start          TIME            NOT NULL                    COMMENT 'Trip start time',
    slot_end            TIME            NOT NULL                    COMMENT 'Trip end time',

    -- Attendance
    expected_kids       INT             DEFAULT 0,
    actual_kids         INT             DEFAULT 0                   COMMENT 'Filled at Completed status',

    -- Financials
    price_per_kid       DECIMAL(8,2)    DEFAULT 0.00,
    total_amount        DECIMAL(8,2)    DEFAULT 0.00               COMMENT 'Stored explicitly – may differ from actual_kids * price_per_kid due to negotiation',
    gst_amount          DECIMAL(8,2)    DEFAULT 0.00,
    advance_paid        DECIMAL(8,2)    DEFAULT 0.00,
    -- balance_due removed: derive as (total_amount - advance_paid) in query

    -- Payment
    payment_mode        ENUM('Cash','UPI','Card','Cheque','Online') DEFAULT 'Online',
    payment_reference   VARCHAR(100)    DEFAULT NULL                COMMENT 'Cheque no., UPI txn ID, bank ref, etc.',

    -- Status
    status              ENUM('Enquiry','Confirmed','Completed','Cancelled') DEFAULT 'Enquiry',
    notes               TEXT,

    created_at          DATETIME        DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    -- Slot conflict guard
    UNIQUE KEY uq_trip_slot (branch_id, trip_date, slot_start),

    CONSTRAINT fk_trip_branch FOREIGN KEY (branch_id) REFERENCES pv_branches(id) ON DELETE RESTRICT,
    CONSTRAINT fk_trip_staff  FOREIGN KEY (staff_id)  REFERENCES pv_staff(id)   ON DELETE SET NULL

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Branch calendar view (primary query)
CREATE INDEX idx_trip_branch_date    ON pv_school_trips(branch_id, trip_date);

-- Upcoming confirmed trips per branch
CREATE INDEX idx_trip_status_branch  ON pv_school_trips(status, branch_id);

-- Global admin calendar across all branches
CREATE INDEX idx_trip_date           ON pv_school_trips(trip_date);

-- ============================================================
-- 12. INTER-BRANCH SETTLEMENTS
--     When a customer purchased pack at branch A but uses
--     sessions at branch B. settlement_rate from pv_branches.
--     Both purchase_branch_id and usage_branch_id serve as
--     isolation keys depending on query perspective.
-- ============================================================
CREATE TABLE pv_inter_branch_settlements (
    id                      INT UNSIGNED    AUTO_INCREMENT PRIMARY KEY,
    customer_id             INT UNSIGNED    NOT NULL,
    checkin_id              INT UNSIGNED    NOT NULL,

    -- Branch parties involved
    purchase_branch_id      INT UNSIGNED    NOT NULL    COMMENT 'Branch where pack was bought – is owed money',
    usage_branch_id         INT UNSIGNED    NOT NULL    COMMENT 'Branch where session was used – owes money',

    -- Settlement financials
    sessions_used           INT             DEFAULT 1,
    settlement_rate         DECIMAL(5,2)    NOT NULL    COMMENT 'Snapshot of pv_branches.settlement_rate at time of use',
    settlement_amount       DECIMAL(8,2)    DEFAULT 0.00,

    -- Settlement status
    settled                 TINYINT(1)      DEFAULT 0,
    settled_at              DATETIME        DEFAULT NULL,
    settled_by_staff_id     INT UNSIGNED    DEFAULT NULL COMMENT 'Admin/manager who marked as settled',
    notes                   TEXT            DEFAULT NULL COMMENT 'Settlement remarks if any',

    created_at              DATETIME        DEFAULT CURRENT_TIMESTAMP,
    updated_at              DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    -- One checkin = one settlement record
    UNIQUE KEY uq_settle_checkin (checkin_id),

    CONSTRAINT fk_settle_customer  FOREIGN KEY (customer_id)        REFERENCES pv_customers(id)  ON DELETE RESTRICT,
    CONSTRAINT fk_settle_checkin   FOREIGN KEY (checkin_id)         REFERENCES pv_checkins(id)   ON DELETE RESTRICT,
    CONSTRAINT fk_settle_purchase  FOREIGN KEY (purchase_branch_id) REFERENCES pv_branches(id)   ON DELETE RESTRICT,
    CONSTRAINT fk_settle_usage     FOREIGN KEY (usage_branch_id)    REFERENCES pv_branches(id)   ON DELETE RESTRICT,
    CONSTRAINT fk_settle_staff     FOREIGN KEY (settled_by_staff_id) REFERENCES pv_staff(id)     ON DELETE SET NULL

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Purchase branch: what am I owed? (receivables view)
CREATE INDEX idx_settle_purchase_pending  ON pv_inter_branch_settlements(purchase_branch_id, settled);

-- Usage branch: what do I owe? (payables view)
CREATE INDEX idx_settle_usage_pending     ON pv_inter_branch_settlements(usage_branch_id, settled);

-- Customer cross-branch history
CREATE INDEX idx_settle_customer          ON pv_inter_branch_settlements(customer_id);

-- Settlement history report by date
CREATE INDEX idx_settle_purchase_date     ON pv_inter_branch_settlements(purchase_branch_id, settled_at);

-- Batch settlement processing
CREATE INDEX idx_settle_pending_usage     ON pv_inter_branch_settlements(settled, usage_branch_id);

-- ============================================================
-- 13. EXPENSE TRACKING
--     Branch-level daily operational expenses.
--     Maps to existing pv_add_expense function.
--     branch_id drives isolation.
-- ============================================================
CREATE TABLE pv_expenses (
    id                      INT UNSIGNED    AUTO_INCREMENT PRIMARY KEY,
    branch_id               INT UNSIGNED    NOT NULL                COMMENT 'Branch that incurred the expense – isolation key',
    staff_id                INT UNSIGNED    DEFAULT NULL            COMMENT 'Staff who logged the expense',
    approved_by_staff_id    INT UNSIGNED    DEFAULT NULL            COMMENT 'Manager who approved the expense',

    -- Expense details
    expense_date            DATE            NOT NULL,
    category                ENUM(
                                'Salary',
                                'Rent',
                                'Utilities',
                                'Supplies',
                                'Maintenance',
                                'Marketing',
                                'Food & Beverage',
                                'Miscellaneous'
                            )               NOT NULL,
    description             VARCHAR(255)    NOT NULL,
    is_recurring            TINYINT(1)      DEFAULT 0              COMMENT '1 = monthly recurring (Salary, Rent etc.)',

    -- Financials
    amount                  DECIMAL(10,2)   NOT NULL               COMMENT 'Base expense amount before GST',
    gst_amount              DECIMAL(10,2)   DEFAULT 0.00           COMMENT 'GST on expense if applicable',
    total_amount            DECIMAL(10,2)   DEFAULT 0.00           COMMENT 'amount + gst_amount',

    -- Payment
    payment_mode            ENUM('Cash','UPI','Card','Online')      DEFAULT 'Cash',
    receipt_ref             VARCHAR(100)    DEFAULT NULL            COMMENT 'Receipt number, invoice ref, etc.',

    notes                   TEXT,

    created_at              DATETIME        DEFAULT CURRENT_TIMESTAMP,
    updated_at              DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_expense_branch    FOREIGN KEY (branch_id)            REFERENCES pv_branches(id) ON DELETE RESTRICT,
    CONSTRAINT fk_expense_staff     FOREIGN KEY (staff_id)             REFERENCES pv_staff(id)    ON DELETE SET NULL,
    CONSTRAINT fk_expense_approver  FOREIGN KEY (approved_by_staff_id) REFERENCES pv_staff(id)    ON DELETE SET NULL

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Primary: branch daily expense list
CREATE INDEX idx_expense_branch_date      ON pv_expenses(branch_id, expense_date);

-- Category breakdown per branch (P&L reports)
CREATE INDEX idx_expense_branch_category  ON pv_expenses(branch_id, category);

-- Monthly P&L by category per branch (composite covering index)
CREATE INDEX idx_expense_branch_date_cat  ON pv_expenses(branch_id, expense_date, category);

-- Global admin view across all branches
CREATE INDEX idx_expense_date             ON pv_expenses(expense_date);

-- ============================================================
-- 14. GOOGLE CHAT NOTIFICATION LOG
--     Tracks every webhook message sent for audit/debug.
--     branch_id nullable — NULL = system-level notification.
-- ============================================================
CREATE TABLE pv_notification_log (
    id              INT UNSIGNED    AUTO_INCREMENT PRIMARY KEY,
    branch_id       INT UNSIGNED    DEFAULT NULL                COMMENT 'NULL = system-level / not branch-specific',
    staff_id        INT UNSIGNED    DEFAULT NULL                COMMENT 'Staff who triggered; NULL = system/cron',
    customer_id     INT UNSIGNED    DEFAULT NULL,

    -- Event reference
    event_type      ENUM(
                        'checkin',
                        'checkout',
                        'auto_close',
                        'purchase',
                        'birthday_booking',
                        'low_balance',
                        'zero_balance',
                        'expense',
                        'other'
                    )               NOT NULL,
    reference_id    INT UNSIGNED    DEFAULT NULL                COMMENT 'checkin_id / purchase_id / expense_id etc.',
    reference_type  VARCHAR(50)     DEFAULT NULL                COMMENT 'Source table name for reference_id',

    -- Webhook payload
    message_preview VARCHAR(500)    DEFAULT NULL                COMMENT 'First 500 chars of the message sent',
    webhook_url     VARCHAR(500)    NOT NULL                    COMMENT 'Google Chat webhook URL used',

    -- Delivery status
    http_status     SMALLINT        DEFAULT NULL                COMMENT '200 = success, NULL = not yet sent',
    error_message   TEXT            DEFAULT NULL                COMMENT 'Webhook error response body if http_status != 200',
    retry_count     TINYINT         DEFAULT 0                   COMMENT 'Number of retry attempts made',
    retry_status    ENUM(
                        'Pending',
                        'Success',
                        'Failed',
                        'Skipped'
                    )               DEFAULT 'Pending'           COMMENT 'Current delivery state',

    sent_at         DATETIME        DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_notif_branch   FOREIGN KEY (branch_id)   REFERENCES pv_branches(id)   ON DELETE SET NULL,
    CONSTRAINT fk_notif_customer FOREIGN KEY (customer_id) REFERENCES pv_customers(id)  ON DELETE SET NULL,
    CONSTRAINT fk_notif_staff    FOREIGN KEY (staff_id)    REFERENCES pv_staff(id)      ON DELETE SET NULL

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Branch notification history (primary isolation query)
CREATE INDEX idx_notif_branch_date      ON pv_notification_log(branch_id, sent_at);

-- Event type breakdown per branch
CREATE INDEX idx_notif_event_branch     ON pv_notification_log(event_type, branch_id);

-- Failed notifications per branch (debug dashboard)
CREATE INDEX idx_notif_status_branch    ON pv_notification_log(http_status, branch_id);

-- Retry queue — pending/failed delivery processing
CREATE INDEX idx_notif_retry_status     ON pv_notification_log(retry_status);

-- Customer notification history
CREATE INDEX idx_notif_customer         ON pv_notification_log(customer_id);

-- ============================================================
-- 15. AUTO-CLOSE CRON LOG
--     Audit trail for every 9 PM cron job execution.
--     branch_id nullable — NULL = ran across all branches.
-- ============================================================
CREATE TABLE pv_cron_log (
    id                  INT UNSIGNED    AUTO_INCREMENT PRIMARY KEY,
    branch_id           INT UNSIGNED    DEFAULT NULL                COMMENT 'NULL = all branches processed in this run',

    -- Timing
    run_at              DATETIME        DEFAULT CURRENT_TIMESTAMP   COMMENT 'When cron job started',
    run_end_at          DATETIME        DEFAULT NULL                COMMENT 'When cron job finished',
    duration_seconds    INT             DEFAULT NULL                COMMENT 'Elapsed time in seconds',

    -- Results
    checkins_closed     INT             DEFAULT 0                   COMMENT 'Number of active checkins auto-closed',
    sessions_deducted   INT             DEFAULT 0                   COMMENT 'Total sessions deducted across all closed checkins',
    notifications_sent  INT             DEFAULT 0                   COMMENT 'Google Chat notifications dispatched',

    -- Errors
    error_count         INT             DEFAULT 0                   COMMENT 'Number of errors encountered',
    errors              TEXT            DEFAULT NULL                COMMENT 'JSON array or newline-separated error details',

    -- Status
    status              ENUM(
                            'Success',
                            'Partial',
                            'Failed'
                        )               DEFAULT 'Success',
    triggered_by        ENUM(
                            'Scheduler',
                            'Manual'
                        )               DEFAULT 'Scheduler'         COMMENT 'How this cron run was initiated',

    CONSTRAINT fk_cron_branch FOREIGN KEY (branch_id) REFERENCES pv_branches(id) ON DELETE SET NULL

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Branch-specific cron history
CREATE INDEX idx_cron_branch_date   ON pv_cron_log(branch_id, run_at);

-- Failed/partial runs for alerting and monitoring
CREATE INDEX idx_cron_status_date   ON pv_cron_log(status, run_at);

-- Global cron timeline (admin view)
CREATE INDEX idx_cron_run_at        ON pv_cron_log(run_at);

-- ============================================================
-- 16. CSV IMPORT LOG
--     Tracks bulk customer imports with upsert results.
--     branch_id drives isolation — every import belongs to
--     a specific branch.
-- ============================================================
CREATE TABLE pv_import_log (
    id              INT UNSIGNED    AUTO_INCREMENT PRIMARY KEY,
    branch_id       INT UNSIGNED    NOT NULL                    COMMENT 'Branch that ran the import – isolation key',
    staff_id        INT UNSIGNED    DEFAULT NULL                COMMENT 'Staff who uploaded the CSV',

    -- Import details
    import_type     ENUM(
                        'Customers',
                        'Purchases',
                        'Other'
                    )               DEFAULT 'Customers',
    file_name       VARCHAR(255)    NOT NULL                    COMMENT 'Original uploaded CSV filename',

    -- Timing
    imported_at     DATETIME        DEFAULT CURRENT_TIMESTAMP   COMMENT 'When import job started',
    completed_at    DATETIME        DEFAULT NULL                COMMENT 'When import job finished',
    duration_seconds INT            DEFAULT NULL                COMMENT 'Processing time in seconds',

    -- Row-level results
    total_rows      INT             DEFAULT 0                   COMMENT 'Total rows in uploaded CSV',
    rows_inserted   INT             DEFAULT 0                   COMMENT 'New records created',
    rows_updated    INT             DEFAULT 0                   COMMENT 'Existing records updated',
    rows_skipped    INT             DEFAULT 0                   COMMENT 'Valid rows skipped – duplicate or no change',
    rows_failed     INT             DEFAULT 0                   COMMENT 'Rows that failed validation or insert',

    -- Status & errors
    import_status   ENUM(
                        'Processing',
                        'Completed',
                        'Partial',
                        'Failed'
                    )               DEFAULT 'Processing'        COMMENT 'Overall job status',
    error_details   TEXT            DEFAULT NULL                COMMENT 'JSON array: [{row, reason}] for failed rows',

    updated_at      DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_import_branch FOREIGN KEY (branch_id) REFERENCES pv_branches(id) ON DELETE RESTRICT,
    CONSTRAINT fk_import_staff  FOREIGN KEY (staff_id)  REFERENCES pv_staff(id)    ON DELETE SET NULL

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Branch import history (primary isolation query)
CREATE INDEX idx_import_branch_date     ON pv_import_log(branch_id, imported_at);

-- Filter by import type per branch
CREATE INDEX idx_import_type_branch     ON pv_import_log(import_type, branch_id);

-- Find failed or in-progress imports per branch
CREATE INDEX idx_import_status_branch   ON pv_import_log(import_status, branch_id);

SET FOREIGN_KEY_CHECKS = 1;
-- ============================================================
-- END OF MIGRATION V1
-- ============================================================

-- ============================================================
-- Alter commands -------
-- ============================================================



ALTER TABLE pv_customers
    ADD COLUMN home_branch_id INT UNSIGNED NOT NULL
        COMMENT 'Branch where customer was first registered – primary isolation key'
        AFTER phone_number,
    ADD CONSTRAINT fk_customer_homebranch
        FOREIGN KEY (home_branch_id) REFERENCES pv_branches(id);

CREATE INDEX idx_customer_home_branch ON pv_customers(home_branch_id);