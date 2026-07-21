# Invoice and Inventory Angular UI Implementation Plan

## 1. Objective

Build the Angular user experience for the invoice and inventory modules defined in
`docs/invoice-inventory-architecture.md`.

The UI must support fast front-desk billing, branch-scoped stock operations, product
administration, returns, and reporting without duplicating backend business rules.
Server responses remain authoritative for price, tax, discount eligibility, stock,
membership credits, invoice numbering, payments, and check-out completion.

## 2. UX principles

- Make invoice creation the primary operational workflow, not a dashboard or landing
  page.
- Optimize the sale screen for repeated keyboard, barcode scanner, and touch use.
- Keep customer/check-in context, cart, totals, and payment status visible together.
- Keep inventory administration separate from front-desk selling.
- Show loading, empty, success, conflict, offline, and permission states explicitly.
- Never hide a rejected API operation behind a generic `Bad request` message.
- Use backend `fieldErrors` next to the corresponding control and show the response
  `message` in an alert region.
- Never expose cost, margin, cross-branch data, or privileged actions to unauthorized
  roles.
- Do not store draft payment references or customer information in browser storage.

## 3. Navigation and routes

Add navigation entries according to role and feature flags.

| Route | Screen | Roles |
|---|---|---|
| `/invoices/new` | Front-desk sale and unified check-out | staff, manager, admin |
| `/invoices` | Invoice search/history | staff, manager, admin |
| `/invoices/:id` | Invoice detail, print/share, permitted actions | staff, manager, admin |
| `/inventory` | Branch stock list and alerts | staff, manager, admin |
| `/inventory/products` | Product/SKU catalogue | admin |
| `/inventory/products/new` | Create product and variants | admin |
| `/inventory/products/:id/edit` | Edit/deactivate product and SKUs | admin |
| `/inventory/receipts/new` | Receive supplier stock | manager, admin |
| `/inventory/transfers` | Transfer list | manager, admin |
| `/inventory/transfers/new` | Create transfer | manager, admin |
| `/inventory/transfers/:id` | Dispatch/receive transfer | manager, admin |
| `/inventory/adjustments/new` | Adjustment or wastage | manager, admin |
| `/inventory/skus/:id/movements` | SKU movement ledger | manager, admin |
| `/reports/sales` | Sales/payment/tax reports | manager, admin |
| `/reports/inventory` | Stock/expiry/margin reports | manager, admin |

Use route guards for authentication, role, and feature flags. Guards improve UX but
do not replace backend authorization. A direct unauthorized URL should redirect to an
existing access-denied screen without briefly rendering protected content.

## 4. Suggested Angular feature structure

Adapt names to the Angular application's current conventions; do not introduce a new
state library or component framework solely for this module.

```text
src/app/features/
  invoices/
    pages/
      invoice-workspace/
      invoice-list/
      invoice-detail/
    components/
      sale-context/
      product-picker/
      barcode-input/
      sale-cart/
      cart-line/
      invoice-totals/
      payment-panel/
      invoice-status/
      return-dialog/
      void-dialog/
    data-access/
      invoice-api.service.ts
      invoice-draft.store.ts
      invoice.models.ts
      invoice.mappers.ts
  inventory/
    pages/
      inventory-list/
      product-list/
      product-editor/
      stock-receipt/
      transfer-list/
      transfer-detail/
      stock-adjustment/
      movement-ledger/
    components/
      inventory-filters/
      sku-selector/
      variant-editor/
      batch-lines/
      stock-alerts/
      transfer-lines/
    data-access/
      catalogue-api.service.ts
      inventory-api.service.ts
      inventory.models.ts
  shared/
    api/
      api-envelope.models.ts
      api-error.mapper.ts
      idempotency-key.service.ts
    forms/
      server-validation.service.ts
    money/
      money-display.pipe.ts
```

Prefer standalone components if that is already the application standard. Keep page
components responsible for orchestration and move reusable interaction into focused
components and API services.

## 5. Contract models

Generate or reconcile models from the live OpenAPI document. Do not invent endpoint
fallbacks or silently accept multiple response shapes.

```typescript
export interface ApiResponse<T> {
  success: boolean;
  message?: string;
  data: T;
  timestamp?: string;
}

export interface ApiErrorResponse {
  success: false;
  status?: number;
  error?: string;
  code?: string;
  message: string;
  path?: string;
  timestamp?: string;
  fieldErrors?: Record<string, string>;
}

export type ProductType = 'RETAIL' | 'MEMBERSHIP' | 'SERVICE';
export type InvoiceLineType =
  | 'RETAIL'
  | 'MEMBERSHIP'
  | 'SERVICE'
  | 'CHECKOUT_CHARGE';

export type InvoiceStatus =
  | 'DRAFT'
  | 'ISSUED'
  | 'PARTIALLY_PAID'
  | 'PAID'
  | 'VOIDED'
  | 'PARTIALLY_REFUNDED'
  | 'REFUNDED';

export type PaymentMode = 'CASH' | 'UPI' | 'CARD' | 'ONLINE';

export interface DraftInvoiceItemRequest {
  lineType: InvoiceLineType;
  skuId?: number;
  packageId?: number;
  quantity: number;
}

export interface InvoiceLineResponse {
  id: number;
  lineNumber: number;
  lineType: InvoiceLineType;
  productId?: number;
  skuId?: number;
  packageId?: number;
  purchaseId?: number;
  description: string;
  sku?: string;
  variantAttributes?: Record<string, string>;
  quantity: number;
  unitPrice: string;
  grossAmount: string;
  discountAmount: string;
  taxableAmount: string;
  taxRate: string;
  taxAmount: string;
  lineTotal: string;
  availableQuantity?: number;
  returnedQuantity?: number;
}

export interface InvoiceResponse {
  id: number;
  invoiceNumber?: string;
  branchId: number;
  customerId?: number;
  checkinId?: number;
  status: InvoiceStatus;
  version: number;
  customer?: InvoiceCustomerSnapshot;
  lines: InvoiceLineResponse[];
  subtotal: string;
  discountTotal: string;
  taxTotal: string;
  roundingAdjustment: string;
  grandTotal: string;
  amountPaid: string;
  balanceDue: string;
  membershipResult?: MembershipPurchaseResult;
  checkoutResult?: CheckoutResult;
}
```

Transport monetary values as decimal strings when the backend contract supports it.
For display use a decimal-safe formatter. Do not use JavaScript floating-point
arithmetic to determine whether payment balances, tax, or invoice totals match.

## 6. API services

All services should unwrap the existing envelope in one shared helper, preserve typed
errors, and let components react to `code` and `fieldErrors`.

### 6.1 `InvoiceApiService`

- `createDraft(request)` -> `POST /invoices`
- `search(query)` -> `GET /invoices`
- `getById(id)` -> `GET /invoices/{id}`
- `replaceItems(id, request, version)` -> `PUT /invoices/{id}/items`
- `finalize(id, request, idempotencyKey)` -> `POST /invoices/{id}/finalize`
- `void(id, reason, idempotencyKey)` -> `POST /invoices/{id}/void`
- `createReturn(id, request, idempotencyKey)` -> `POST /invoices/{id}/returns`
- `getDocument(id)` -> `GET /invoices/{id}/document` as `Blob`
- `share(id, request, idempotencyKey)` -> `POST /invoices/{id}/share`

### 6.2 `CatalogueApiService`

- Category list/create
- Product search/detail/create/update/status
- SKU create/update
- Branch price, availability, and reorder configuration

### 6.3 `InventoryApiService`

- Stock search and alert queries
- SKU movement history
- Receipt creation
- Adjustment and wastage creation
- Transfer create/detail/dispatch/receive

### 6.4 Existing service integration

Reuse existing customer search, active check-in, checkout preview, package, and auth
services. Do not create duplicate customer/package clients inside the invoice feature.

Update allowed CORS request headers in the backend contract to include
`Idempotency-Key` if it is not already present.

## 7. Draft state and request coordination

Use a feature-local draft store implemented with the application's existing state
style. The state should include:

```typescript
interface InvoiceDraftState {
  invoice: InvoiceResponse | null;
  customerContext: CustomerContext | null;
  checkinContext: CheckinContext | null;
  productQuery: string;
  productResults: ProductSearchItem[];
  payments: PaymentDraft[];
  loadingDraft: boolean;
  savingItems: boolean;
  finalizing: boolean;
  lastError: ApiErrorResponse | null;
  dirty: boolean;
}
```

State rules:

- The backend-created draft is the persisted cart and source of totals.
- Debounce text search, cancel stale requests, and never let an older result replace a
  newer query.
- Add/remove/quantity edits call `replaceItems` and replace local lines/totals with the
  returned draft.
- Send the current invoice `version`. Handle `STALE_INVOICE_VERSION` by loading the
  latest draft and asking the user to review changes.
- Disable conflicting item edits while a save is in progress, or serialize mutations.
- Keep one generated idempotency key for each finalize attempt. Reuse it after timeout
  or unknown network result; create a new key only after a definite rejected attempt
  that did not finalize.
- Do not persist payments, payment references, or customer details to `localStorage`.
- Warn before navigating away from a dirty draft. Server-side draft cleanup policy is
  owned by the backend.

## 8. Front-desk invoice workspace

The screen should use a stable three-region desktop layout and collapse sensibly for
tablet widths:

1. Context header: branch, customer, active check-in, children, membership/session
   balance, and check-out impact.
2. Product picker: search/barcode input, category filters, products, variants,
   availability, and membership package details.
3. Cart and payment area: line editor, server totals, payment methods, remaining amount,
   and finalization action.

### 8.1 Entry points

- Main navigation creates a normal retail draft.
- Customer detail starts a customer-linked draft.
- Active check-in `Checkout and invoice` action starts a check-in-linked draft.
- Invoice history opens issued invoices read-only.

Customer is optional only for retail-only invoices if the backend policy allows it.
Adding a membership line must require a selected customer before finalization.

### 8.2 Product picker

- Autofocus barcode/search when the workspace opens.
- Treat scanner input as keyboard text followed by Enter unless a different scanner
  integration is explicitly selected.
- Exact active barcode match adds one unit immediately.
- Multiple matches or a product with variants opens a variant selector.
- Product results show selling price and available quantity, not cost.
- Out-of-stock/inactive products cannot be added.
- Membership results show package name, paid/bonus/total sessions, validity, current
  balance, and backend-provided resulting balance preview.

### 8.3 Cart

- Use steppers or numeric inputs for quantity and familiar icons for remove actions.
- Disable quantity above currently returned availability, but still handle a server
  stock conflict because availability may change concurrently.
- Render line price, discount, tax, and total exactly as returned.
- Show product variant attributes as swatches/compact attributes where appropriate.
- Do not permit direct price or discount editing unless the API and role policy expose
  an approved override workflow.
- Keep layout dimensions stable while quantities and validation messages change.

### 8.4 Payment panel

- Use a segmented control/menu for payment mode and allow multiple payment rows.
- Require positive amount for every row.
- Require provider reference according to backend policy for UPI/card/online modes.
- Show backend `grandTotal`, payment entered, amount remaining, and cash change.
- Payment arithmetic displayed before submission is guidance only; finalization uses
  server validation.
- Disable finalize when cart is empty, required customer/check-out fields are missing,
  a draft save is pending, payment form is invalid, or finalization is in progress.
- Prevent double clicks and use the same idempotency key for uncertain retries.

### 8.5 Successful finalization

Replace the editable workspace with a concise success state containing:

- Invoice number and payment status
- Customer and check-in identity when present
- Updated membership/session balance
- Completed check-out status
- Print, share, and view invoice actions
- `New sale` action that creates a new empty draft and key

Do not optimistically show success before the finalize response arrives.

## 9. Invoice history and detail

### 9.1 List

Provide server-side filters for date range, status, invoice number/search, customer,
and payment mode. Use backend pagination and bounded date ranges. Persist only benign
filter preferences in query parameters.

Rows show invoice number, date/time, customer/walk-in, item summary, total, payment
status, and staff. Role-permitted overflow actions include view, print, share, return,
and void.

### 9.2 Detail

Show immutable snapshots rather than current product/customer values. Include lines,
tax breakdown, payments/refunds, membership purchase link, check-in link, audit staff,
returns/credit notes, and status timeline.

- `DRAFT`: editable only through the workspace.
- `ISSUED`/`PAID`: read-only; print/share and permitted return actions.
- `VOIDED`/`REFUNDED`: prominent status and reason; no ordinary sale actions.

### 9.3 Print and share

Fetch the document as a blob and open the browser print flow without exposing an
authenticated API URL as a public link. Revoke temporary object URLs after use.

Share dialog validates destination and channel, displays consent-sensitive warnings
defined by business policy, and reports backend delivery failure without changing the
invoice status.

## 10. Return and void workflows

Void is available only where the API permits it and requires a reason plus explicit
confirmation. Never remove the invoice from history after voiding.

Return UI:

- Loads returnable quantity from original line snapshots.
- Quantity cannot exceed `original - returned`.
- Captures condition/resellable status, reason, refund mode, and notes.
- Membership lines are disabled unless the backend explicitly returns them as
  returnable.
- Shows a server-calculated refund/credit preview before final submission when the API
  supports it.
- On success, routes to the credit note/updated invoice detail.

## 11. Catalogue administration

### 11.1 Product list

Server-side search/filter by category, product type, and active status. Display product
code, name, category, type, SKU count, and status. Cost data appears only for roles
authorized by the backend.

### 11.2 Product editor

Use a reactive form with product information and a `FormArray` of SKUs.

Product validation:

- Product code and name required with contract maximum lengths.
- Category and product type required.
- `MEMBERSHIP` requires package and forces inventory tracking off.
- `RETAIL` defaults inventory tracking on.

SKU validation:

- At least one SKU required.
- SKU code required and unique within the form.
- Barcode optional but validated against allowed backend format.
- Selling price cannot be negative.
- Cost price cannot be negative and is privileged.
- Unit of measure and tax profile required.
- Variant combinations cannot repeat within one product.
- Expiry toggle is available for inventory-tracked goods.

Backend duplicate codes/barcodes map to the exact form row. Products with history are
deactivated rather than deleted.

### 11.3 Branch SKU configuration

Manager/admin can edit branch sale-price override, availability, reorder level, and
reorder quantity according to role. Only admin can change negative-stock policy.
Version conflicts reload current data and preserve a reviewable copy of attempted
changes.

## 12. Inventory screens

### 12.1 Stock list

Display SKU, product, variant, category, on-hand, reserved, available, reorder level,
nearest expiry, and state. Use restrained status indicators for low/out-of-stock and
expiring items. Normal staff must not receive or render unit cost or valuation.

Filters map directly to API query parameters: search, category, low stock, and expiry
window. Selecting a row opens movement history for authorized users.

### 12.2 Stock receipt

Reactive form fields:

- Supplier, supplier invoice/reference, receipt date, and notes
- SKU, batch number, manufactured/expiry dates, quantity, unit cost, and tax per line

Rules:

- At least one line; quantities greater than zero.
- Batch and future expiry required when SKU `hasExpiry` is true.
- Duplicate SKU/batch entries should be merged or rejected consistently.
- Submit once with an idempotency key and show the posted receipt result.

### 12.3 Adjustment and wastage

Use separate operation choices rather than allowing arbitrary signed quantities.
Require SKU, quantity, reason code, and notes. Show available stock and reject a
reduction beyond it in the UI, while retaining server conflict handling.

### 12.4 Transfers

- Create: destination branch plus requested SKU quantities.
- Dispatch: source manager records dispatched quantity/batches.
- Receive: destination manager records received quantity and variance reason.
- Status controls determine available actions; dispatched transfers are not editable.
- Clearly label source versus destination and never derive either from user-editable
  hidden values.

### 12.5 Movement ledger

Read-only paginated table with timestamp, movement type, quantity delta, resulting
balance if supplied, batch/expiry, reference, reason, and staff. Filters remain in URL
query parameters so an audit view can be shared internally.

## 13. Validation and API error handling

Use Angular validators for immediate required, format, range, and cross-field feedback,
then merge backend validation into the same form.

```typescript
function applyServerErrors(
  form: FormGroup,
  fieldErrors: Record<string, string>
): UnmappedFieldError[] {
  // Convert paths such as items[1].quantity to FormArray controls.
  // Preserve existing client validator errors and return unknown paths for summary.
}
```

Requirements:

- Map paths such as `items[1].quantity`, `skus[0].barcode`, and
  `payments[1].providerReference` to nested controls.
- Do not overwrite client validation keys when adding a `server` error.
- Clear a control's server error when the user edits that control.
- Unknown paths appear in the form-level alert rather than disappearing.
- Focus the first invalid control after submission and announce the summary using an
  accessible alert region.
- Preserve backend message text for actionable business errors.

Error-code behavior:

| Code | UI response |
|---|---|
| `INSUFFICIENT_STOCK` | Mark affected quantity, refresh draft/availability, keep cart |
| `SKU_NOT_AVAILABLE_AT_BRANCH` | Mark/remove only after confirmation, refresh results |
| `PRODUCT_INACTIVE` | Mark line unavailable and refresh catalogue result |
| `BATCH_EXPIRED` | Show item error; backend chooses another valid batch if possible |
| `PAYMENT_TOTAL_MISMATCH` | Mark payment group and replace totals with server draft |
| `DUPLICATE_PAYMENT_REFERENCE` | Mark provider reference control |
| `STALE_INVOICE_VERSION` | Reload draft and require user review before retry |
| `INVOICE_ALREADY_FINALIZED` | Load invoice detail; do not issue again |
| `INVOICE_NOT_EDITABLE` | Switch to read-only detail |
| `RETURN_QUANTITY_EXCEEDED` | Mark affected return quantity |
| `MEMBERSHIP_RETURN_NOT_ALLOWED` | Disable membership return and preserve other lines |

For `401`, clear authentication and route to login using the existing interceptor.
For `403`, show access denied. For `409`, use code-specific recovery. For unknown
errors, show the backend message when safe and retain entered data.

## 14. Loading, concurrency, and offline behavior

- Use skeleton/table loading states for initial queries and localized progress states
  for buttons.
- Finalize, return, receipt, transfer, and adjustment buttons remain disabled until
  their request resolves.
- A timeout after mutation is an unknown outcome. Offer `Check status`/retry using the
  same idempotency key; never create a new transaction automatically.
- On network loss, preserve the in-memory draft but do not claim it is saved.
- Do not implement offline invoice finalization in the first release.
- Refresh availability after a stock conflict and after successful finalization.
- Avoid background polling on the active cart. Refresh alerts/list screens at a modest
  interval only if existing application patterns support it.

## 15. Accessibility and responsive behavior

- All controls have programmatic labels and validation associations.
- Status is conveyed by text/icon as well as color.
- Keyboard order follows search, results, cart, payment, finalize.
- Barcode input remains usable without a mouse.
- Dialogs trap focus and return it to the invoking control.
- Tables provide meaningful headers; mobile layouts preserve all financial values.
- Icon-only actions use the application's icon library and tooltips/accessible names.
- Test at front-desk desktop, tablet landscape, tablet portrait, and narrow mobile
  widths; no text, totals, or actions may overlap.

## 16. Security and privacy

- Role-based navigation is derived from authenticated principal claims.
- Never send `branchId`, staff id, calculated prices, tax, stock balance, cost, or
  invoice number as trusted user-entered authority.
- Do not log payment references, tokens, full customer payloads, or document blobs.
- Sanitize filenames received in `Content-Disposition` before use.
- Render product/customer text as text, not trusted HTML.
- Clear invoice draft state on logout and user/branch context change.
- Do not cache privileged inventory responses in a shared service after logout.

## 17. Testing strategy

### 17.1 Unit tests

- API URLs, query parameters, headers, envelope unwrapping, and typed error propagation.
- Idempotency key reuse after timeout and replacement after completed operation.
- Nested server error mapping for `FormArray` paths.
- Draft reducer/store serialization, stale response protection, and version conflicts.
- Role and feature-flag route guards.
- Decimal display and payment guidance without floating-point equality assumptions.

### 17.2 Component tests

- Product search, exact barcode add, variant selection, unavailable product states.
- Quantity editing and server-returned total replacement.
- Customer required for membership but optional for approved retail walk-in.
- Split payment validation and provider reference validation.
- Finalize double-click prevention and success/read-only transition.
- Stock receipt batch/expiry validation.
- Transfer status action visibility.
- Returnable quantity and membership-return restrictions.
- Field error display, focus, and error-summary accessibility.

### 17.3 Integration/E2E tests

1. Retail walk-in sale, payment, print, and invoice history.
2. Customer membership plus snack invoice with updated session balance.
3. Active check-in unified checkout and invoice completion.
4. Concurrent stock conflict retains cart and shows available quantity.
5. Finalize timeout retry produces exactly one invoice.
6. Manager receives expiring batch; stock and expiry alert update.
7. Source dispatch and destination receive stock transfer with variance.
8. Manager returns item and sees credit note, refund, and stock update.
9. Staff cannot access product administration, costs, adjustments, or returns.
10. Validation error maps `items[1].quantity` to the correct cart line.

## 18. Delivery plan

### F1. Contract foundation

- Export OpenAPI, generate/reconcile models, add envelope/error helpers, API services,
  route guards, and feature flags.
- Add service contract tests.

Exit: TypeScript compilation and API service tests pass against the exported contract.

### F2. Retail invoice workspace

- Draft store, product/barcode search, cart, server totals, payment panel, finalize,
  invoice detail, print, and history.
- Cover idempotency, stock conflict, stale draft, and navigation guard behavior.

Exit: a branch staff user can complete and retrieve a retail sale without duplicate
finalization or hidden errors.

### F3. Membership and check-in integration

- Customer/check-in entry points, checkout preview, membership items, resulting balance,
  and unified finalize response.

Exit: package, stock, payment, and check-out outcomes display from one final response.

### F4. Inventory administration

- Product/SKU editor, branch settings, stock list, receipts, adjustments, wastage,
  transfers, movements, and alerts.

Exit: permitted roles can complete every inventory movement workflow and staff cannot
access privileged data/actions.

### F5. Returns, reporting, and production readiness

- Void/return/credit note flows, sales/inventory reports, accessibility, responsive
  validation, telemetry, and end-to-end test suite.

Exit: all acceptance scenarios pass against the deployed API and accountant-approved
invoice output.

## 19. Definition of done

- UI uses only endpoints present in the exported OpenAPI contract.
- Every request and response type is explicit; no feature-level `any` types.
- Backend field errors appear beside controls, including nested array fields.
- All mutation retry paths preserve idempotency correctly.
- Server values replace local preview values after every cart mutation.
- No unauthorized route, action, cost, margin, or cross-branch data is rendered.
- Empty/loading/error/conflict/success states are implemented for every screen.
- Keyboard, barcode scanner, tablet, print, and responsive workflows are verified.
- Unit, component, TypeScript, lint, production build, and E2E checks pass.
- Debug payloads, mocked endpoint fallbacks, and console logging are removed.
- Swagger/OpenAPI snapshot and frontend models are synchronized for release.

## 20. Backend contract clarifications needed before UI integration

The Angular team can scaffold screens and mocked service tests immediately, but these
details must be locked in OpenAPI before connecting live flows:

- Exact request/response schemas for draft creation, item replacement, finalization,
  returns, share, receipts, adjustments, and transfers.
- Whether decimal amounts are JSON strings or numbers.
- Whether draft item updates replace all lines or support line-level commands.
- Discount/price override API and permission/approval behavior.
- Payment reference requirements per mode and cash change handling.
- Anonymous retail policy and required customer snapshot fields.
- Tax-inclusive display and invoice tax-breakdown fields.
- Return preview endpoint and membership cancellation policy.
- Document response content type and share channels.
- Inventory report endpoints and response schemas.
- `Idempotency-Key` inclusion in backend CORS allowed headers.

Until these are exported, the UI should isolate mocked assumptions in fixtures and
must not add undocumented production endpoint fallbacks.
