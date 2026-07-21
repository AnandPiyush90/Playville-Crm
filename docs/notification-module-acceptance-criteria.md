# Invoice notification acceptance criteria

## Automated acceptance

- An authenticated staff user can send a finalized invoice PDF by an enabled email channel.
- The invoice snapshot email or phone is used when the request omits a destination.
- Draft invoices and invoices belonging to another branch are rejected before delivery.
- Disabled or incomplete branch channels return an actionable business-rule error.
- Every attempted provider delivery is audited as `PENDING`, then `SENT` or `FAILED`.
- Reusing the same branch-scoped `Idempotency-Key` returns the original delivery and never sends twice.
- Branch settings reject email enablement without a sender address and WhatsApp enablement without a Meta phone-number ID.
- Only administrators can read or update branch notification configuration.

## UI acceptance

- Branch Settings exposes separate Email and WhatsApp enablement controls and validates their mandatory fields.
- Provider access tokens and SMTP passwords are never displayed or persisted by the UI.
- Invoice Detail presents explicit Email invoice and WhatsApp invoice actions.
- Share actions are unavailable for drafts, prefill available customer contact data, and display success or actionable failure feedback.
- Repeated clicks while a delivery request is running cannot submit a second delivery.

## Environment acceptance

- Email delivery is verified against the configured SMTP provider using a received PDF attachment.
- WhatsApp delivery is verified using a Meta-approved document-header template and a real test recipient.
- The database migration is applied successfully to a schema containing V9 invoice tables and the current branch/staff/customer tables.
