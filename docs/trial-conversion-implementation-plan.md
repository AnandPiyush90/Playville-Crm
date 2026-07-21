# Trial Visit and Conversion Implementation Plan

## 1. Objective

Deliver a production-ready walk-in flow in which PlayVille staff can:

1. Find an existing family by phone or register a new customer.
2. Capture one or more children, safety notes, and waiver acceptance before entry.
3. Check whether the family/child is eligible for a complimentary first visit.
4. Issue a controlled one-session trial entitlement.
5. Check selected children in using paid or complimentary entitlements.
6. At checkout, sell a package or record a non-conversion/follow-up outcome.
7. Preserve an auditable trail and report trial-to-purchase conversion.

The backend is the source of truth for eligibility, balances, entitlement allocation,
branch access, checkout calculations, and conversion. Angular must not reproduce these
rules.

## 2. Parallel-work agreement

Use two VS Code instances/workspaces:

- Backend instance: Spring Boot repository.
- Frontend instance: this Angular repository.

Both tracks must agree on the contract in section 4 before implementation. The backend
owns the final OpenAPI document. When an endpoint is implemented, export the live
contract from `http://localhost:8080/api/v1/api-docs` into this repository's
`swagger.json` and reconcile Angular types against it.

Parallelization boundary:

- Backend may implement migrations, domain logic, endpoints, validation, and tests
  independently.
- Frontend may implement layouts, form state, service interfaces, routing, and mocked
  component tests against the agreed contract.
- Do not connect the new UI to guessed endpoint variants. Integrate only endpoints in
  section 4 or the updated Swagger.
- Do not remove current customer, purchase, or check-in APIs. Changes are additive and
  backward compatible until the new flow passes its smoke tests.

## 3. Locked business decisions for the first release

- A person who enters the facility must be a customer, not only an enquiry.
- Trial policy is one complimentary first-visit entitlement per family by default.
- The entitlement may cover all selected eligible children in one check-in session.
  Keep the data model capable of per-child policy later.
- Default trial validity is the issuing branch's local business day. Make the duration
  configurable; never accept an arbitrary validity from ordinary staff.
- Existing paid customers are not automatically trial eligible. A manager may issue a
  manual complimentary entitlement with a required reason.
- Trials are not purchases and must not appear as revenue or zero-price packages.
- Sessions are reserved at check-in and consumed at successful checkout. A cancelled
  visit releases the reservation.
- Purchase during/after a trial retains links to the originating check-in and trial.
- A checkout can finish without purchase, but staff must record an outcome.
- Enquiry management is included as a later slice after the physical-visit workflow.

## 4. Shared API contract

All responses retain the existing `{ success, message, data, timestamp }` envelope.
Authenticated endpoints derive staff and branch from the JWT.

### 4.1 Customer onboarding

`POST /api/v1/customer-onboarding`

```json
{
  "customer": {
    "parentName": "Anita Rao",
    "phoneNumber": "9876543210",
    "email": "anita@example.com",
    "leadSource": "Walk_in",
    "emergencyContactName": "Ravi Rao",
    "emergencyContactPhone": "9876543211",
    "marketingConsent": true,
    "disclaimerAccepted": true,
    "disclaimerVersion": "2026-01"
  },
  "kids": [
    {
      "kidName": "Aarav",
      "dob": "2021-05-10",
      "gender": "Male",
      "specialNotes": "Peanut allergy"
    }
  ],
  "visitPurpose": "COMPLIMENTARY_TRIAL",
  "trial": {
    "campaignCode": "FIRST_VISIT",
    "notes": "Walk-in trial"
  }
}
```

Response data contains `customer`, `kids`, `trialEligibility`, optional
`issuedEntitlement`, and `nextAction` (`CHECK_IN`, `PURCHASE`, or `CUSTOMER_DETAIL`).

The endpoint is transactional and idempotent using an `Idempotency-Key` header. A
duplicate phone returns `409` with code `CUSTOMER_PHONE_EXISTS` and the existing
customer id; it must not silently create a second family.

### 4.2 Trial eligibility and entitlement APIs

- `GET /api/v1/customers/{customerId}/trial-eligibility?kidIds=1,2`
- `POST /api/v1/customers/{customerId}/trial-entitlements`
- `GET /api/v1/customers/{customerId}/entitlements?status=ACTIVE&type=COMPLIMENTARY_TRIAL`
- `POST /api/v1/customer-entitlements/{id}/cancel` (manager/admin, reason required)
- `POST /api/v1/customers/{customerId}/complimentary-entitlements` (manager/admin)

Issue-trial request:

```json
{
  "kidIds": [10, 11],
  "campaignCode": "FIRST_VISIT",
  "notes": "Issued at front desk"
}
```

Eligibility returns `eligible`, `scope`, `eligibleKidIds`, `reasonCode`, `reason`, and
an optional existing entitlement. Expected reason codes include
`FIRST_VISIT`, `TRIAL_ALREADY_USED`, `TRIAL_ALREADY_ACTIVE`, `PREVIOUS_PAID_PURCHASE`,
`ACTIVE_CHECKIN`, `WAIVER_REQUIRED`, `CUSTOMER_INACTIVE`, and `KID_INELIGIBLE`.

### 4.3 Check-in

Preserve the existing `kidIds` request temporarily. Add optional allocation details:

`POST /api/v1/checkins`

```json
{
  "customerId": 42,
  "kidIds": [10, 11],
  "visitType": "COMPLIMENTARY_TRIAL",
  "entitlementId": 501,
  "notes": "First visit"
}
```

The response adds `visitType`, `entitlementAllocations`, and `conversionStatus`.
The backend validates and reserves the entitlement atomically. It must never trust a
client branch id or allow a negative balance.

Add:

- `GET /api/v1/checkins/{checkinId}/checkout-preview`
- `POST /api/v1/checkins/{checkinId}/cancel` with a reason

### 4.4 Purchase and checkout

Extend `POST /api/v1/purchases` with optional:

```json
{
  "sourceCheckinId": 700,
  "sourceTrialEntitlementId": 501,
  "purchaseContext": "TRIAL_CHECKOUT"
}
```

Extend `POST /api/v1/checkins/{checkinId}/checkout`:

```json
{
  "extraCharges": 0,
  "checkoutNotes": "Enjoyed the facility",
  "conversionOutcome": "PURCHASED",
  "conversionReason": null,
  "purchaseId": 900,
  "followUpAt": null
}
```

Outcomes: `PURCHASED`, `FOLLOW_UP_REQUIRED`, `DECLINED`, `NOT_OFFERED`.
Reasons: `PRICE`, `NEEDS_TIME`, `CHILD_NOT_INTERESTED`, `WILL_RETURN`, `OTHER`.

For the initial release, purchase then checkout are separate calls. Both must be
idempotent, and checkout with `PURCHASED` validates that the purchase belongs to the
same customer/check-in. Add `POST /checkins/{id}/convert-and-checkout` later only if
partial operation handling is operationally unacceptable.

### 4.5 Reporting

- `GET /api/v1/reports/trial-funnel?from=&to=&branchId=&staffId=&leadSource=&campaignCode=`
- `GET /api/v1/reports/trial-conversions` with the same filters and pagination.

Funnel data includes issued, checked-in, completed, purchased same-day, purchased in
7 days, expired/no-show, declined, and conversion rate. Monetary values use decimal
types and never floating-point arithmetic.

### 4.6 Enquiries (final slice)

- `POST /api/v1/enquiries`
- `GET /api/v1/enquiries` with status/search/pagination
- `GET /api/v1/enquiries/{id}`
- `PUT /api/v1/enquiries/{id}`
- `PATCH /api/v1/enquiries/{id}/status`
- `POST /api/v1/enquiries/{id}/convert`

Statuses: `NEW`, `CONTACTED`, `VISIT_SCHEDULED`, `CONVERTED_TO_CUSTOMER`, `LOST`.
Conversion links to an existing customer by normalized phone or creates a customer;
it never produces an unlinked duplicate.

## 5. Spring Boot implementation plan

### B1. Baseline and conventions

- Inspect entities, Flyway/Liquibase migrations, exception handling, security context,
  branch scoping, auditing, and current balance mutation code.
- Add characterization tests for purchase balance addition and checkout deduction.
- Confirm the database and locking strategy.
- Use current package conventions; do not introduce a second architectural pattern.

Exit: existing backend tests pass and current behavior is documented by tests.

### B2. Schema and domain

Add migrations for:

- Customer safety, consent, and lifecycle summary fields.
- `customer_entitlements`.
- `entitlement_transactions` ledger.
- Check-in entitlement/visit/conversion fields; use a child allocation table if the
  current model has one row per family visit.
- Purchase attribution fields.
- Useful indexes on normalized phone, customer/type/status, expiry, check-in status,
  purchase source trial, and reporting dates.

Add enums for entitlement type/status, ledger transaction type, visit type, customer
lifecycle, purchase context, conversion outcome, and conversion reason.

Add optimistic `@Version` or atomic conditional updates. Store timestamps in UTC and
calculate business-day expiry in the branch timezone.

Exit: migration applies to an existing database without losing balances or purchases.

### B3. Entitlement services

- Implement eligibility as a pure query service.
- Implement transactional grant, reserve, release, consume, expire, cancel, and manual
  adjustment operations.
- Add a scheduled expiry job that is safe to run repeatedly.
- Enforce family trial uniqueness in both service logic and an appropriate database
  constraint/locking scheme.
- Maintain `globalSessionBalance` only as a compatibility projection during migration.

Exit: concurrency tests prove two simultaneous requests cannot grant or consume twice.

### B4. Trial and onboarding controllers

- Implement section 4.1 and 4.2 DTOs/controllers.
- Validate phone format, child ownership, waiver requirements, roles, and branch scope.
- Add structured error codes in the response body.
- Make onboarding transactional and idempotent.

Exit: Swagger examples and controller/service integration tests cover new/existing
customer, eligible/ineligible trial, duplicate request, and rollback on child failure.

### B5. Check-in integration

- Change check-in to select/validate entitlement and reserve sessions.
- Support multi-child visits without double-consuming a family-scoped trial.
- Reject duplicate active check-in with `409 ACTIVE_CHECKIN_EXISTS`.
- Add cancel/release and checkout-preview operations.
- Include visit funding in active and detail responses.

Exit: paid visits still work; trial reserve/cancel/retry/multi-kid cases pass.

### B6. Purchase and checkout conversion

- Add attribution fields to purchase command/response.
- At checkout, consume the reservation exactly once.
- Record outcome and follow-up information.
- Validate referenced purchase ownership and update lifecycle summary.
- Ensure retries return the completed result and do not consume again.

Exit: purchase+checkout, decline, follow-up, network retry, and invalid purchase link
tests pass.

### B7. Reporting and enquiries

- Implement aggregate queries from transactional records, not summary strings.
- Verify branch and role scoping.
- Implement enquiry CRUD and atomic conversion after the trial flow is stable.

Exit: reports reconcile against seeded end-to-end scenarios; enquiry conversion cannot
create duplicate customers.

### B8. Backend completion gate

- Unit, repository, controller, security, concurrency, and migration tests pass.
- Swagger documents every enum, error response, example, and required field.
- `/auth/hash` is removed or disabled outside local development.
- Export `/api/v1/api-docs` to the frontend `swagger.json`.
- Provide seed data or a repeatable script for the end-to-end cases in section 7.

## 6. Angular implementation plan

### F1. Contract types and API services

- Add typed models for onboarding, eligibility, entitlements, allocations, preview,
  purchase context, conversion outcomes, reports, and enquiries.
- Keep normalization at service boundaries and unwrap the standard response envelope.
- Add explicit methods matching section 4.
- Remove undocumented 404 fallback endpoint chains from `checkin.service.ts` after the
  new Swagger contract is available.
- Add service tests for request URLs/bodies and response normalization.

Exit: `npx.cmd tsc -p tsconfig.app.json --noEmit` and service tests pass.

### F2. Quick onboarding workflow

- Convert onboarding into sections for guardian, safety/consent, dynamic child rows,
  visit purpose, and trial status.
- Before submit, find by normalized phone or handle backend `409` by offering to open
  the existing customer.
- Require waiver for physical visits, but keep marketing consent independent.
- For `COMPLIMENTARY_TRIAL`, call the transactional onboarding endpoint and navigate to
  `/admin/checkin?customerId=<id>`.
- For `BUY_PACKAGE_NOW`, navigate to purchases with `customerId` after creation.
- Always clear loading in next/error paths and call `detectChanges()` where required by
  repository guidance.

Exit: new and existing family paths, multiple kids, validation, duplicate phone, API
error, and dark/light rendering tests pass.

### F3. Customer detail and list

- Display lifecycle, trial eligibility/status, paid balance, active entitlements, visit
  history, and purchase history without mixing complimentary sessions into revenue.
- Add actions: check in, issue eligible trial, purchase, manager comp, and follow-up.
- Disable actions with a visible backend reason rather than hiding them.

Exit: staff can understand the customer's current state without visiting another page.

### F4. Check-in workflow

- Load customer, active entitlements, eligibility, and active visits.
- Show funding source and expiry; allow multiple eligible children.
- Send the selected visit type/entitlement and display backend conflict messages.
- Show entitlement allocation in active check-ins.
- Add cancel check-in action with confirmation/reason and refresh all affected state.

Exit: trial, paid, multi-kid, insufficient balance, expired trial, duplicate check-in,
cancel, and retry cases work.

### F5. Checkout conversion workflow

- Load checkout preview when an active visit is selected.
- For a trial, present package comparison and three explicit actions: buy and checkout,
  follow up, or decline/checkout.
- Reuse purchase API/form logic without duplicating it in the component; extract a
  focused shared purchase flow/service if necessary.
- If purchase succeeds but checkout fails, preserve the purchase id and present a safe
  retry checkout action.
- Refresh active visits, customer entitlements/balance, purchases, and lifecycle after
  completion.

Exit: the partial-failure case is recoverable without a duplicate payment or purchase.

### F6. Reports and enquiries

- Add trial funnel summary and conversion table with date/branch/source/staff filters.
- Add enquiry list/create/edit/detail/convert screens after core conversion is stable.
- Customer conversion should navigate to onboarding/customer detail with server-returned
  identity, never copied form identity.

Exit: displayed totals match backend seeded results, pagination and empty/error/loading
states work, and both themes are readable.

### F7. Frontend completion gate

- Run `npx.cmd tsc -p tsconfig.app.json --noEmit`.
- Run relevant unit tests and the available build command.
- Run the repository's demo smoke checklist plus section 7 below.
- No successful API response leaves a spinner active.
- No new component hardcodes theme-breaking colors.
- Remove debug raw-response UI and obsolete compatibility fallbacks before release.

## 7. End-to-end acceptance scenarios

1. New family, one child, waiver accepted: trial issued, checked in, checked out, no
   purchase, outcome recorded.
2. New family, two children: one family trial funds the same visit correctly and is not
   consumed twice.
3. Existing family with unused eligible trial: no duplicate customer is created.
4. Family that used a trial: second trial is rejected with a clear reason.
5. Trial customer buys a package at checkout: purchase has revenue and attribution;
   trial remains complimentary and conversion reporting increments once.
6. Purchase succeeds and checkout request times out: retry completes without duplicate
   purchase or entitlement consumption.
7. Check-in is cancelled: reservation is released and audit history remains.
8. Expired trial cannot check in; manager can issue a separately classified manual comp
   with an audited reason.
9. Paid package customer continues through the existing flow without regression.
10. Staff from another branch cannot view or mutate unauthorized operational records.
11. Same onboarding request submitted twice creates one customer/trial only.
12. Concurrent trial issuance and concurrent checkout attempts cannot double grant or
    double consume.
13. Enquiry converts to an existing matching customer without duplication.
14. Trial funnel totals reconcile with the underlying entitlement, check-in, purchase,
    and outcome records.

## 8. Release sequence

Release behind feature flag `trialConversionFlow`:

1. Deploy additive database migration and backend compatibility code.
2. Deploy new backend APIs with feature disabled for ordinary staff.
3. Export Swagger and complete frontend integration.
4. Enable for one test branch and execute all acceptance scenarios.
5. Reconcile balances, purchases, trials, and conversion report daily during pilot.
6. Enable remaining branches.
7. Remove compatibility balance mutations/fallback endpoints only after the migration
   period and data reconciliation are complete.

Rollback must disable the feature/UI without rolling back migrations or deleting new
transaction records. Database migrations should be forward-only.

## 9. Instructions to start each VS Code implementation

### Backend instance prompt

> Implement the backend track in `docs/trial-conversion-implementation-plan.md`, starting
> with B1 and proceeding in order. Preserve existing APIs, use the repository's existing
> architecture and migration framework, and keep Swagger current. Do not implement UI.
> Stop at each phase exit gate to run tests and report changed endpoints/schema. Treat
> entitlement transactions as the source of truth and make issue/reserve/consume and
> checkout idempotent and concurrency-safe.

### Frontend instance prompt

> Implement the Angular track in `docs/trial-conversion-implementation-plan.md`, starting
> with F1 and proceeding in order. Follow `AGENTS.md`, preserve existing working flows,
> use `/api/v1`, normalize response envelopes in services, and validate with
> `npx.cmd tsc -p tsconfig.app.json --noEmit`. Build only against the shared contract or
> updated `swagger.json`; keep backend business rules out of Angular. Do not modify the
> Spring Boot repository.

