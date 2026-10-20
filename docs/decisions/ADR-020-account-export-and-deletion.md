# ADR-020: Self-service data export and account deletion

## Status
Accepted (2026-10-20). Backlog LEGAL-01, PR 3.

## Date
2026-10-20

## Context
- The privacy notice (LEGAL-01) promises access and cancellation rights (ARCO) from the account
  page. Today a user can rectify data (profile, email change, ADR-019), but cannot get a copy of
  it or delete the account. Both need a manual procedure (`docs/legal/README.md`).
- The owner decided on 2026-10-20 that an account and all its data are **deleted at once** on
  request. Open with the lawyer: whether LFPDPPP Art. 24 requires a blocking period first.
- What a user owns, and how it is linked:
  - `coffees.owner_id`, `recipes.owner_id`, `brew_sessions.owner_id` and `brew_methods.owner_id`
    point to `users`. A null `owner_id` on a brew method means the shared catalog.
  - Recipes reference a coffee and a brew method; brew sessions reference a recipe.
  - Ownership checks keep users from referencing each other's rows (AUTH-05.8), so a user's rows
    only point at the user's own rows or at catalog methods.
  - Token tables point to `users`: `refresh_tokens`, `password_reset_tokens` and
    `email_verification_tokens` have no cascade; `email_change_requests` cascades (V32).

## Decision

**Export (access right).**
- `GET /api/auth/me/export`, authenticated, returns one JSON document as an attachment
  (`brewdeck-export-<date>.json`):
  - profile: email, display name, language, theme, email verified, created at, accepted terms
    version and date;
  - the user's coffees, own brew methods, recipes and brew sessions, in the same shapes as the
    API's responses.
- Never exported: the password hash, token hashes, and internal ids of other users.
- Limit `ACCOUNT_EXPORT_USER`: 5 per hour per user.
- Unverified users may export while email verification is enforced (ADR-012). The data is theirs.

**Deletion (cancellation right).**
- `DELETE /api/auth/me` with `{ currentPassword }`:
  - A wrong password returns `400`.
  - Limit `ACCOUNT_DELETE_USER`: 5 per 15 minutes per user. Wrong passwords count, so a stolen
    access token cannot be used to guess the password.
  - Success returns `204` and clears the refresh cookie.
- **Hard delete in one transaction**, children first:
  1. brew sessions;
  2. recipes;
  3. coffees;
  4. the user's own brew methods;
  5. refresh, reset and verification tokens;
  6. the user, whose email-change requests cascade.

  Shared catalog methods are untouched.
- Every session ends with the account: the user row is gone, so JWT lookups fail at once.
- After the commit, a confirmation email goes to the deleted address, in the user's language. It
  has no links.
- Unverified users may delete their account while email verification is enforced.
- The address becomes free at once: signing up again creates a new, empty account.
- **Admins** may delete their own account. `BREWDECK_ADMIN_EMAIL` bootstrap promotes the address
  again only if it signs up and verifies again (AUTH-05.4).

**Web.**
- On the account page, a "Your data" section:
  - "Download my data" saves the JSON;
  - "Delete my account" opens a confirmation dialog that asks for the current password and says
    the action cannot be undone.
- After deletion the browser signs out and lands on a "Your account was deleted" message.

**No feature flag.** API and web ship together in one PR. The feature is complete, tested, and
needed to honor the privacy notice.

## Consequences
- **Positive:**
  - Access and cancellation rights are self-service, so no manual work in Neon.
  - The privacy notice's "coming soon" becomes true.
- **Negative:**
  - Deletion cannot be undone. Neon point-in-time restore (6 h on the free plan) is the only way
    back, and only for the whole database.
  - The deletion order must follow every new table that references users or user-owned rows.
    `AccountDeletionIntegrationTest` fails if a reference is left behind.
- **If the lawyer requires a blocking period (Art. 24):** add `users.deleted_at`, block login and
  access at once, and purge after the period. The export and the endpoints stay the same.

## Alternatives considered
- **Soft delete (`deleted_at`) now.** It keeps personal data with no stated need, against the
  owner's decision. Rejected until the lawyer says otherwise.
- **`ON DELETE CASCADE` on every foreign key.** It is simpler to call, but a stray `DELETE` on
  `users` would wipe data silently, and it needs a migration over every table. The explicit,
  tested order is clearer. Rejected.
- **Email confirmation link before deleting.** It adds a step, but the password check already
  proves intent. Rejected; the confirmation email after the fact covers awareness.
- **Asynchronous export by email.** The data is small, so a synchronous download is enough.
  Rejected for now.
