# Birthday offerings and inventory workflow

## Roles

- **Admin** owns the master offering: code, category, name, base price, GST, unit, lead time, minimum order, active state, and optional SKU link.
- **Manager** owns the current branch controls: customer-facing name, branch price override, staff availability, and whether linked stock is reserved.
- **Staff** can only select active offerings approved for their branch. Quote options show branch price, stock visibility, and lead time.

## Inventory lifecycle

1. An enquiry saves immutable quote-line snapshots without reserving inventory.
2. Confirmation locks each affected SKU balance and reserves the quoted quantity.
3. Cancellation releases every open reservation.
4. Completion reduces reserved and on-hand stock together, consumes valid batches FEFO, and writes `SALE` movements referencing the birthday booking.
5. If stock is insufficient, confirmation fails atomically and the booking remains an enquiry.
6. Existing historical quote and invoice lines remain valid even when catalogue settings change.

## Offering types

- `NONE`: service or non-stock item, such as an artist or extra time.
- `OPTIONAL`: linked to a SKU, but each branch decides whether to reserve it.
- `REQUIRED`: linked to a SKU and enabled for reservation by default on newly created offerings.

## Acceptance criteria

- Admin can create and edit global birthday offerings and cannot enable inventory behavior without a SKU.
- Manager cannot edit master tax/SKU data but can configure the current branch.
- Staff never sees disabled master or branch offerings.
- Quote calculations always use current branch price and save it as a snapshot.
- Minimum quantity, lead time, and aggregate stock availability are validated server-side.
- Confirm, cancel, and complete actions are transactional with inventory changes.
- Booking detail reports `NOT_REQUIRED`, `RESERVED`, `RELEASED`, or `CONSUMED` inventory state.
