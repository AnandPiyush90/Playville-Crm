# Invoice and Inventory Management Architecture

## 1. Objective

Add a branch-aware point-of-sale and inventory module that allows PlayVille to:

1. Maintain a reusable catalogue for snacks, beverages, other eatables, socks,
   apparel, and future retail products.
2. Maintain independent stock quantities and selling prices at each centre.
3. Track stock receipts, sales, returns, transfers, adjustments, wastage, and
   expired goods through an auditable ledger.
4. Create one invoice containing membership/package purchases, check-out charges,
   and retail products.
5. Associate an invoice with a registered customer and optionally a check-in,
   while still supporting a controlled walk-in retail sale.
6. Accept one or more payment methods and produce a printable/shareable invoice.
7. Provide branch-level sales, tax, margin, low-stock, expiry, and stock movement
   reports.

The first release should be a modular monolith inside the existing Spring Boot
application. Inventory, invoicing, package purchases, and check-out remain separate
domain modules but are coordinated by one transactional checkout facade.

## 2. Recommended business model

### 2.1 Product types

Use one product catalogue with these product types:

- `RETAIL`: physical goods whose stock is tracked, such as snacks and socks.
- `MEMBERSHIP`: a PlayVille package that credits sessions after payment.
- `SERVICE`: a non-stock charge, such as an extra play charge or party add-on.

`MEMBERSHIP` products reference the existing `pv_packages` row. Do not copy package
balance rules into inventory. A membership invoice line is the commercial record;
the existing purchase and entitlement logic remains the source of truth for session
credit.

### 2.2 Categories and variants

Categories are hierarchical and configurable. Initial examples are `Food`,
`Beverages`, `Apparel`, `Socks`, and `Services`.

A product is the customer-facing concept, while a SKU is the exact sellable and
stocked variant. For example:

- Product: PlayVille Grip Socks
- SKUs: Small/Blue, Medium/Blue, Large/Pink

Food without variants still has one default SKU. Barcode, sale price, cost price,
tax code, reorder level, and stock are maintained at SKU level.

### 2.3 Branch ownership

- Catalogue data is shared across all branches.
- SKU availability, price override, reorder level, and stock are branch-specific.
- Every stock movement, invoice, payment, and register session carries `branch_id`.
- Staff derive their branch from the JWT; clients cannot choose another branch.
- An admin may query multiple branches, but an ordinary sale always occurs in the
  authenticated staff member's branch.

### 2.4 Invoice policy

- `DRAFT` is an editable cart and has no accounting or stock effect.
- `ISSUED` is immutable and has invoice number, tax snapshots, stock movements, and
  payment records.
- An issued invoice is never deleted or edited. Corrections use `VOIDED` before
  settlement or a return/credit note after settlement.
- Prices, names, SKU, HSN/SAC or equivalent tax code, and tax rates are copied onto
  invoice lines so historical invoices do not change when the catalogue changes.
- Amounts use `DECIMAL`/Java `BigDecimal`, never floating point.
- Tax rules, invoice labels, registration numbers, and rounding are configuration,
  not hard-coded assumptions. Final statutory fields must be approved by PlayVille's
  accountant before production rollout.

## 3. Bounded modules

| Module | Responsibility |
|---|---|
| Catalogue | Categories, products, SKUs, barcodes, tax profiles, branch price/availability |
| Inventory | Stock balances, batches, expiry, movement ledger, transfers, counts, low-stock rules |
| Invoicing | Draft cart, pricing snapshots, totals, invoice numbering, issue/void/return lifecycle |
| Payments | Split tenders, references, payment/refund audit, reconciliation status |
| Membership | Existing package purchase and session-credit behavior |
| Checkout orchestration | Atomically issue invoice, take stock, record payment, credit membership, and link check-in |
| Reporting | Sales, tax, payment, inventory valuation, margin, expiry, and audit views |

Controllers should call application services. Repositories must not be called across
controllers, and UI clients must never calculate authoritative totals or stock.

## 4. Data model

### 4.1 Catalogue

#### `pv_product_categories`

`id`, `parent_id`, `category_code`, `category_name`, `display_order`, `is_active`,
`created_at`, `updated_at`.

#### `pv_products`

`id`, `product_code`, `product_name`, `description`, `category_id`, `product_type`,
`package_id` (nullable), `track_inventory`, `is_active`, audit timestamps.

Rules:

- `product_code` is globally unique and stable.
- `MEMBERSHIP` requires `package_id` and cannot track inventory.
- `RETAIL` normally tracks inventory.
- Deactivation is allowed; physical deletion is not.

#### `pv_product_skus`

`id`, `product_id`, `sku_code`, `barcode`, `variant_attributes_json`, `unit_of_measure`,
`default_sale_price`, `default_cost_price`, `tax_profile_id`, `has_expiry`, `is_active`,
audit timestamps.

`variant_attributes_json` stores display attributes such as
`{"size":"M","colour":"Blue"}`. Frequently searched dimensions can be normalized
later without changing invoice history.

#### `pv_tax_profiles`

`id`, `tax_code`, `tax_name`, `hsn_sac_code`, `rate_percent`, `price_includes_tax`,
`effective_from`, `effective_to`, `is_active`.

Rates are effective-dated. An invoice line stores the resolved rate and tax amounts.

#### `pv_branch_skus`

`id`, `branch_id`, `sku_id`, `sale_price_override`, `reorder_level`, `reorder_quantity`,
`allow_negative_stock`, `is_available`, `version`, audit timestamps.

Unique key: `(branch_id, sku_id)`. `allow_negative_stock` should default to false and
only admin can change it.

### 4.2 Inventory

#### `pv_inventory_balances`

`id`, `branch_id`, `sku_id`, `quantity_on_hand`, `quantity_reserved`, `version`,
`updated_at`.

Unique key: `(branch_id, sku_id)`. Available quantity is
`quantity_on_hand - quantity_reserved`. Use row locking or optimistic version checks
when issuing an invoice.

#### `pv_inventory_batches`

`id`, `branch_id`, `sku_id`, `batch_number`, `supplier_id`, `received_at`,
`manufactured_on`, `expires_on`, `unit_cost`, `quantity_received`,
`quantity_remaining`, `status`, audit timestamps.

Batches are required for expiry-controlled food and optional for apparel. Sales use
FEFO (first expiry, first out); products without expiry use FIFO.

#### `pv_inventory_movements`

`id`, `branch_id`, `sku_id`, `batch_id`, `movement_type`, `quantity_delta`,
`unit_cost_snapshot`, `reference_type`, `reference_id`, `reason_code`, `notes`,
`performed_by_staff_id`, `approved_by_staff_id`, `idempotency_key`, `created_at`.

Movement types: `OPENING_STOCK`, `RECEIPT`, `SALE`, `SALE_RETURN`, `TRANSFER_OUT`,
`TRANSFER_IN`, `ADJUSTMENT_IN`, `ADJUSTMENT_OUT`, `WASTAGE`, and `EXPIRY`.

This table is append-only. `quantity_delta` is positive for stock in and negative for
stock out. Balance is a fast projection; movements are the audit source of truth.

#### `pv_stock_transfers` and `pv_stock_transfer_items`

Header fields: transfer number, source branch, destination branch, status
(`DRAFT`, `DISPATCHED`, `RECEIVED`, `CANCELLED`), staff and timestamps.

Item fields: SKU, batch, requested, dispatched, and received quantities. Dispatch
creates `TRANSFER_OUT`; receipt creates `TRANSFER_IN`. Receiving staff can record a
variance, but cannot silently alter the dispatched quantity.

#### `pv_suppliers` and `pv_stock_receipts`

Supplier master stores supplier identity and contact/tax metadata. Receipt header and
items capture supplier invoice reference, SKU, batch, expiry, quantity, cost, and tax.
Supplier procurement is deliberately separate from customer invoicing.

### 4.3 Invoicing and payments

#### `pv_invoices`

`id`, `invoice_number`, `branch_id`, `customer_id` (nullable for walk-in retail),
`checkin_id` (nullable), `status`, `invoice_type`, `invoice_date`, `due_at`,
`customer_name_snapshot`, `customer_phone_snapshot`, `customer_email_snapshot`,
`subtotal`, `discount_total`, `tax_total`, `rounding_adjustment`, `grand_total`,
`amount_paid`, `balance_due`, `notes`, `issued_by_staff_id`, `voided_by_staff_id`,
`void_reason`, `idempotency_key`, `version`, audit timestamps.

Invoice number is generated server-side from a locked branch/year sequence, for
example `NAL/2026-27/000123`. The exact format is configurable and numbers are never
reused.

Invoice types: `SALE`, `CREDIT_NOTE`. Statuses: `DRAFT`, `ISSUED`, `PARTIALLY_PAID`,
`PAID`, `VOIDED`, `REFUNDED`, `PARTIALLY_REFUNDED`.

#### `pv_invoice_items`

`id`, `invoice_id`, `line_number`, `line_type`, `product_id`, `sku_id`, `package_id`,
`purchase_id` (nullable), `description_snapshot`, `sku_snapshot`, `hsn_sac_snapshot`,
`quantity`, `unit_price`, `gross_amount`, `discount_amount`, `taxable_amount`,
`tax_rate_snapshot`, `cgst_amount`, `sgst_amount`, `igst_amount`, `tax_amount`,
`line_total`, `returned_quantity`, `metadata_json`.

Line types: `RETAIL`, `MEMBERSHIP`, `SERVICE`, `CHECKOUT_CHARGE`. All arithmetic is
recomputed server-side. `purchase_id` links a finalized membership line to the
existing `pv_purchases` record.

#### `pv_payments`

`id`, `invoice_id`, `branch_id`, `payment_type`, `payment_mode`, `amount`,
`provider_reference`, `status`, `received_by_staff_id`, `idempotency_key`,
`paid_at`, `notes`.

Payment types are `PAYMENT` and `REFUND`. Modes initially include `CASH`, `UPI`,
`CARD`, and `ONLINE`. Multiple rows permit split payment. Never store full card data.

#### `pv_invoice_sequences`

`branch_id`, `financial_year`, `next_number`, `version`. Lock this row while assigning
an invoice number. A database unique key on `invoice_number` is still mandatory.

#### Returns

`pv_sales_returns` and `pv_sales_return_items` reference the original invoice and
line. A posted return creates positive inventory movements for resellable goods,
refund payment rows when applicable, and a credit note. Membership returns require a
separate manager-approved policy because credited sessions may already have been used.

## 5. Key relationships

```text
Customer ----< Invoice >---- Branch ----< Inventory Balance >---- SKU >---- Product
                  |                                      |                    |
                  |                                      +----< Movement      +---- Package
                  +----< Invoice Item >---- SKU/Package
                  |
                  +----< Payment
                  |
                  +---- Check-in

Invoice Item (MEMBERSHIP) ---- Purchase ---- session balance/entitlement logic
Invoice Item (RETAIL) -------- Inventory Movement (SALE)
```

## 6. Transactional workflows

### 6.1 Front-desk sale and check-out

1. Staff opens an active check-in or searches/selects a customer.
2. UI requests `checkout-preview`; backend returns session impact and current cart
   context.
3. Staff adds membership and/or retail SKUs by search or barcode.
4. Backend creates/updates a `DRAFT` invoice and returns authoritative prices, tax,
   stock availability, discounts, membership details, and totals.
5. Staff submits payments and an `Idempotency-Key` to finalize.
6. Backend validates customer/check-in ownership, locks stock rows, recalculates all
   totals, validates payment total, and assigns an invoice number.
7. For each retail line, backend allocates batches and writes `SALE` movements.
8. For each membership line, backend invokes the existing package purchase logic and
   links the resulting `pv_purchases` row.
9. Backend stores payments, issues the invoice, and completes check-out when requested.
10. The response contains the invoice, updated membership/session balance, updated
    stock availability, and a print/share URL or document endpoint.

Steps 6-9 must commit in one database transaction. Retrying the same idempotency key
returns the original result and never duplicates stock deduction, payment, membership
credit, or check-out.

### 6.2 Stock receipt

Manager selects supplier, records supplier invoice, lines, batch and expiry details,
and submits the receipt. Backend locks affected balances, writes `RECEIPT` movements,
updates batch and balance projections, and posts the receipt atomically.

### 6.3 Stock adjustment and wastage

Staff may submit a request; manager/admin approval is required for reductions above a
configurable threshold. A reason and notes are mandatory. The original movement is
never edited; corrections use a compensating movement.

### 6.4 Return

Backend validates original line, return window, previously returned quantity, and
branch. It creates a credit note, stock movement for resellable quantity, refund or
store-credit record, and updated invoice refund status in one transaction.

## 7. API contract

Use the existing success envelope and validation error contract. Every mutating POS,
inventory, and payment request accepts `Idempotency-Key`.

### 7.1 Catalogue

- `GET /api/v1/product-categories`
- `POST /api/v1/product-categories` (admin)
- `GET /api/v1/products?search=&categoryId=&type=&active=&page=&size=`
- `GET /api/v1/products/{id}`
- `POST /api/v1/products` (admin)
- `PUT /api/v1/products/{id}` (admin)
- `PATCH /api/v1/products/{id}/status` (admin)
- `POST /api/v1/products/{id}/skus` (admin)
- `PUT /api/v1/skus/{id}` (admin)
- `PUT /api/v1/branch-skus/{skuId}` (manager/admin branch pricing and reorder data)

Product creation example:

```json
{
  "productCode": "SOCKS-GRIP",
  "productName": "PlayVille Grip Socks",
  "categoryId": 20,
  "productType": "RETAIL",
  "trackInventory": true,
  "skus": [
    {
      "skuCode": "SOCKS-GRIP-M-BLU",
      "barcode": "890000000001",
      "variantAttributes": { "size": "M", "colour": "Blue" },
      "unitOfMeasure": "PIECE",
      "defaultSalePrice": 149.00,
      "defaultCostPrice": 65.00,
      "taxProfileId": 2,
      "hasExpiry": false
    }
  ]
}
```

### 7.2 Inventory

- `GET /api/v1/inventory?search=&categoryId=&lowStock=&expiringBefore=&page=&size=`
- `GET /api/v1/inventory/skus/{skuId}/movements?from=&to=&type=&page=&size=`
- `POST /api/v1/inventory/receipts` (manager/admin)
- `POST /api/v1/inventory/adjustments` (manager/admin; reason required)
- `POST /api/v1/inventory/wastage` (manager/admin; reason required)
- `POST /api/v1/stock-transfers` (manager/admin)
- `POST /api/v1/stock-transfers/{id}/dispatch` (manager/admin)
- `POST /api/v1/stock-transfers/{id}/receive` (destination manager/admin)
- `GET /api/v1/inventory/alerts`

### 7.3 Invoices and payments

- `POST /api/v1/invoices` creates a draft.
- `GET /api/v1/invoices?from=&to=&status=&customerId=&paymentMode=&page=&size=`
- `GET /api/v1/invoices/{id}`
- `PUT /api/v1/invoices/{id}/items` replaces draft items with version checking.
- `POST /api/v1/invoices/{id}/finalize` validates payment and issues atomically.
- `POST /api/v1/invoices/{id}/void` (manager/admin; reason required).
- `POST /api/v1/invoices/{id}/returns` (manager/admin).
- `GET /api/v1/invoices/{id}/document` returns PDF or print-ready content.
- `POST /api/v1/invoices/{id}/share` sends through an approved channel and audits it.

Draft/finalize example:

```json
{
  "customerId": 42,
  "checkinId": 700,
  "completeCheckout": true,
  "items": [
    { "lineType": "MEMBERSHIP", "packageId": 3, "quantity": 1 },
    { "lineType": "RETAIL", "skuId": 110, "quantity": 2 },
    { "lineType": "RETAIL", "skuId": 205, "quantity": 1 }
  ],
  "payments": [
    { "paymentMode": "UPI", "amount": 4000.00, "providerReference": "UPI123" },
    { "paymentMode": "CASH", "amount": 297.00 }
  ],
  "checkout": {
    "conversionOutcome": "PURCHASED",
    "checkoutNotes": "Package and retail sale"
  }
}
```

The response includes line snapshots, totals, payment status, membership purchase
details, new session balance, invoice number, and check-out status.

### 7.4 Error codes

Use stable machine-readable codes with field errors where applicable:

- `INSUFFICIENT_STOCK`
- `SKU_NOT_AVAILABLE_AT_BRANCH`
- `PRODUCT_INACTIVE`
- `BATCH_EXPIRED`
- `INVOICE_NOT_EDITABLE`
- `INVOICE_ALREADY_FINALIZED`
- `PAYMENT_TOTAL_MISMATCH`
- `DUPLICATE_PAYMENT_REFERENCE`
- `RETURN_QUANTITY_EXCEEDED`
- `MEMBERSHIP_RETURN_NOT_ALLOWED`
- `STALE_INVOICE_VERSION`

Example:

```json
{
  "success": false,
  "code": "INSUFFICIENT_STOCK",
  "message": "Some items are no longer available in the requested quantity",
  "fieldErrors": {
    "items[1].quantity": "Only 1 unit is available"
  }
}
```

## 8. Authorization and controls

| Capability | Staff | Manager | Admin |
|---|---:|---:|---:|
| Search products and view own-branch stock | Yes | Yes | Yes |
| Create/finalize invoice | Yes | Yes | Yes |
| Apply standard configured discount | Yes | Yes | Yes |
| Override price/discount above threshold | No | Yes | Yes |
| Receive or adjust stock | No | Yes | Yes |
| Dispatch/receive transfer | No | Yes | Yes |
| Void, refund, or return | No | Yes | Yes |
| Manage catalogue/tax profiles | No | No | Yes |
| Cross-branch reports | No | Optional | Yes |

Record both requesting and approving staff for sensitive actions. Introduce explicit
permissions later if the three roles become too coarse; do not scatter role strings
through service code.

## 9. UI experience

The front-desk checkout should be one operational screen:

- Customer/check-in and membership balance remain visible.
- Product search and barcode entry add items quickly.
- Variant selection shows branch availability and price.
- Membership packages appear in the same item picker but display sessions, bonus,
  validity, current balance, and resulting balance.
- Cart shows quantity, price, discount, tax, and line total.
- Payment area supports split tenders and clearly shows amount remaining/change due.
- Finalize is disabled until server validation succeeds.
- The issued invoice offers print and share actions.

Inventory administration should have separate catalogue, stock, receipts, transfers,
adjustments, expiry, and reporting views. Avoid exposing cost price or margin to normal
front-desk staff.

## 10. Reporting

Initial reports:

- Daily sales by branch, category, SKU, staff, and payment mode.
- Membership versus retail versus service revenue.
- Tax summary based on immutable invoice snapshots.
- Current stock, available stock, low stock, out of stock, and negative-stock audit.
- Batch expiry report for 7/15/30/60-day windows.
- Stock movement ledger and stock valuation.
- Gross margin using invoice revenue and batch cost snapshots.
- Wastage, expiry, and adjustment variance.
- Returns, voids, refunds, and manager overrides.
- Cash/UPI/card reconciliation per staff register session.

Reporting queries should use branch/date indexes and read models where necessary;
invoice finalization must not synchronously run report aggregation.

## 11. Non-functional requirements

- Idempotency on invoice finalization, payments, receipts, transfers, and returns.
- Row locking in deterministic SKU-id order to prevent deadlocks and overselling.
- Unique constraints for invoice number, SKU code, barcode (when present), and
  idempotency keys.
- No negative stock unless an explicit admin-controlled branch-SKU policy permits it.
- Append-only inventory and financial audit records.
- Optimistic version field on drafts and stock configuration to reject stale edits.
- Server-side pricing, tax, discount, stock, membership, and check-out validation.
- Structured logs include correlation id, branch, staff, invoice, and idempotency key,
  but exclude sensitive payment/customer data.
- Pagination and bounded date ranges on all list/report APIs.
- Database backups and tested restore procedure before production activation.
- Feature flags for `invoice-module`, `inventory-module`, and `unified-checkout`.

## 12. Delivery plan

### Phase 1: Catalogue and branch stock foundation

- Schema, entities, repositories, catalogue APIs, and branch-SKU configuration.
- Opening stock import and movement ledger.
- Stock list, low-stock alerts, and basic audit report.

Exit: admin can configure products/SKUs; manager can load opening stock; branch staff
can search only saleable SKUs with accurate availability.

### Phase 2: Retail invoice MVP

- Draft, totals, invoice number sequence, single/split payments, finalization, PDF/print.
- Retail sale movements and invoice search/detail.
- Concurrency and idempotency tests.

Exit: two simultaneous sales cannot oversell stock; retry cannot duplicate an invoice,
payment, or stock movement.

### Phase 3: Unified membership and check-out

- Membership product mapping and invoice-line-to-purchase link.
- Transactional facade integrating `PurchaseService` and `CheckinService` behavior.
- Customer/check-in invoice UI contract and conversion attribution.

Exit: one invoice can sell a package and retail items, update session balance, consume
stock, collect payment, and close the check-in exactly once.

### Phase 4: Inventory operations

- Suppliers, batch receipts, expiry, wastage, adjustments, and transfers.
- FEFO allocation and expiry alerts.
- Approval rules and audit reporting.

Exit: every balance change reconciles to movements and every expiring food item can be
traced to a batch receipt.

### Phase 5: Returns, reconciliation, and production hardening

- Returns, credit notes, refunds, manager overrides, register reconciliation.
- Tax, margin, inventory valuation, and exception reports.
- Load, security, backup/restore, and end-to-end tests.

Exit: accountant-approved invoice output, reconciled stock/payment reports, production
runbook, rollback plan, and signed acceptance scenarios.

## 13. Acceptance scenarios

1. Sell two snack items to a walk-in and issue a paid invoice.
2. Sell socks with size/colour variant and decrement only that branch/SKU.
3. Sell a membership and snack in one invoice; sessions and stock update once.
4. Finalize during check-out and preserve customer, check-in, trial, purchase, and
   invoice links.
5. Retry finalization after a timeout and receive the same invoice without duplicates.
6. Reject a concurrent sale when remaining stock has already been consumed.
7. Receive a food batch with expiry and sell from the earliest valid batch.
8. Block sale of expired stock and show a field-level UI error.
9. Transfer stock between centres with separate dispatch and receipt audit.
10. Return an unopened item and create stock-in, credit note, and refund records.
11. Reject return quantity above the original unreturned quantity.
12. Ensure staff cannot see cost/margin, adjust stock, or access another branch.
13. Reconcile inventory balance to the sum of all movements.
14. Reconcile invoice totals to payments/refunds and daily payment-mode totals.

## 14. Decisions required before implementation

PlayVille stakeholders should lock these choices before schema migration:

- Whether retail sales may be anonymous or always require a customer phone number.
- Whether all prices are tax-inclusive and which tax profiles apply per product.
- Required invoice numbering format and financial-year reset behavior.
- Whether unpaid/partially paid invoices and customer credit are permitted.
- Discount limits for staff and managers.
- Return window, resellable condition, refund modes, and membership cancellation policy.
- Whether barcode scanners are keyboard-wedge devices or require device integration.
- Whether batches are mandatory for every edible item.
- Whether purchase orders are needed in the first release or supplier receipts are
  sufficient.
- Invoice delivery channels and customer consent requirements.

Recommended MVP defaults are: anonymous retail allowed but customer required for a
membership; tax-inclusive pricing; no customer credit; no negative stock; batches
mandatory for expiring items; supplier receipts without purchase orders; manager-only
voids/returns/stock adjustments; and print plus email invoice delivery.
