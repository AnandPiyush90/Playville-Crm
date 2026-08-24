# Reporting Module Plan

This document is the product and technical specification for a PlayVille CRM
reporting module. It is derived from the current backend domain model (entities,
enums, services, and existing `/reports` APIs). Implementation is deferred; this
file is the backlog and design contract.

Related documents:

- [Invoice and Inventory Management Architecture](invoice-inventory-architecture.md) §10 (sales, tax, stock, margin)
- [Invoice / Inventory Angular UI Plan](invoice-inventory-angular-ui-plan.md) (`/reports/sales`, `/reports/inventory`)
- [Trial Conversion Implementation Plan](trial-conversion-implementation-plan.md) §4.5
- [Customer Onboarding / Check-in Guide](customer-onboarding-checkin-checkout-guide.md)
- [Inventory Management Guide](inventory-management-guide.md)
- [Birthday Booking Guide](birthday-booking-guide.md)

---

## 1. Objective

Give **Admin** (org-wide) and **Centre Manager** (own branch) read-only reports
that answer operational, commercial, and compliance questions from data the CRM
already stores.

The module should:

1. Reuse existing tables; do not invent parallel “report facts” unless a query is
   too expensive or a field is missing (see §8).
2. Stay inside the Spring Boot modular monolith as query services + REST under
   `/reports`.
3. Enforce branch scoping and role-based field hiding (especially cost and margin).
4. Bound every list/aggregate by date range and pagination.
5. Make money reports reconcile to invoices where invoices exist, and say clearly
   when a figure still comes from legacy `pv_purchases`.

Front-desk `staff` must not access reports. Audit search already exists at
`/audit-logs` and is not duplicated here.

---

## 2. Audience and access

| Role | Scope | Notes |
|---|---|---|
| `admin` | All branches, or a selected `branchId` | Cross-branch comparison and finance views |
| `manager` | JWT home branch only | Daily ops, sales mix, stock, funnel |
| `staff` | None | Floor work only |

`SecurityConfig` already restricts `/reports/**` to `admin` and `manager`.

**Branch scoping today:** `TrialReportingService` forces the JWT branch and rejects
a different `branchId`. That is correct for managers and **must be relaxed for
admin** when the module is built (optional `branchId`, or omit for all-branch
aggregates).

**Sensitive columns:** cost price, stock valuation, and gross margin must not be
exposed to `staff`. Prefer **admin-only** for margin/valuation; managers may see
quantity, low stock, and expiry without unit cost. This matches inventory
architecture guidance.

---

## 3. What exists today

| Item | Status |
|---|---|
| `GET /reports/trial-funnel` | Implemented (`TrialReportController` + `TrialReportingService`) |
| `GET /reports/trial-conversions` | Implemented (paged detail) |
| `GET /audit-logs` | Implemented (not a KPI report) |
| Sales / inventory / occupancy reports | Not implemented |
| `/expenses/**` security matcher | Present; **no expense entity** |

Default trial window: last 30 days. Filters: `from`, `to`, `branchId`, `staffId`,
`leadSource`, `campaignCode`. Feature flag: trial conversion flow.

---

## 4. Domain inventory (reportable facts)

Roles: `StaffRole` = `admin` | `manager` | `staff`.

### 4.1 Customers and kids

- Customer: home / purchase / first-visit branch, lead source, marketing consent,
  global session balance, current package, total visits, disclaimer flags.
- Kid: DOB, gender, branch, active flag. Age ≤ 8 is eligibility logic, not a stored
  field.

Lead sources: `Walk_in`, `Google`, `Instagram`, `Facebook`, `Friend_Referral`,
`School`, `Other`.

### 4.2 Enquiries

Statuses: `NEW`, `CONTACTED`, `VISIT_SCHEDULED`, `CONVERTED_TO_CUSTOMER`, `LOST`.

Fields: branch, converted customer, lead source, visit scheduled at, child name/DOB.

### 4.3 Trials and entitlements

Types: `COMPLIMENTARY_TRIAL`, `MANUAL_COMPLIMENTARY`.

Statuses: `ACTIVE`, `RESERVED`, `CONSUMED`, `EXPIRED`, `CANCELLED`.

Check-in conversion: outcome `PURCHASED` | `FOLLOW_UP_REQUIRED` | `DECLINED` |
`NOT_OFFERED`; reason `PRICE` | `NEEDS_TIME` | `CHILD_NOT_INTERESTED` |
`WILL_RETURN` | `OTHER`; `follow_up_at`; linked purchase.

Purchase context: `STANDARD` | `TRIAL_CHECKOUT`.

### 4.4 Check-in and sessions

Visit type: `PAID` | `COMPLIMENTARY_TRIAL`.

Check-in status: `Active`, `Completed`, `Auto_Closed`, `Cancelled`.

Also: kids count, session deductions (Checkout vs Auto_Close), extra/GST/total
charged, in-session retail lines (`OPEN` / `CHARGED` / `CANCELLED`).

Branch hours: `open_time`, `close_time`, `closed_day` (heatmap vs operating hours).
There is **no capacity** field, so occupancy % cannot be computed without a later
schema change.

### 4.5 Membership purchases (legacy commercial record)

Package, sessions added, amount, GST, discount, payment mode (`Cash`, `UPI`,
`Card`, `Online`), upgrade flag, staff, source check-in / trial entitlement.

Package `validity_days` exists; **purchase row has no stored expiry timestamp**.

### 4.6 Invoices and payments (financial source of truth)

Invoice status: `DRAFT`, `UNPAID`, `ISSUED`, `PARTIALLY_PAID`, `PAID`, `VOIDED`,
`PARTIALLY_REFUNDED`, `REFUNDED`.

`invoice_type` includes at least `SALE` and `BIRTHDAY_BOOKING`.

Line types: `RETAIL`, `MEMBERSHIP`, `SERVICE`, `CHECKOUT_CHARGE`.

Payments: mode `CASH` | `UPI` | `CARD` | `ONLINE`, `paid_at`, received-by staff,
`payment_type` (payment vs refund).

Returns: `pv_sales_returns` (original + credit invoice).

**Reporting rule:** exclude `DRAFT` and usually `VOIDED` from revenue. Use
`invoice_date` / `paid_at` consistently and document which clock each report uses.

### 4.7 Birthdays

Statuses: `Enquiry`, `Confirmed`, `Completed`, `Cancelled`.

Party date/slot, expected guests, actual kids/adults, extra minutes, amounts
(base, discount, GST, total, advance), quote lines (category, SKU), inventory
reservation `RESERVED` | `RELEASED` | `CONSUMED`, linked invoice.

### 4.8 School trips

Same booking statuses. School name, trip date/slot, expected vs actual kids,
price per kid, totals, GST, advance, payment mode.

### 4.9 Inventory

Balances: on-hand, reserved, available.

Branch SKU: reorder level/qty, negative-stock policy, price override.

Batches: received at, expires on, unit cost, qty remaining.

Movements: `OPENING_STOCK`, `RECEIPT`, `SALE`, `SALE_RETURN`, `TRANSFER_OUT`,
`TRANSFER_IN`, `ADJUSTMENT_IN`, `ADJUSTMENT_OUT`, `WASTAGE`, `EXPIRY`.

### 4.10 Inter-branch

`pv_inter_branch_settlements`: purchase branch vs usage branch, sessions used,
settlement rate/amount, settled flag.

### 4.11 Compliance and notifications

Disclaimer acceptances and signing requests (channel, status, expiry, signed at).

Notification deliveries: channel, purpose, status, attempts, errors (invoice,
disclaimer, and similar).

### 4.12 Audit and jobs

`pv_audit_logs` (existing API). `pv_cron_logs` for auto-close job health.

---

## 5. Report catalogue

Each row is a report that **can be built from current tables** unless §8 says
otherwise. Suggested API names are stable identifiers for later implementation,
not a commitment to ship all at once.

Shared query params unless noted: `from`, `to`, `branchId` (admin), `page`/`size`
for lists. Default range: last 30 days; reject inverted ranges; cap max span
(recommend 93 days for detail, 366 for aggregates).

### 5.1 Centre operations (manager daily)

| ID | Report | Insight | Primary sources | Extra filters |
|---|---|---|---|---|
| OPS-01 | Live occupancy / floor snapshot | Who is in-centre now, kids on floor | `pv_checkins` (`Active`) + `pv_checkin_kids` | staffId |
| OPS-02 | Daily visit summary | Check-ins, kids, avg duration, completed vs auto-closed vs cancelled | `pv_checkins` | visitType, status, staffId |
| OPS-03 | Hourly / day-of-week heatmap | Peak load vs opening hours | Check-in timestamps + `pv_branches` hours | visitType |
| OPS-04 | Session usage | Sessions deducted, checkout vs auto-close | `pv_session_deductions` | staffId, deductionSource |
| OPS-05 | In-session retail attach | Items added during visit; charged vs cancelled | `pv_checkin_session_items` | skuId, status |
| OPS-06 | Staff activity | Check-ins handled, invoices issued, payments taken | Check-in, invoice, payment staff FKs | staffId |

### 5.2 Revenue and collections

Invoices are the financial source of truth. Membership mix and trial attribution
may still join `pv_purchases` until every membership sale is invoice-backed.

| ID | Report | Insight | Primary sources | Extra filters |
|---|---|---|---|---|
| REV-01 | Daily sales (Z-report) | Gross, discount, tax, net, paid, outstanding | `pv_invoices` + `pv_payments` | invoiceType, staffId |
| REV-02 | Sales mix | Membership vs retail vs service vs checkout vs birthday | `pv_invoice_items.line_type`, `invoice_type` | lineType |
| REV-03 | Payment-mode reconciliation | Cash / UPI / Card / Online | `pv_payments` | paymentMode, staffId |
| REV-04 | Outstanding / ageing | Unpaid and partial, days outstanding | Invoice `balance_due`, `invoice_date`, status | status |
| REV-05 | Tax / GST summary | Taxable vs tax by snapshot rate | Invoice + line tax fields | — |
| REV-06 | Discounts given | Discount leakage by staff / package / day | Invoice `discount_total`, purchase `discount_applied` | staffId, packageId |
| REV-07 | Voids, returns, refunds | Corrections and leakage | Invoice status, `pv_sales_returns`, payment type | — |
| REV-08 | Membership sales | Packages sold, sessions added, upgrades, GST | `pv_purchases` + membership invoice lines | packageId, paymentMode, purchaseContext |
| REV-09 | Branch comparison | Same KPIs side by side | Aggregates of REV/OPS | **admin only** |

**Gap:** no cash-register / till session. REV-03 is “payments by staff and day”,
not a formal drawer close.

### 5.3 Membership health

| ID | Report | Insight | Primary sources | Extra filters |
|---|---|---|---|---|
| MEM-01 | Active session liability | Sessions still owed (unearned play) | `pv_customers.global_session_balance` | homeBranch, packageId |
| MEM-02 | Package mix | Families on each package | `current_package_id` + purchases | packageId |
| MEM-03 | Low-balance families | 0–2 sessions left | Session balance | threshold |
| MEM-04 | Dormant / at-risk | No visit in 14/30/60 days, optionally still have balance | Last `checkin_time` per customer | idleDays, minBalance |
| MEM-05 | Expiring packages | Validity window ending | `validity_days` + purchase `created_at` | windowDays |
| MEM-06 | Recharge and upgrade | Repeat buy vs first buy, upgrade path | `is_upgrade`, `upgraded_from_package` | packageId |
| MEM-07 | Sessions sold vs consumed | Burn rate | Purchases `sessions_added` vs deductions | — |

MEM-04 is derived (no `last_visit_at`). MEM-05 is approximate until expiry is
stored on the purchase (see §8).

### 5.4 CRM, trials, and conversion

| ID | Report | Insight | Primary sources | Extra filters |
|---|---|---|---|---|
| CRM-01 | Enquiry funnel | NEW → CONTACTED → VISIT_SCHEDULED → CONVERTED / LOST | `pv_enquiries` | leadSource, status |
| CRM-02 | Lead-source volume | Channel mix for enquiries and new customers | Enquiry + `Customer.lead_source` | leadSource |
| CRM-03 | Enquiry → customer → first purchase | End-to-end CRM conversion | `converted_customer_id` + first purchase/invoice | leadSource |
| CRM-04 | Trial funnel | Issued → check-in → complete → same-day / 7-day buy → expired / declined | Existing `TrialReportingService` | staffId, leadSource, campaignCode |
| CRM-05 | Trial conversion detail | Outcome, reason, campaign, follow-up | Existing paged API | same |
| CRM-06 | Follow-up overdue | `FOLLOW_UP_REQUIRED` and `follow_up_at` passed | `pv_checkins` | staffId |
| CRM-07 | Complimentary leakage | Manual complimentary vs first-visit trial | `EntitlementType` | type, status |

CRM-04 and CRM-05 already ship; extend admin branch scoping only.

Paid ads (Instagram / Facebook / Google), first-touch lock, phone/email matching,
and monthly CPA/ROAS are specified in
[Lead Management and Paid-Ads Attribution Plan](lead-management-module-plan.md)
(new reports ADS-01–ADS-03). Do not treat `Customer.lead_source` alone as ad
performance.

### 5.5 Birthdays

| ID | Report | Insight | Primary sources | Extra filters |
|---|---|---|---|---|
| BDAY-01 | Booking pipeline | Enquiry / Confirmed / Completed / Cancelled | `pv_birthday_bookings` | status |
| BDAY-02 | Upcoming calendar | Parties by date/slot, expected guests | Party date + slot | from/to as party dates |
| BDAY-03 | Birthday revenue | Quoted vs invoiced vs collected | Booking amounts + linked invoice | status |
| BDAY-04 | Upsell mix | Cake, food boxes, catalog categories | `pv_birthday_quote_lines.category` | category |
| BDAY-05 | Show-up vs quote | Expected vs actual kids/adults, extra minutes | Completion fields | — |
| BDAY-06 | Cancellation rate | Lost bookings and revenue | Status `Cancelled` | — |
| BDAY-07 | Party inventory | Reserved / released / consumed | `BirthdayInventoryReservation` | status |

### 5.6 School trips

| ID | Report | Insight | Primary sources | Extra filters |
|---|---|---|---|---|
| TRIP-01 | Trip pipeline and calendar | Status and schedule | `pv_school_trips` | status |
| TRIP-02 | Revenue and collection | Total, GST, advance vs outstanding | Amount fields | — |
| TRIP-03 | Yield | Expected vs actual kids, revenue per kid | `expected_kids` / `actual_kids` | — |
| TRIP-04 | School repeat | Which schools return | Group by `school_name` | — |

### 5.7 Inventory and margin

| ID | Report | Insight | Primary sources | Extra filters | Access |
|---|---|---|---|---|---|
| INV-01 | Stock on hand | On-hand, reserved, available | `pv_inventory_balances` | skuId, categoryId | manager, admin |
| INV-02 | Low / out / negative stock | Vs reorder level and negative-stock policy | Balances + `pv_branch_skus` | — | manager, admin |
| INV-03 | Batch expiry | 7 / 15 / 30 / 60 day windows | `pv_inventory_batches.expires_on` | windowDays | manager, admin |
| INV-04 | Movement ledger | Receipt, sale, return, transfer, wastage, expiry, adjustment | `pv_inventory_movements` | movementType, skuId | manager, admin |
| INV-05 | Wastage and expiry loss | Qty and (optionally) cost | Movements `WASTAGE`/`EXPIRY` × `unit_cost_snapshot` | — | qty: manager; cost: admin |
| INV-06 | Stock valuation | Qty remaining × batch cost | Batches | — | **admin** |
| INV-07 | Gross margin | Invoice line revenue vs batch cost snapshot | Invoice items + movement cost | lineType | **admin** |
| INV-08 | Transfers in flight | Dispatch vs receive | `pv_stock_transfers` | status | manager, admin |

These match the “initial reports” list in invoice-inventory architecture §10.

### 5.8 Multi-branch (admin primary)

| ID | Report | Insight | Primary sources | Extra filters |
|---|---|---|---|---|
| XBR-01 | Inter-branch session settlement | Usage branch owes purchase branch | `pv_inter_branch_settlements` | settled |
| XBR-02 | Customer travel | Home vs visit vs purchase branch | Customer FKs + check-ins | — |
| XBR-03 | Cross-branch stock transfers | What moved where | Transfers + movements | — |

Managers may see XBR-01/XBR-03 **only for their branch as purchase, usage, from,
or to**.

### 5.9 Compliance, communications, exceptions

| ID | Report | Insight | Primary sources | Extra filters |
|---|---|---|---|---|
| CMP-01 | Unsigned / stale disclaimer | Families at legal or check-in risk | Customer flags + acceptances vs active template | — |
| CMP-02 | Signing funnel | Created / sent / signed / expired | `pv_disclaimer_signing_requests` | channel, status |
| CMP-03 | Notification delivery | Email/WhatsApp success vs fail | `pv_notification_deliveries` | channel, purpose, status |
| CMP-04 | Auto-close exceptions | Visits closed by job, not staff | `auto_closed`, `pv_cron_logs` | — |

### 5.10 People and kids (marketing / ops)

| ID | Report | Insight | Primary sources | Extra filters |
|---|---|---|---|---|
| PPL-01 | New families | Onboarding volume | `pv_customers.created_at` | leadSource |
| PPL-02 | Age mix | Kids by age band | `pv_kids.dob` | — |
| PPL-03 | Upcoming kid birthdays | Party upsell list (next 7/30 days) | Kid DOB (not booking table) | windowDays |
| PPL-04 | Marketing consent list | Who may be contacted | `marketing_consent` | — |

PPL-04 is a contact list, not a broadcast tool. Respect consent and do not dump
full PII into generic dashboards; use paged export with audit.

---

## 6. Dashboard vs detail

Do not build 40 screens. Group the catalogue into **dashboards** (KPI cards +
charts) that drill into **detail reports** (paged tables, CSV later).

| Dashboard | Audience | Cards / widgets | Drill-down IDs |
|---|---|---|---|
| Centre today | Manager | Active check-ins, kids on floor, today’s sales, outstanding, low-stock count | OPS-01, OPS-02, REV-01, REV-04, INV-02 |
| Sales | Manager, admin | Net sales, mix, tender split, tax | REV-01–REV-07 |
| Membership | Manager, admin | Liability, low balance, dormant, upgrades | MEM-01–MEM-07 |
| Growth | Manager, admin | Enquiry funnel, trial conversion, lead source | CRM-01–CRM-07 |
| Events | Manager, admin | Upcoming birthdays/trips, pipeline, collection | BDAY-*, TRIP-* |
| Inventory | Manager, admin | Low stock, expiry, wastage | INV-01–INV-08 |
| Network | Admin | Branch comparison, settlements | REV-09, XBR-* |
| Compliance | Manager, admin | Unsigned disclaimers, failed notifications | CMP-* |

---

## 7. API and implementation conventions (when built)

Suggested layout:

```text
GET /reports/dashboard/centre-today
GET /reports/dashboard/sales
GET /reports/...

GET /reports/visits/summary
GET /reports/visits/occupancy
GET /reports/sales/daily
GET /reports/sales/mix
GET /reports/sales/payments
GET /reports/sales/outstanding
GET /reports/sales/tax
GET /reports/membership/...
GET /reports/enquiries/funnel
GET /reports/trial-funnel          (existing)
GET /reports/trial-conversions     (existing)
GET /reports/birthdays/...
GET /reports/school-trips/...
GET /reports/inventory/...
GET /reports/settlements/...
GET /reports/compliance/...
```

Keep existing trial paths so the Angular UI does not break.

Implementation notes:

1. Read-only Spring services; native SQL or JPQL aggregations; no writes.
2. Indexes already useful: check-in time, invoice date, entitlement expiry,
   inventory movements by branch/SKU/time. Add covering indexes if queries scan.
3. Do not aggregate inside invoice finalization (architecture §10).
4. Shared DTO envelope: period, branchId(s), generatedAt, plus `kpis` and/or
   `rows`.
5. CSV export can be a later `Accept` header or `/export` sibling; same auth.
6. Feature flags only if a report depends on an unreleased module (trial reports
   already require the trial conversion flag).

---

## 8. Data gaps (do not fake the number)

| Desired insight | Gap | Workaround now | Later schema |
|---|---|---|---|
| Package expiry list | No expiry timestamp on purchase | `created_at + validity_days` (null validity = unlimited) | Store `expires_at` on purchase/entitlement |
| Dormant customers | No `last_visit_at` | `MAX(checkin_time)` per customer | Denormalize on checkout |
| Occupancy % | No branch capacity | Headcount only | `max_kids` / `max_checkins` on branch |
| Till / shift close | No register session | Payments by staff + calendar day | Register session entity |
| P&L / net of opex | No expenses module | Revenue only | Expense entity (`/expenses/**` already reserved) |
| Staff productivity hours | No staff clock-in | Counts of actions, not hours | Attendance |
| NPS / incident quality | Not modelled | Skip | Feedback / incident entity |
| True first-visit vs returning | `total_visits` is a counter | Use check-in history | Keep as-is if counter is reliable |

Dual commercial history: older membership money lives on `pv_purchases`; newer
unified checkout lives on invoices. Reports must label the source or union with
deduplication via `invoice_items.purchase_id`.

---

## 9. Delivery phases

Ship in this order so managers get a daily pack before finance depth.

### Phase A — Manager daily pack

- OPS-01, OPS-02
- REV-01, REV-03, REV-04
- INV-02, INV-03
- CRM-04, CRM-05 (existing) + CRM-06
- Admin branch scoping fix on trial reports
- Centre-today dashboard

Exit: a manager can open one screen for “who is here, what we sold, what we are
owed, what is running out, which trial follow-ups are due”.

### Phase B — Growth and events

- CRM-01, CRM-02, CRM-03
- MEM-01, MEM-03, MEM-04
- BDAY-01, BDAY-02, BDAY-03
- TRIP-01, TRIP-02, TRIP-03

Exit: enquiry and trial conversion can be compared; parties and trips have a
pipeline and collection view.

### Phase C — Admin / finance

- REV-02, REV-05, REV-06, REV-07, REV-09
- INV-05 (cost), INV-06, INV-07
- XBR-01
- MEM-07

Exit: tax, voids/returns, margin, valuation, and inter-branch settlement can be
reconciled without spreadsheet exports.

### Phase D — Polish after schema (optional)

- Purchase `expires_at` → accurate MEM-05
- `last_visit_at` → cheaper MEM-04
- Expenses, register sessions, branch capacity
- CSV export, scheduled email of yesterday’s Z-report
- PPL-03 / PPL-04 as gated exports with audit

---

## 10. Acceptance principles

1. Manager cannot read another branch’s numbers.
2. Admin can select one branch or all branches; totals must not double-count.
3. Draft and voided invoices do not inflate sales.
4. Cost and margin never appear on staff clients; admin-only unless product
   explicitly grants manager finance access.
5. Trial conversion still increments once per entitlement (existing trial plan).
6. Date bounds are required or defaulted; unbounded “all time” list APIs are
   rejected.
7. Each Phase A report has at least one integration or repository test against
   seeded branch data.

---

## 11. Out of scope for v1

- Real-time WebSocket occupancy (polling the occupancy endpoint is enough).
- Embedded BI / Metabase; keep reports in-app.
- Marketing campaign send from PPL-04.
- Editing operational data from a report (reports are read-only).
- Replacing `/audit-logs`.

When implementation starts, implement Phase A first and tick IDs in this file
rather than inventing a second catalogue.
