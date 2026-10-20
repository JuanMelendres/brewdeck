# ADR-019: Secure email change

## Status
Accepted (2026-10-20). Backlog AUTH-04.

## Date
2026-10-20

## Context
- The email is the login identifier and the only recovery channel (password reset, ADR-012
  verification). Today it cannot be changed: `UpdateProfileRequest` has only `displayName`.
- Changing it is an account-takeover path. With a stolen session alone, an attacker who could
  change the email would then reset the password and lock the owner out. OWASP (Authentication
  Cheat Sheet, "Changing a user's registered email address") asks for:
  - re-authentication;
  - confirmation from the new address;
  - a notice to the old address;
  - short-lived, single-use tokens.
- Building blocks that already exist:
  - `SecureTokens` (random token, SHA-256 hash stored).
  - Atomic single-use claim (`UPDATE ... WHERE used_at IS NULL`, AUTH-05.5).
  - `User.revokeAccessTokens()` (`token_version`, AUTH-05.6) and `RefreshTokenService.revokeAllForUser`.
  - `EmailAddresses.normalize` and the V18 lowercase check.
  - Per-account rate limits (ADR-011).
  - Signup's no-enumeration pattern (AUTH-05.7): same answer, the difference only shows in email.
- Owner decisions on 2026-10-20:
  - The confirmation link lasts **1 hour**.
  - The old address gets a **notice with a cancel link**.
  - If the new address already has an account, the API **answers the same** and does not reveal it.
  - Confirming **closes every session**.

## Decision

**Flow.**
1. **Request:** `POST /api/auth/me/email-change` with `{ newEmail, currentPassword }`, authenticated.
   - Wrong `currentPassword` returns `400`, the same as change-password.
   - `newEmail` equal to the current email returns `400` (validation error).
   - Otherwise the API answers **`202`** with one fixed message ("check your new inbox"),
     whether or not the address is free.
   - Unverified users may call it even while email verification is enforced (ADR-012), since it
     is the only way to fix a mistyped signup address. Confirming verifies the new one.
2. **New address free:**
   - Any pending request of the user is cancelled first, so at most one is pending.
   - A request is stored with a confirm token for the new address and a cancel token for the old.
   - The new address gets the confirm link (`/email-change/confirm?token=…`).
   - The old address gets a notice with the cancel link (`/email-change/cancel?token=…`), the new
     address partly masked (`j***@example.com`), and advice to reset the password if the owner did
     not ask for this.
3. **New address taken:** nothing is stored. The new address gets an "account exists" notice. The
   old address gets nothing, since there is nothing to cancel.
4. **Confirm:** `POST /api/auth/email-change/confirm` with `{ token }`. It is public, because the
   token is the credential and the link may open on another device.
   - The request is claimed atomically: `confirmed_at IS NULL AND cancelled_at IS NULL AND
     expires_at > now`. Zero rows means the link is invalid, expired or already used.
   - Then `users.email = new_email` and `email_verified = true`; the new address just proved
     control.
   - Then every session is closed: `revokeAccessTokens()` plus `revokeAllForUser`.
   - If the address was taken between request and confirm, the `users.email` unique constraint
     fails. The API answers with the same "invalid link" error and logs it.
   - The old address gets a final "your email was changed" notice. It has no link, because the
     cancel token is no longer valid.
5. **Cancel:** `POST /api/auth/email-change/cancel` with `{ token }`. It is public and valid while
   the request is pending (1 hour).
   - It marks the request cancelled.
   - It **also closes every session**. A cancel means "I did not ask for this", so whoever holds a
     session should be logged out. The answer points to the password reset.

**Data (V32).** A new table, `email_change_requests`:

| Column | Notes |
|---|---|
| `id` | primary key |
| `user_id` | foreign key to `users`, `ON DELETE CASCADE` |
| `new_email` | normalized (V18 rule) |
| `confirm_token_hash` | `CHAR(64)`, unique |
| `cancel_token_hash` | `CHAR(64)`, unique |
| `expires_at` | `TIMESTAMPTZ` |
| `confirmed_at` | `TIMESTAMPTZ`, nullable |
| `cancelled_at` | `TIMESTAMPTZ`, nullable |
| `created_at` | `TIMESTAMPTZ` |

There is an index on `user_id`. Only hashes are stored, never raw tokens.

**Limits (ADR-011).**
- `EMAIL_CHANGE_ACCOUNT`: 3 requests per hour per user id. Wrong passwords count, so a stolen
  access token cannot be used to guess the password here.
- `EMAIL_CHANGE_TOKEN_IP`: 10 confirm or cancel attempts per 15 minutes per IP.

**Feature flag.** `auth-email-change` is a RELEASE flag (owner: Juan; expires 2027-01-31).
- It is off everywhere until the web is done, and the last PR turns it on in `staging`.
- `requireEnabled` guards the request and confirm endpoints.
- **Cancel is never guarded**, so a pending request can always be stopped.
- Removal: once it has been on in staging and production with no issues, delete the flag, its
  rows and the disabled path.

**Web.**
- Account page: a "Change email" section with the new email and the current password, using the
  existing `PasswordField`. It shows only while the flag is on (alias `emailChange`).
- New pages `/email-change/confirm` and `/email-change/cancel`. Both carry the token in the URL,
  so they get `Referrer-Policy: no-referrer` like the reset and verify pages.
- Both pages act only after a click, never on load, so a mail scanner that opens the link cannot
  confirm or cancel the change. Both sign the browser out, since every session ends.
- After confirming, the user is sent to log in with the new address. After cancelling, the page
  points to the password reset.
- Texts in en and es.

**Delivery, three PRs:**
1. API: V32, the flag seed (V33), service, endpoints, the four mail templates (en/es), limits and
   tests, including cross-user and race tests.
2. Web: dialog, the two pages, i18n and tests.
3. Turn the flag on in `staging`; API docs, OpenAPI, Postman and the ASVS checklist row.

## Consequences
- **Positive:**
  - The email can change without opening a takeover path. It needs the password, the new inbox
    and a quiet old inbox.
  - The new address arrives verified.
  - No account enumeration.
- **Negative:**
  - Two inboxes and four mail templates to maintain.
  - Closing every session on confirm forces a login on every device.
- **Known limits:**
  - After the change is **confirmed**, the old owner can no longer act from email. If an attacker
    had the password **and** the session **and** the owner missed the hour-long notice, recovery
    needs manual help from the operator. **Assumption:** acceptable for staging; revisit with
    LEGAL-01 (operator contact) before production. Update 2026-10-20: the operator contact is
    jmelendresdev@gmail.com (LEGAL-01 spike); the "email changed" notice should name it once the
    privacy notice is published.
  - `BREWDECK_ADMIN_EMAIL` bootstrap matches by address. An admin who changes email keeps the
    role, but the variable must be updated or bootstrap logs "no account exists".
- **Cleanup:** expired and finished requests stay until the AUTH-06 purge job handles them.

## Alternatives considered
- **Change at once, notice only.** It is simpler, but a stolen session plus the password is
  enough to take the account. Rejected.
- **Confirm from the old address too (two-sided).** It is stronger, but it blocks owners who
  lost the old inbox, which is a common reason to change the email. The cancel link gives the old
  inbox a veto without making it a requirement. Rejected.
- **Reuse `email_verification_tokens`.** That table has no place for the new address or the
  cancel token, and mixing both purposes would weaken the verification queries. Rejected.
- **`409` for a taken address.** It has better UX, but it reveals which addresses have accounts.
  Rejected (owner, 2026-10-20).
