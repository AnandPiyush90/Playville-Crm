# Email Template — System Settings UX and API plan

## Goal

Give branch administrators a friendly, safe way to customize every customer email without changing code. The initial catalog contains Invoice Email and Disclaimer Signing. New system events can be added to the catalog without changing the settings-screen structure.

## Recommended screen

Add `System Settings > Email Templates` after Email Provider. Use one accordion card per template, based on the supplied reference, with a cleaner two-column editing experience.

Each collapsed card shows the template name, purpose, and either `Default` or `Customized`. Opening a card shows:

- Subject input with character counter.
- Plain-text message editor with character counter.
- Available Variables panel. Each variable is a clickable chip that inserts it at the cursor; its label and example appear in a tooltip.
- Live Preview tab populated with safe example values.
- `Save changes`, `Discard changes`, and `Reset to default` actions.
- Unsaved-change warning when closing the accordion or navigating away.
- Inline validation for unknown variables and empty content.
- Last updated timestamp and optimistic-conflict message if another administrator saved first.

Only one accordion should be open at a time on desktop. On mobile, the editor, variables, and preview stack vertically. Preserve drafts locally only for the current editing session; never persist email bodies in browser storage after logout.

## Template variables

Use the readable `{{VARIABLE_NAME}}` syntax. Variables are selected from the API response and must not be typed from an undocumented global list.

### Invoice Email

- `{{CUSTOMER_NAME}}`
- `{{INVOICE_NUMBER}}`
- `{{BRANCH_NAME}}`

### Disclaimer Signing

- `{{GUARDIAN_NAME}}`
- `{{DISCLAIMER_URL}}`
- `{{EXPIRES_AT}}`
- `{{BRANCH_NAME}}`

## API workflow

```text
GET    /api/v1/system-settings/email-templates
GET    /api/v1/system-settings/email-templates/{key}
PUT    /api/v1/system-settings/email-templates/{key}
POST   /api/v1/system-settings/email-templates/{key}/preview
DELETE /api/v1/system-settings/email-templates/{key}
```

`DELETE` resets the branch override; it does not remove the system template. All endpoints are admin-only and branch-scoped.

Example update:

```json
{
  "subject": "Your PlayVille invoice {{INVOICE_NUMBER}}",
  "bodyText": "Hello {{CUSTOMER_NAME}},\n\nYour invoice is attached.\n\n{{BRANCH_NAME}}",
  "version": 2
}
```

The client sends the returned version on subsequent saves. A conflict response means it must reload before overwriting another administrator's update.

## Safety and delivery rules

- Unknown variables are rejected when saving.
- Required runtime values are checked before queueing.
- Default templates remain available even when no database override exists.
- Rendered content is encrypted in the durable email outbox.
- Raw SMTP credentials, disclaimer tokens, and passwords are never template variables.
- Initial templates are plain text. Rich HTML editing can be added later only with server-side HTML sanitization and a constrained editor.

## Frontend acceptance criteria

- Templates load as accordion cards with `Default` or `Customized` badges.
- Clicking a variable inserts it at the current cursor position.
- Preview never sends an email.
- Save is disabled until content is valid and changed.
- Reset requires confirmation and immediately displays the default content.
- A failed save keeps the user's draft visible.
- Keyboard navigation and screen-reader labels cover accordions, variable chips, editors, and actions.
