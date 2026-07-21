# PlayVille CRM Birthday Booking Guide

## 1. Purpose of the birthday module

The Birthday Booking module manages the complete journey from the first customer enquiry to the final party invoice and event completion.

It brings together:

- Birthday calendar and slot availability
- Customer and birthday-child selection
- Birthday packages
- Guest counts
- Food boxes
- Add-ons and extras
- Decoration packages
- Server-calculated quotes and GST
- Inventory reservation
- Advance and balance payments
- Invoice generation
- Rescheduling and cancellation
- Event-day actuals
- Audit history

## 2. Main birthday screens

| Screen | Purpose |
|---|---|
| Birthday Calendar | View bookings by date and open booking details |
| Birthday Enquiries | Review birthday enquiries separately when enabled |
| New Birthday Booking | Build a quote and create a booking |
| Birthday Settings | Configure branch rates and policies |
| Decoration Packages | Create and manage decoration options |
| Birthday Offerings | Configure food, add-ons, extras, prices and inventory links |
| Booking Detail | Review schedule, customer, financials, inventory and event status |
| Invoice Detail | Review and collect payments against the birthday invoice |

## 3. Roles and responsibilities

### Admin

- Creates global birthday offerings.
- Links offerings to inventory SKUs.
- Defines inventory behaviour, base prices, tax rates and lead times.
- Maintains product/SKU master data when physical stock is required.
- Reviews audit history.

### Branch manager

- Configures branch-specific birthday policies and rates.
- Overrides offering display names and prices for the branch.
- Controls whether an offering is available at the branch.
- Controls whether linked stock is reserved.
- Manages decoration packages.
- Reviews calendar conflicts, inventory and payments.

### Branch staff

- Creates enquiries and bookings.
- Selects customer, child, package, food and extras.
- Shares quotes and invoices.
- Records payments.
- Confirms, reschedules, cancels and completes permitted bookings.
- Records event-day actuals and notes.

## 4. One-time setup before accepting bookings

### Step 1: Configure birthday packages

A birthday package supplies the main commercial structure:

- Package code and name
- Base price
- Included number of kids
- Included number of adults
- Included duration
- Extra-kid price
- Extra-adult price

Only active birthday packages appear in the booking workspace.

### Step 2: Configure branch birthday policy

Open **Birthday Settings** and configure:

- **Extra kid price** — branch rate for kids above the package allowance.
- **Extra adult price** — branch rate for adults above the package allowance.
- **Extra time, 30 minutes** — charge for additional event time.
- **Separate enquiry calendar** — controls whether enquiries appear in their own calendar view.
- **Cancellation policy** — branch rules displayed or used operationally for cancellation decisions.
- **Refund policy** — branch rules for advance/payment refunds.
- **Preferred share channel** — intended default channel for sharing booking information.
- **Share configuration** — non-secret operational sharing configuration.

Do not place provider passwords, API keys or other secrets in policy fields.

### Step 3: Configure birthday offerings

Open **Birthday Offerings**. An offering can be one of:

- `FOOD` — food-box components such as samosa, juice or sandwich.
- `ADD_ON` — optional party add-ons.
- `EXTRA` — extra services or chargeable requirements.

Each offering contains:

| Field | Meaning |
|---|---|
| Item code | Unique permanent code, for example `FOOD-SAMOSA` |
| Item name | Staff/customer-facing name |
| Category | Food, add-on or extra |
| Description | What is included |
| Unit label | Piece, plate, box, bottle, etc. |
| Unit price | Default price per unit |
| Tax rate | Applicable GST percentage |
| Inventory SKU | Optional link to a physical inventory SKU |
| Inventory mode | None, optional or required |
| Minimum order quantity | Smallest permitted order |
| Lead-time days | Minimum operational preparation notice |
| Active | Whether the master offering remains usable |

### Step 4: Configure the offering for each branch

Admin or Manager can configure:

- Display-name override
- Unit-price override
- Available at branch
- Reserve inventory

The branch override takes precedence over the default offering price.

Inventory reservation can only work when the offering is linked to a valid SKU with branch stock.

### Step 5: Configure decoration packages

Open **Decoration Packages** and capture:

- Package code
- Package name
- Description of included decoration
- Price
- Tax rate
- Active status

Examples:

- Classic Balloon Setup
- Spider-Man Theme
- Princess Theme
- Premium Stage Decoration

Deactivate packages that are no longer sold. Historical bookings remain unchanged.

## 5. Customer and child prerequisites

Before creating a booking:

1. The parent/customer must exist in CRM.
2. The birthday child must exist under that customer.
3. The selected child must belong to the selected customer.
4. Customer phone and email should be complete for quote, invoice and notification use.

Use **Onboard Customer** or the customer profile to correct missing details before confirming the booking.

## 6. Complete booking workflow

### Stage 1: Receive the enquiry

Capture the customer's initial requirement:

- Parent/customer
- Birthday child
- Preferred date
- Preferred start time
- Expected kids and adults
- Package preference
- Food preference
- Decoration requirement
- Cake requirement
- Notes or special instructions

If the separate enquiry calendar is enabled, enquiry-stage records can be reviewed independently from confirmed events.

### Stage 2: Check slot availability

Select the party date and start time. The system calculates a standard slot end time using the configured implementation duration of **2 hours 30 minutes**.

The CRM prevents overlapping confirmed birthday bookings for the same branch and time interval.

An enquiry may coexist while the customer is deciding, but confirmation requires the slot still to be available.

### Stage 3: Select party and guests

In **New Birthday Booking**, select:

- Customer
- Birthday child
- Party date
- Start time
- Number of kids
- Number of adults
- Birthday package

The server compares the guest counts with the package allowances and calculates:

- Included kids
- Included adults
- Extra kids
- Extra adults
- Applicable extra-guest charges

### Stage 4: Build kids' and adults' food boxes

Food boxes are configured independently for kids and adults.

For each box type, capture:

- Number of boxes
- Food components
- Quantity of each component per box

Example:

`10 kids' boxes × 1 samosa per box = 10 samosas`

The quote engine calculates the total component quantities and line amounts. If a food offering is inventory linked and reservation is enabled, the calculated total quantity becomes the reservation requirement.

### Stage 5: Add optional add-ons and extras

Select configured offerings and quantities.

Examples:

- Return gifts
- Mascot appearance
- Photography
- Extra game session
- Additional beverages
- Party host

Minimum order quantity, branch availability and lead-time requirements should be considered before confirmation.

### Stage 6: Select decoration and capture event brief

Select an active decoration package or choose no priced decoration.

Capture operational notes such as:

- Cake flavour and weight
- Eggless requirement
- Theme preference
- Child's preferred name on backdrop
- Allergy information
- Special assistance
- Outside vendor information
- Arrival or setup instructions

Operational notes should be clear enough for another staff member to run the event.

### Stage 7: Calculate the quote

The quote is calculated on the server so staff cannot manipulate totals in the browser.

The quote contains itemized lines for:

- Base birthday package
- Extra kids
- Extra adults
- Kids' food boxes
- Adults' food boxes
- Add-ons
- Decoration package
- Extras

For each line the CRM retains:

- Description
- Quantity
- Unit price
- Tax rate
- Line total
- Linked catalog item/SKU where applicable
- Whether inventory will be reserved at confirmation

The summary shows:

- Subtotal
- GST/tax total
- Grand total

Review guest counts and every line with the customer before saving the booking.

### Stage 8: Create the booking

When saved, the booking records an immutable commercial snapshot of the calculated quote rather than depending only on future configuration values.

The initial status is normally **Enquiry**.

The booking stores:

- Branch
- Customer and child
- Staff attribution
- Date and slot
- Expected guests
- Cake and notes
- Quoted financial amounts
- Advance amount and payment details, when provided
- Quote lines
- Inventory requirements

### Stage 9: Issue or share the invoice

The birthday invoice can be created from the booking.

It contains the birthday quote lines and links back to the booking. Customer and seller/branch invoice details are taken from CRM snapshots.

Staff can:

- Open the invoice
- Print or download the invoice PDF
- Share it using configured email/WhatsApp notification channels
- Record payments

### Stage 10: Record advance payment

Capture:

- Payment amount
- Payment mode: Cash, UPI, Card or Online
- Provider/payment reference where applicable
- Idempotency key generated by the UI/API client

The payment updates:

- Amount paid
- Balance due
- Invoice payment status

The advance cannot exceed the booking total. Payment references are strongly recommended for UPI, Card and Online transactions.

### Stage 11: Confirm the booking

Move the booking from **Enquiry** to **Confirmed** only after operational and commercial review.

Confirmation performs two critical checks:

1. The party slot must still be available.
2. Required linked inventory must be available.

When successful, inventory-enabled quote lines reserve their required SKU quantities. Reserved stock is no longer available for unrelated sales.

If inventory is insufficient, confirmation is rejected and the booking remains unconfirmed. Manager should receive stock, adjust the offering, or agree on an alternative with the customer.

### Stage 12: Prepare before the event

Manager and staff should review upcoming confirmed bookings for:

- Payment balance
- Guest counts
- Food quantities
- Inventory reservation status
- Decoration package
- Cake requirement
- Vendor/party notes
- Staffing
- Lead-time commitments
- Customer contact information

Do not remove reserved birthday stock through manual adjustments unless the booking is corrected or cancelled first.

### Stage 13: Reschedule when necessary

Enquiry and Confirmed bookings can be rescheduled.

Enter the new date and start time. The standard slot end is recalculated.

For a confirmed booking, the new slot must be free. Completed and Cancelled bookings cannot be rescheduled.

After rescheduling, staff should recheck customer communication, supplier lead times, stock expiry and decoration availability.

### Stage 14: Event-day operation

Open the booking detail and verify:

- Correct customer and child
- Scheduled time
- Actual arrival
- Actual kids
- Actual adults
- Extra time used
- Food and add-ons supplied
- Decoration delivered
- Pending payment
- Completion notes

Any additional charge should be properly itemized and invoiced rather than hidden in free-text notes.

### Stage 15: Complete the booking

Record:

- Actual kids
- Actual adults
- Actual extra minutes
- Completion notes

Completion consumes the inventory reserved for the booking and changes status to **Completed**.

A Cancelled booking cannot be completed. If an Enquiry is completed directly, the service attempts to reserve and consume required inventory as part of completion; operationally, normal confirmation before the event is strongly recommended.

### Stage 16: Collect final balance

Record the remaining payment against the birthday invoice using the actual payment mode and reference.

The invoice should show:

- Grand total
- Advance/previous payments
- Final payment
- Balance due
- Paid or partially paid status

Branch closing review should identify completed events with any remaining balance.

## 7. Booking statuses

| Status | Meaning | Permitted next action |
|---|---|---|
| Enquiry | Customer is considering the party | Confirm or Cancel |
| Confirmed | Slot and required inventory are committed | Complete through the completion action, Reschedule or Cancel |
| Completed | Event has taken place | Terminal status |
| Cancelled | Booking will not take place | Terminal status |

Invalid backward transitions are blocked. For example, a Completed or Cancelled booking cannot return to Confirmed.

## 8. Inventory reservation lifecycle

### No inventory

Use for pure services such as a party host or photography.

### Optional inventory link

Offering may be linked to an SKU, while branch decides whether reservation is needed.

### Required inventory

Offering is expected to use a linked SKU and reserve physical stock.

Lifecycle:

1. Quote calculates required quantity.
2. Booking remains Enquiry without consuming stock.
3. Confirmation reserves stock.
4. Cancellation releases reserved stock.
5. Completion consumes reserved stock and creates inventory movement history.

Example:

`20 food boxes × 1 juice bottle = 20 JUICE-200ML reserved`

## 9. Cancellation workflow

Before cancelling, staff should:

1. Confirm the customer request and reason.
2. Review the configured cancellation policy.
3. Review payments and refund eligibility.
4. Record clear operational notes.
5. Change status to **Cancelled**.

Cancellation releases birthday inventory reservations. Financial refund processing should follow the branch refund policy and invoice/payment controls.

Cancellation does not delete the booking; the history remains available for audit and reporting.

## 10. GST and pricing

Each quote line carries its price and tax rate. This allows package, food, add-ons and decoration to use appropriate rates.

Important rules:

- Use branch price overrides only when approved.
- Do not calculate totals manually outside the server quote.
- Confirm whether configured prices include tax.
- Ensure branch GST identity is configured for tax invoices.
- Preserve payment references for reconciliation.
- Consult the business accountant for tax classification decisions.

## 11. Calendar and conflict rules

- Calendar results are branch-specific.
- Confirmed bookings cannot overlap.
- Slot comparison uses actual start and end intervals, not only identical start times.
- Cancelled bookings do not block the slot.
- Enquiries can be displayed separately based on branch policy.
- Always rerun availability before confirmation or confirmed rescheduling.

## 12. Audit trail

Important birthday actions are recorded centrally, including:

- Offering creation and update
- Branch offering configuration
- Decoration package changes
- Policy changes
- Booking creation
- Status change
- Confirmation
- Cancellation
- Rescheduling
- Completion
- Invoice creation
- Payment recording
- Invoice sharing

Admin and Manager can review these events under **Audit Report**. Audit records cannot be edited or deleted through CRM APIs.

## 13. Recommended operational checklist

### At enquiry

- Customer and child verified
- Date/time preference captured
- Guest estimate captured
- Package explained
- Contact details complete

### Before confirmation

- Quote reviewed with customer
- Slot available
- Advance recorded
- Payment reference captured
- Inventory available
- Decoration available
- Policies communicated

### One to three days before event

- Customer reconfirmed
- Guest counts reconfirmed
- Food quantities checked
- Stock reservation checked
- Decoration and vendors confirmed
- Staff assigned
- Balance reminder sent

### Event day

- Actual attendance recorded
- Food/add-ons delivered
- Extra time captured
- Exceptions documented
- Final payment collected
- Event completed in CRM

### End-of-day manager review

- Completed status verified
- Invoice/payment balance checked
- Inventory consumed correctly
- Cancellation/refund exceptions reviewed
- Audit events reviewed when necessary

## 14. Troubleshooting

### Package does not appear

Confirm the birthday package is active and correctly configured.

### Food/add-on/extra does not appear

Confirm:

1. Offering is active.
2. Offering is available for the current branch.
3. Lead-time/minimum quantity requirements are satisfied.
4. If inventory is required, a valid SKU is linked.

### Decoration package does not appear

Confirm it is active and belongs to the current branch configuration.

### Booking cannot be confirmed

Likely causes:

- Another confirmed booking overlaps the slot.
- Required inventory is insufficient.
- Linked SKU is inactive or unavailable at the branch.
- A required booking field is invalid.

### Invoice has incorrect customer or seller information

Review customer contact data and Branch Settings before issuing the invoice. Issued invoices use snapshots to preserve historical accuracy.

### Inventory reservation looks wrong

Review quote quantities, quantity-per-box calculations, linked SKU, branch reserve-inventory setting and current reservation movements.

### Booking cannot be rescheduled

Completed and Cancelled bookings are terminal. A Confirmed booking also requires the new slot to be available.

### Balance is still due after payment

Verify the payment was recorded against the birthday invoice, used the correct amount and was not entered only as a note.

