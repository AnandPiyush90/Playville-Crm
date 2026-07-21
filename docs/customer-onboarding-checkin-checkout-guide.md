# PlayVille CRM Customer Onboarding, Purchase, Check-in and Checkout Guide

## 1. Purpose

This guide explains the complete customer journey in PlayVille CRM:

1. Create the parent/customer profile.
2. Create one or more child profiles.
3. Record consent and visit information.
4. Issue a complimentary trial or sell a paid package.
5. Check the family into the branch.
6. Assign snacks, drinks or merchandise during the visit.
7. Create an invoice and collect payment for visit items.
8. Complete checkout and deduct sessions accurately.
9. Retain purchase, entitlement, inventory and audit history.

## 2. Main screens

| Screen | Purpose |
|---|---|
| Customers | Search and review existing families |
| Onboard Customer | Create parent and child records in one guided workflow |
| Customer Detail | Review contact details, kids, package, balance and history |
| Kids | Add, update or deactivate child profiles |
| Packages | Review available PlayVille session packages |
| Purchases | Sell a package and add paid sessions |
| Check-in / Check-out | Start visits, manage active visits and complete checkout |
| Invoices | Review package and visit-item invoices and payments |
| Inventory | Configure and maintain snacks, drinks and merchandise |
| Audit Report | Review important actions performed by staff |

## 3. Roles and responsibilities

### Admin

- Configures branches, packages, product/SKU master and policies.
- Manages staff access.
- Reviews cross-branch audit activity.

### Branch manager

- Reviews customer, purchase, check-in and checkout operations.
- Manages branch stock, prices, receipts and adjustments.
- Reviews branch audit history and exceptions.

### Branch staff

- Onboards customers and kids.
- Records package purchases and payments.
- Checks eligible kids in.
- Assigns visit items.
- Collects checkout payment and completes the visit.
- Records clear cancellation and checkout notes.

## 4. Before creating a new customer

Search by mobile number or parent name first. The mobile number is the primary duplicate-control field.

Do not create another customer record when:

- The parent already exists with a different spelling.
- A child needs to be added to an existing family.
- Contact details have changed.
- The customer is visiting another PlayVille branch.

Use the existing customer profile and update it when appropriate.

## 5. Complete customer onboarding workflow

### Step 1: Select the visit purpose

The onboarding workflow can direct staff to the correct next action.

Typical purposes:

- **Complimentary trial** — creates an eligible trial entitlement and proceeds to check-in.
- **Buy package now** — creates the family and proceeds to package purchase.
- **Customer detail / enquiry** — creates the record without starting a physical visit.

Physical-visit purposes require waiver/disclaimer acceptance.

### Step 2: Enter parent/customer details

#### Parent name — required

Enter the name of the parent or legal guardian responsible for the child.

#### Mobile number — required

- Must contain 10 to 15 digits in the guided onboarding flow.
- Formatting characters are removed before saving.
- The number must be unique.
- Use the number the branch can contact during the visit.

#### Email — optional but recommended

Used for invoices, receipts and notifications. Enter a valid address when available.

#### Lead source

Identifies how the family discovered PlayVille, for example walk-in, referral, campaign or social media. This supports enquiry and conversion reporting.

#### Emergency contact name and phone

Capture an alternative responsible contact for physical visits.

#### Marketing consent

Record only the customer's explicit permission to receive marketing communication. It is separate from operational invoice or booking messages.

#### Disclaimer/waiver acceptance

For a physical visit:

- Acceptance is mandatory.
- The accepted version should be recorded.
- CRM stores the acceptance timestamp.

Staff must not select acceptance without the parent actually agreeing.

### Step 3: Add at least one child

The guided onboarding flow requires one or more kids.

For each child capture:

#### Child name — required

Use the name staff should use during the visit.

#### Date of birth — required

- Must be a valid past date.
- Current check-in eligibility is for children aged **0 to 8 years**.
- Age is calculated from date of birth; staff should not enter a manually estimated age.

#### Gender — optional

Capture only when provided and operationally relevant.

#### Special notes — optional but important

Examples:

- Allergy information
- Accessibility requirements
- Sensory considerations
- Medical or behavioural instructions supplied by the parent

Use factual, respectful and operationally relevant language.

### Step 4: Trial information, when applicable

For a complimentary trial, staff may record:

- Campaign code
- Trial notes

CRM issues a family trial entitlement and returns **Check in** as the next action.

### Step 5: Save onboarding safely

Onboarding uses an idempotency key. If the browser retries the same request, CRM returns the already-created result instead of creating duplicate customers and kids.

After saving, verify:

- Parent contact details
- Child names and dates of birth
- Waiver status
- Home/first-visit branch
- Trial entitlement or recommended next action

## 6. Creating or maintaining children later

Use the existing customer profile or **Kids** screen to:

- Add another child to the family.
- Correct a child name.
- Correct date of birth.
- Update gender or special notes.
- Deactivate a child who should no longer be selectable.

Do not delete historical child records that appear in past check-ins. Deactivation preserves visit history.

## 7. Packages and sessions

A PlayVille package normally contains:

- Package name
- Number of sessions
- Bonus sessions, when applicable
- Validity or package terms
- Total selling price
- Active status

The customer's **global session balance** is the paid-session balance available for standard visits.

If two kids are checked in during one paid visit, two sessions are required and two sessions are deducted at checkout.

## 8. Complete package-purchase workflow

### Step 1: Select the customer

Confirm the correct parent and mobile number before recording payment.

### Step 2: Select an active package

Only sell the package agreed with the customer. Inactive packages cannot be purchased.

### Step 3: Capture payment

Required information:

- Package
- Payment mode: Cash, UPI, Card or Online
- Payment reference for UPI, Card or Online payments
- Approved discount, when applicable
- Notes, when needed

### Step 4: Submit with an idempotency key

Purchase creation is idempotent. Retrying the same submission must not add sessions twice.

### Step 5: CRM updates the account

The purchase records:

- Customer and branch
- Staff member
- Package
- Sessions added
- Balance before and after
- Amount paid
- GST portion
- Discount
- Payment mode and reference
- Purchase context

CRM then:

- Adds sessions to the global balance.
- Sets the current package.
- Records the purchase branch.
- Creates or reuses the linked invoice/receipt.

### Trial conversion purchase

When a package is purchased during trial checkout, the purchase also references:

- Source check-in
- Source trial entitlement
- Trial checkout purchase context

This provides conversion reporting and prevents repeated purchase creation for the same trial check-in.

## 9. Check-in prerequisites

Before checking in, verify:

- Customer is active.
- Selected kids belong to that customer.
- Kids are active and age eligible.
- Customer does not already have an active check-in.
- Paid visit has enough sessions for all selected kids.
- Complimentary trial has a valid active entitlement.
- Correct branch and staff login are being used.

## 10. Paid check-in workflow

1. Open **Check-in / Check-out**.
2. Find or load the customer.
3. Select one or more eligible kids.
4. Choose the standard paid visit type.
5. Review session balance.
6. Click **Check in**.

CRM creates:

- One active family check-in.
- A link for every selected child.
- Branch and staff attribution.
- Check-in timestamp.
- Number of kids.

Paid sessions are validated at check-in but deducted at successful checkout. This prevents silently consuming sessions for a visit that is cancelled before completion.

Only one active check-in is permitted for a customer at a time.

## 11. Complimentary-trial check-in workflow

1. Onboard or select the customer.
2. Confirm an eligible trial entitlement exists.
3. Select eligible kids.
4. Choose **Complimentary trial**.
5. Select the entitlement.
6. Click **Check in**.

CRM reserves the entitlement so it cannot be used by another concurrent visit.

At trial checkout, staff must record the commercial outcome:

- Purchased a package
- Follow-up required
- Declined/not converted
- Other configured outcome

When a package is purchased, that purchase must belong to the same customer and reference the trial check-in.

## 12. Active visit management

The active check-in list shows:

- Customer and mobile number
- Selected children
- Check-in time
- Current session balance
- Funding type
- Visit items
- Checkout notes/actions

Use **Refresh active** when another staff member may have changed the visit.

## 13. Visit-item purchase workflow

Customers may take items such as:

- Snacks
- Cold drinks
- Coffee
- Socks
- Merchandise

### Add an item

1. Open the active check-in.
2. Under **Visit items**, select a category and SKU.
3. Enter quantity.
4. Click **Add**.

The selector contains only retail SKUs that are:

- Active
- Available at the branch
- Positively stocked
- Configured with a sale price

CRM freezes the product name, SKU, price and GST snapshot and reserves the quantity immediately.

### Update quantity

Change the quantity on the visit-item row. CRM increases or releases the difference in reserved stock.

### Remove an item

Click **Remove**. The line is cancelled and its stock reservation is released.

## 14. Checkout without visit items

For a normal paid visit with no retail items:

1. Review selected kids.
2. Review any permitted manual charge and notes.
3. Click **Check out**.
4. CRM deducts one paid session for every selected child.
5. Check-in becomes **Completed**.
6. Customer visit count is updated.
7. A session-deduction ledger entry is recorded per child.

Manual charges should be used carefully. Prefer configured products or service SKUs when an itemized invoice is required.

## 15. Payment-gated checkout with visit items

When visit items exist, direct checkout is blocked until their invoice is fully paid.

Workflow:

1. Staff clicks the checkout action.
2. CRM prepares or refreshes a draft checkout invoice linked to the check-in.
3. Invoice lines must exactly match the open visit items.
4. Staff reviews customer, products, quantities, GST and total.
5. Staff selects payment mode and enters the complete payment amount.
6. Staff clicks **Collect payment & check out**.

In one controlled transaction CRM:

- Verifies full payment.
- Verifies the visit cart has not changed.
- Claims the visit's stock reservations.
- Deducts physical inventory exactly once.
- Records inventory sale movements.
- Finalizes the invoice.
- Records payment.
- Deducts paid sessions or consumes the trial entitlement.
- Completes the check-in.

If payment, stock or cart validation fails, the visit remains active and the transaction rolls back.

## 16. GST and invoice behaviour

Each visit item uses the SKU's configured tax profile. The invoice preserves:

- Seller/branch identity
- Customer name, phone and email
- Product description and SKU
- Quantity and unit price
- Taxable amount
- GST rate and amount
- Grand total
- Payments and balance

Branch GST and invoice details must be maintained in Branch Settings.

## 17. Checkout notes

Use checkout notes for concise operational information, such as:

- Parent reported an incident.
- Item was replaced before billing.
- Follow-up call requested.
- Child left early.

Do not use notes as a substitute for product lines, payments, cancellation reasons or incident-management procedures.

## 18. Check-in cancellation

Only an active check-in can be cancelled.

Staff must provide a cancellation reason.

Cancellation:

- Changes status to **Cancelled**.
- Releases open visit-item stock reservations.
- Releases a reserved complimentary-trial entitlement.
- Does not deduct paid sessions.
- Preserves the record for reporting and audit.

Use cancellation when the visit did not complete. Do not complete and then attempt to cancel historically.

## 19. Session and entitlement ledger

The CRM preserves financial and operational traceability through ledgers.

### Paid sessions

Package purchase increases global session balance. Successful paid checkout decreases it based on selected kids.

### Trial entitlement

Typical lifecycle:

`Issued → Reserved at check-in → Consumed at checkout`

If the active visit is cancelled:

`Reserved → Released`

Entitlement transactions capture session/reservation deltas, related check-in, staff, notes and occurrence time.

## 20. Cross-branch visits

A customer may use paid sessions at another branch when business rules permit.

CRM retains:

- Purchase branch
- Usage branch
- Sessions used
- Settlement rate
- Settlement amount

Staff should always operate under the branch where the visit physically occurs.

## 21. Status lifecycle

### Check-in statuses

| Status | Meaning |
|---|---|
| Active | Customer is currently inside the branch |
| Completed | Checkout succeeded |
| Auto-Closed | Visit was closed by an approved automated/manager process |
| Cancelled | Visit did not complete; reservations were released |

### Visit-item statuses

| Status | Meaning |
|---|---|
| OPEN | Assigned to active visit and reserved |
| CHARGED | Included in finalized paid invoice |
| CANCELLED | Removed from visit and reservation released |

## 22. Audit trail

Important activities are recorded in **Audit Report**, including:

- Customer creation and update
- Child creation and update
- Package purchase
- Check-in
- Visit-item create/update/remove
- Checkout and cancellation
- Invoice finalization and payment
- Inventory adjustment

Audit records include staff, branch, action, outcome, time, IP address and correlation ID. They are not editable through CRM.

## 23. Recommended operational checklist

### Onboarding

- Existing customer searched first
- Parent name and mobile verified
- Email captured when available
- Emergency contact captured
- Waiver accepted for physical visit
- Child DOB and special notes verified
- Correct visit purpose selected

### Package purchase

- Correct customer and package
- Sessions and price explained
- Discount approved
- Payment mode/reference captured
- Invoice generated
- Balance updated once

### Check-in

- Correct kids selected
- Age eligibility verified
- Sufficient paid sessions or valid trial
- No active duplicate visit
- Parent confirms visit details

### During visit

- Every retail item added immediately
- Quantity kept accurate
- Removed items removed from the cart
- Important operational notes recorded

### Checkout

- Kids and session deduction reviewed
- Visit items and GST reviewed
- Full item payment collected
- Trial outcome recorded
- Invoice available
- Visit shows Completed

### End-of-day manager review

- No unexpected long-running active check-ins
- Completed visits match session deductions
- Visit-item invoices are fully paid
- Cash/UPI/Card totals reconcile
- Inventory reservations are reasonable
- Cancellation reasons are clear

## 24. Troubleshooting

### Customer already exists

Search by normalized phone number and use the existing profile. Do not change the number merely to bypass duplicate control.

### Child cannot be checked in

Confirm the child belongs to the selected customer, is active and is within the supported age range.

### Insufficient session balance

Number of paid sessions must be at least the number of selected kids. Sell the agreed package before check-in or reduce the selected kids only when factually correct.

### Customer already has an active check-in

Refresh active visits and complete or cancel the existing visit. Do not create another family profile.

### Trial entitlement cannot be selected

Confirm it exists, is active, belongs to the customer, has not expired and is not already reserved or consumed.

### Visit item does not appear

Confirm product/SKU is active, available at the branch, has positive available stock and has a configured selling price.

### Item quantity cannot be increased

Available stock is insufficient. Receive stock, choose another SKU or keep the quantity actually available.

### Checkout says invoice required

The active visit contains retail items. Use the payment-gated checkout flow and fully pay the generated invoice.

### Checkout invoice says visit items changed

The cart was edited after draft preparation. Return to the active visit, verify items, and prepare/refresh the checkout invoice again.

### Sessions were not deducted

Verify the check-in reached Completed status. Failed payment or invoice validation leaves the visit active and does not deduct sessions.

### Stock appears reserved after cancellation

Refresh inventory and inspect the check-in/item state. Review inventory movements and Audit Report before entering a manual adjustment.

