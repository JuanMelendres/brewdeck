# ADR-012: Require a verified email (flagged rollout)

## Status
Accepted

## Date
2026-09-30

## Context
Unverified accounts had full access (audit finding 9), so anyone could register with an address they do not own. Until PR #164, verification emails were never actually sent (the SMTP adapter was a placeholder), so the requirement could not have been enforced. The owner chose to **fully block** unverified users (2026-09-29).

## Decision
- **Policy:** an authenticated user whose email is not verified gets **`403`** with `"code": "EMAIL_NOT_VERIFIED"` on every endpoint except those needed to finish verifying:
  `GET /api/auth/me`, `POST /api/auth/resend-verification`, `POST /api/auth/verify-email`, `POST /api/auth/logout`, `POST /api/auth/refresh`, `GET /api/feature-flags`.
  Public endpoints (`/api/public/**`, register/login/forgot/reset) and anonymous requests are unaffected. Admins are not exempt.
- **Enforcement:** `EmailVerificationRequiredFilter` runs in the security chain right after the JWT filter, before any controller or side effect. It reads `emailVerified` from the `AuthenticatedUser` principal, which the JWT filter loads fresh from the DB on every request, so verifying takes effect on the very next request with no extra query.
- **Error shape:** `ErrorResponse` gains an optional `code` field (`null` elsewhere), so the client can tell this `403` apart from "not an admin".
- **Rollout:** gated by the RELEASE flag `auth-require-email-verification` (V20). It is **disabled in every environment, including local**. The flag is evaluated only for unverified users (a cached lookup). It is exposed to the frontend as `requireEmailVerification`.

## Consequences
- **Positive:** closes the "register with someone else's email" gap once enabled; the backend is the source of truth.
- **Negative:** turning the flag on before email delivery works locks unverified users out, including the owner if his account is unverified. That is why it starts off everywhere.
- **Negative:** the flag gates an access policy. This is acceptable only as a temporary rollout switch (the policy is additive over the previous behaviour). Per the flag policy it must be removed once stable, leaving the check unconditional.

### Recovery if you get locked out
1. Disable the flag (`FeatureFlagAdminService.setEnabled("auth-require-email-verification", "<env>", false, ...)`, or `UPDATE feature_flags SET enabled = false WHERE feature_key = 'auth-require-email-verification' AND environment = '<env>';` and wait up to 45 s for the cache), **or**
2. Mark the account verified: `UPDATE users SET email_verified = true WHERE email = '<you>';`.

## Enabling checklist
1. Configure SMTP for the environment (`BREWDECK_MAIL_ENABLED=true`, `SPRING_MAIL_*`; see `docs/development/environment-variables.md`).
2. Register a test account and confirm the verification email arrives and its link works.
3. Make sure your own account is verified.
4. Enable the flag for that environment.

## Alternatives Considered
- **Read-only access until verified:** rejected by the owner in favour of a full block.
- **Grace period:** rejected; more complexity for a single-user deployment.
- **Frontend-only gating:** rejected; the backend must be the source of truth.
- **Enforce without a flag:** rejected; it would lock users out before email delivery is configured.
