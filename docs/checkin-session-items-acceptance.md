# Check-in session item acceptance criteria

## Staff and manager clarity

- Staff can assign an available branch SKU to any active check-in from either active-visit view.
- The visit cart shows product, SKU, frozen unit price, GST rate, quantity, line total, and assigning staff member.
- Repeated assignment of the same SKU increases one open line instead of producing ambiguous duplicates.
- Staff can correct quantity or remove an item until checkout begins.
- Manual non-inventory charges remain available and are clearly separated from itemized products.

## Inventory integrity

- Assigning or increasing an item reserves branch stock under a pessimistic inventory lock.
- An assignment exceeding available stock is rejected unless that branch SKU explicitly allows negative stock.
- Reducing/removing an item or cancelling the check-in releases its reservation.
- Successful checkout reduces on-hand and reserved quantities together and writes `SALE` movements referencing the session-item record.
- Expiry-controlled inventory is consumed FEFO from valid batches; checkout fails atomically if a valid batch is unavailable.
- A failed checkout leaves the visit active, its cart intact, and inventory reservations unchanged.

## Customer billing clarity

- Price and GST snapshots are frozen at assignment time so later catalogue changes do not alter the visit charge.
- Checkout preview returns itemized taxable amount, GST, and total.
- Checkout response retains charged item lines and combines them with any manual checkout charge.
- Existing package-session deduction and trial-conversion behavior remains unchanged.

## Security and branch isolation

- All item endpoints require authentication and enforce the current JWT branch.
- Completed, cancelled, or cross-branch visits cannot be edited.
