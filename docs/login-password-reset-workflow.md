# PlayVille CRM Login and Password Reset Workflow

## Scope

This document describes the staff login and password reset flow shared by the Angular frontend and Spring Boot backend. It is the reference point for future auth changes and security reviews.

## Runtime endpoints

The backend runs with the `/api/v1` servlet context path. The Angular development proxy forwards `/api` requests to `http://localhost:8080`.

| Purpose | Method | URL | Authentication |
| --- | --- | --- | --- |
| Staff login | `POST` | `/api/v1/auth/login` | Public |
| Request password reset | `POST` | `/api/v1/auth/forgot-password` | Public |
| Confirm password reset | `POST` | `/api/v1/auth/reset-password` | Public |
| Current staff session | `GET` | `/api/v1/auth/me` | Bearer JWT |

### Request examples

Login:

```json
{
  "username": "staff.username",
  "password": "current-password"
}
```

Forgot password:

```json
{
  "email": "staff@example.com"
}
```

Reset password:

```json
{
  "token": "token-from-email-link",
  "newPassword": "new-password-at-least-8-characters"
}
```

## Normal login flow

1. The user opens `/login` and submits username and password.
2. Angular calls `POST /api/v1/auth/login` through the development proxy.
3. Spring Security authenticates the credentials using the existing staff provider and BCrypt password hash.
4. The backend returns the staff identity, branch information, and a JWT.
5. Angular stores the JWT under `playville_auth_token` and the user payload under `playville_auth_user`.
6. Authenticated requests send `Authorization: Bearer <token>`.
7. The JWT contains the staff `tokenVersion`. The security filter compares it with the current database value on protected requests.

The regular login UX is unchanged by password reset. Invalid credentials continue to use the existing generic login error.

## Forgot-password flow

1. The user selects `Forgot password?` on the login screen.
2. Angular validates the email format locally and calls the forgot-password endpoint.
3. The backend normalizes the email and resolves the client IP from `X-Forwarded-For`, `X-Real-IP`, or the remote address.
4. Per-email and per-IP throttling is checked.
5. If the email belongs to an active staff account, previous reset tokens for that email are deleted.
6. A cryptographically random token is generated. Only a BCrypt hash is stored in `pv_password_reset_tokens`.
7. The raw token is placed only in the email link. It is never logged or stored in the database.
8. The email is sent through the configured SMTP server.
9. The API returns the same success message for an existing account, missing account, invalid input handled by the endpoint, or throttled request:

   `If an account exists, password reset instructions have been sent.`

This response is intentional account-enumeration protection. The server still sends an email only when a matching staff account exists.

## Reset confirmation flow

1. The email link opens `/reset-password?token=<raw-token>` in Angular.
2. The page validates that the new password is at least eight characters and that confirmation matches.
3. Angular calls `POST /api/v1/auth/reset-password`.
4. The backend checks the token against active, unused, unexpired hashed tokens.
5. The matching staff password is replaced with a BCrypt hash.
6. The staff `tokenVersion` is incremented, invalidating all previously issued JWTs for that account.
7. The token is marked used and reset records for that email are removed.
8. The user sees a success message and is redirected to login.

Missing, invalid, expired, and already-used tokens are rejected. A reset token is single-use and has a default lifetime of 15 minutes.

## Email template

Subject:

`Reset your PlayVille CRM password`

Body:

```text
Hello,

We received a request to reset the password for your PlayVille CRM account.

Reset your password using this secure link:
<generated frontend URL>/reset-password?token=<raw token>

This link expires in 15 minutes and can be used only once.

If you did not request a password reset, you can safely ignore this email. Your password will not change unless the link is used.

Regards,
PlayVille CRM Support
```

The sender, frontend URL, and lifetime are configurable. The link is built with URI query encoding rather than string concatenation.

## Local end-to-end testing

Prerequisites:

- Docker Desktop running
- Backend repository at `D:\workspace\playville-crm`
- Frontend repository at `D:\workspace\playville-crm-ui`
- A staff record with a known email address

The active backend `.env` is configured for a host-run backend and the bundled Mailpit service:

- Host-run backend SMTP: `localhost:1025`
- Docker app SMTP: `mailpit:1025` (set by `docker-compose.yml`)
- Inbox UI: `http://localhost:8025`
- Frontend reset URL: `http://localhost:4200/reset-password`
- Token lifetime: 15 minutes

Start the database and Mailpit:

```powershell
cd D:\workspace\playville-crm
docker compose up -d db mailpit
```

Start the backend in a separate terminal:

```powershell
cd D:\workspace\playville-crm
.\mvnw.cmd spring-boot:run
```

Start the Angular frontend in another terminal:

```powershell
cd D:\workspace\playville-crm-ui
npm.cmd start
```

Test the flow:

1. Open `http://localhost:4200/login`.
2. Select `Forgot password?`.
3. Submit the registered staff email.
4. Open `http://localhost:8025` and open the newest reset message.
5. Follow the reset link, enter a matching password, and submit.
6. Return to login and authenticate with the new password.
7. Verify any old JWT session is rejected on its next protected request.

For a direct API check, use a registered email and inspect only the Mailpit inbox for the message. Do not paste real production SMTP credentials into `.env` or commit them.

## Configuration reference

| Setting | Environment variable | Local default |
| --- | --- | --- |
| SMTP host | `PLAYVILLE_SMTP_HOST` | `localhost` in application properties; `mailpit` in local `.env` |
| SMTP port | `PLAYVILLE_SMTP_PORT` | `1025` |
| SMTP username | `PLAYVILLE_SMTP_USERNAME` | empty |
| SMTP password | `PLAYVILLE_SMTP_PASSWORD` | empty |
| SMTP auth | `PLAYVILLE_SMTP_AUTH` | `false` |
| SMTP STARTTLS | `PLAYVILLE_SMTP_STARTTLS` | `false` |
| From address | `PLAYVILLE_EMAIL_FROM` | `no-reply@playville.local` |
| Frontend base URL | `PLAYVILLE_FRONTEND_BASE_URL` | `http://localhost:4200` |
| Token lifetime | `PLAYVILLE_PASSWORD_RESET_TTL_MINUTES` | `15` |

For staging or production, use a trusted HTTPS frontend URL, a real sender address, authenticated SMTP, STARTTLS where supported, and secrets supplied by the deployment environment.

## Database and migration

Migration `V37__password_reset_tokens.sql` creates `pv_password_reset_tokens` with:

- Hashed token value
- Normalized email
- Creation and expiry timestamps
- Single-use timestamp
- Lookup indexes

The same migration adds `token_version` to `pv_staff`. Flyway applies this migration before the reset endpoints are used.

## Security review checklist

- Never return whether an email exists.
- Never log raw reset tokens, passwords, or reset URLs.
- Store only a one-way token hash.
- Enforce expiration and single use on the server.
- Delete or invalidate older reset links when a new request is made.
- Rate-limit by both email and client IP.
- Increment `token_version` after a successful reset.
- Keep forgot/reset endpoints public but keep all staff data endpoints protected.
- Use HTTPS and a controlled frontend base URL outside local development.
- Keep SMTP credentials outside source control.

## Files involved

- `src/app/login/login.component.ts`: login and forgot-password state
- `src/app/login/login.component.html`: login and forgot-password controls
- `src/app/auth.service.ts`: Angular auth API calls and token storage
- `src/app/reset-password/reset-password.component.ts`: reset form and token submission
- `src/app/app.routes.ts`: public reset route
- `src/main/java/com/playville/crm/auth/AuthController.java`: auth endpoints
- `src/main/java/com/playville/crm/auth/service/PasswordResetService.java`: token lifecycle and password update
- `src/main/java/com/playville/crm/auth/service/PasswordResetMailService.java`: reset email composition and delivery
- `src/main/java/com/playville/crm/auth/service/PasswordResetRateLimiter.java`: throttling
- `src/main/java/com/playville/crm/auth/service/PasswordResetAuditService.java`: redacted security audit logging
- `src/main/java/com/playville/crm/security/JwtTokenProvider.java`: token version claim
- `src/main/java/com/playville/crm/security/JwtAuthenticationFilter.java`: stale-token rejection
- `src/main/resources/db/migration/V37__password_reset_tokens.sql`: schema migration
