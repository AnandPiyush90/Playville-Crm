# PlayVille CRM Inventory Management Guide

## 1. What is a product?

A **product** is the general item PlayVille sells or consumes, for example:

- Hot Coffee
- Coca-Cola
- PlayVille Socks
- Chips

The product holds common information such as its name, category and description.

## 2. What is an SKU?

**SKU** means **Stock Keeping Unit**. It is the unique code used to identify one exact sellable or stock-controlled variation of a product.

Example:

| Product | SKU | Meaning |
|---|---|---|
| Coca-Cola | COKE-250ML | 250 ml bottle |
| Coca-Cola | COKE-750ML | 750 ml bottle |
| PlayVille Socks | SOCKS-S-BLUE | Small, blue pair |
| PlayVille Socks | SOCKS-M-PINK | Medium, pink pair |

Why SKUs matter:

- Stock is maintained against the SKU, not only the product name.
- Different sizes or variants can have different prices and costs.
- Receipts, transfers, expiry batches and sales refer to the exact SKU.
- Reports can show which exact variation was sold or is running low.
- An SKU code must be unique across the CRM.

Recommended SKU format: `PRODUCT-VARIANT-SIZE`, using uppercase letters, numbers and hyphens. Examples: `COFFEE-REG`, `COKE-250ML`, `SOCKS-M-BLUE`.

For a product with only one variation, one simple SKU is enough. Example: product **Hot Coffee**, SKU **COFFEE-REG**.

## 3. How does a barcode help?

A barcode is a machine-readable identifier attached to a physical item. A barcode scanner behaves like a keyboard and enters the barcode into the CRM.

Benefits:

- Faster selection during checkout and stock receipt.
- Fewer mistakes between similar products or sizes.
- Exact SKU identification.
- Easier physical stock counting.
- Existing manufacturer barcodes can normally be reused.

Barcode is optional, but when entered it must be unique. Products such as packaged cold drinks, chips and apparel usually benefit from barcodes. A prepared hot coffee may not need one.

## 4. Product fields

### Product code — required

A short, permanent business code for the product. Example: `COFFEE`, `COKE`, `SOCKS`.

Use a stable code. Do not use the price or current branch in the code.

### Product name — required

The customer-facing and staff-facing name. Example: `Hot Coffee with Milk`.

### Category — required

Groups products in inventory and in the visit checkout selector. Recommended categories:

- Beverages
- Snacks
- Apparel
- Party Supplies
- Merchandise

Admin can create, edit, deactivate and reactivate categories from **Inventory → Products → Product categories**.

### Description — optional

Additional explanation for staff. Example: `Regular hot coffee with milk, served in one cup`.

### Track inventory

Choose how the item should be managed:

- **Track stock** — use for cold drinks, packaged snacks, socks and merchandise. Enter opening stock during product creation or receive stock later. Checkout only shows the item when quantity is available.
- **Non-stock sale item** — use for prepared coffee, tea or similar items where PlayVille charges by quantity but does not maintain stock. These items remain available during a visit without stock receipts or deductions.

A prepared item may still use **Track stock** when the branch deliberately counts cups or servings.

## 5. SKU fields

### SKU code — required

Unique code for the exact variation. Example: `COFFEE-REG`.

### Barcode — optional

Unique manufacturer or PlayVille barcode used for scanning.

### Unit — required

How stock is counted:

- `PIECE` — one snack or one cup
- `BOTTLE` — cold drinks or water
- `PACK` — packaged goods
- `PAIR` — socks

Use one consistent unit for receiving, transferring and selling the SKU.

### Base sale price — required

Default customer selling price. This becomes the fallback price when the branch has no override.

### Cost price — optional but strongly recommended

The business purchase cost per unit. It supports margin and stock-value reporting. It is not shown to the customer.

### GST profile

Select the applicable configured tax profile. The profile contains the GST rate and whether the sale price includes tax.

Use `No GST` only when the item genuinely has no mapped tax profile. GST decisions should follow the business accountant's guidance.

### Branch sale price

Optional price override for the current branch. If blank, the base sale price is used.

Example: base price ₹60; Whitefield branch override ₹70.

### Expiry tracked

Enable for products managed by batch and expiry date, such as packaged food or beverages.

When enabled:

- Receipts should contain batch number and expiry date.
- The CRM sells from valid batches.
- Expired stock should not be sold.
- Expiry alerts can be reviewed in Inventory.

### SKU active

Controls whether this variation can be used. Deactivate discontinued variants instead of deleting historical data.

### Available at branch

Controls whether the SKU may be sold by the current branch. A product can be globally active but unavailable at a particular branch.

## 6. Creating an item from start to finish

Example: create **Hot Coffee**.

1. Open **Inventory → Products**.
2. Create or select category **Beverages**.
3. Enter product code `COFFEE`.
4. Enter product name `Hot Coffee`.
5. Choose **Non-stock sale item** for normal prepared coffee, or **Track stock** if cups/servings will be counted.
6. Enter SKU code `COFFEE-REG`.
7. Select unit `PIECE`.
8. Enter base sale price and cost price.
9. Select the correct GST profile.
10. Enable **SKU active** and **Available at branch**.
11. For a stock-tracked item, enter **Opening stock** while creating it. Expiry-tracked items must be received from **Inventory → Receipts** so batch and expiry details are recorded.
12. Save the product.

An active non-stock SKU appears during visit checkout without inventory quantity. A stock-tracked SKU appears only when it is active, available at the branch and has positive available stock.

## 7. How stock quantities work

| Quantity | Meaning |
|---|---|
| On hand | Physical quantity currently owned by the branch |
| Reserved | Quantity temporarily committed to an active visit, transfer or booking |
| Available | On hand minus reserved quantity |
| Reorder level | Quantity at which the branch should purchase more |

Example: 20 bottles on hand and 3 reserved means 17 bottles available.

## 8. Receiving and adjusting stock

### Receipts

Use **Inventory → Receipts** when stock arrives from a supplier. Capture:

- Supplier
- Supplier invoice reference
- SKU
- Quantity
- Unit cost
- Batch number, when relevant
- Expiry date, when relevant
- Notes

Receipt is the preferred way to add purchased stock because it preserves the supplier and cost trail.

### Stock adjustments

Use **Inventory → Stock** for controlled corrections:

- `OPENING_STOCK` — initial quantity during setup
- `ADJUSTMENT_IN` — verified stock increase
- `ADJUSTMENT_OUT` — verified stock decrease
- `WASTAGE` — damaged or consumed stock
- `EXPIRY` — expired stock removed

Reason and notes should explain why the adjustment was necessary. Important operations are recorded in the Audit Report.

## 9. Suppliers

Use **Inventory → Suppliers** to create and maintain vendors. Recommended information:

- Supplier name
- Phone
- Email
- GST/tax number

Deactivate suppliers that are no longer used instead of removing their historical receipts.

## 10. Branch transfers

Use **Inventory → Transfers** when stock moves between branches.

Flow:

1. Source branch creates a draft transfer.
2. Manager reviews SKU, quantity and destination.
3. Source branch dispatches it.
4. Destination branch receives it.
5. Variances are recorded if received quantity differs.

Stock should not be adjusted manually at both branches to imitate a transfer; use the transfer workflow so both ledgers remain connected.

## 11. Visit-item checkout workflow

1. Staff checks in the customer normally.
2. Under **Active check-ins → Visit items**, staff selects an item by category.
3. Staff enters quantity and clicks **Add**.
4. The CRM reserves stock immediately.
5. Staff can update the quantity or remove the item before checkout.
6. Removing the item releases its reservation.
7. At checkout, the CRM prepares an itemized invoice.
8. Staff collects the full payment.
9. Invoice finalization converts reserved stock into a sale.
10. The visit is completed only after successful payment.

The invoice preserves the product name, SKU, unit price and GST snapshot used when the item was assigned.

## 12. Birthday booking inventory

Birthday food, add-ons or extras may be linked to inventory SKUs. When reservation is enabled:

- Confirming a booking reserves the required stock.
- Cancelling releases the reservation.
- Completion consumes the reserved quantity.
- The birthday offering remains the customer-facing selection, while the SKU maintains physical stock.

## 13. Deactivation versus deletion

PlayVille uses safe deactivation for products, SKUs, categories and suppliers.

Deactivation prevents future use but retains:

- Past invoices
- Stock movements
- Supplier receipts
- Transfers
- Audit history

This is safer than physical deletion and is necessary for financial and operational traceability.

## 14. Recommended role responsibilities

| Role | Responsibility |
|---|---|
| Admin | Categories, product/SKU master, GST mapping and global configuration |
| Manager | Branch prices, stock receipts, adjustments, suppliers, transfers and reports |
| Branch staff | Assign visit items, update quantities, remove items and collect checkout payment |

## 15. Daily operational checks

Branch manager should review:

- Low-stock items
- Out-of-stock items
- Reserved quantities
- Upcoming expiry
- Unreceived transfers
- Receipt and adjustment accuracy
- Paid invoices versus inventory sales
- Important changes in **Audit Report**

## 16. Quick troubleshooting

### Item does not appear during checkout

Confirm that:

1. Product is active.
2. Product type is retail.
3. SKU is active.
4. SKU is available at the branch.
5. Positive available stock exists.
6. A sale price is configured.

### Category dropdown is empty

Create an active category from **Inventory → Products → Product categories**.

### Stock is less than expected

Review reserved quantity, visit carts, birthday reservations, transfers, wastage, expiry and the inventory movement history before entering a manual adjustment.
