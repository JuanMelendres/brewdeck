# Slice C.2 — Password Reset (design)

**Date:** 2026-07-11
**Phase:** 6 (Auth & Multi-User) · Slice C (Account UX) · sub-slice C.2
**Branch:** `feature/auth-password-reset`
**Depends on:** Slice A (JWT auth), Slice C.1 (account profile + change-password)

## Goal

Let a user who has forgotten their password regain access without support intervention:
request a reset link by email, then set a new password using a single-use, time-limited token.
No live SMTP dependency required to ship — delivery goes through a mail **port** whose default
adapter logs the link, mirroring the AI `RecipeSuggestionPort` pattern already in the codebase.

## Non-goals (deferred)

- Real transactional email delivery (SMTP adapter is stubbed behind a toggle; wiring a real
  provider is a follow-up).
- Email verification on registration (Slice C.3 — shares this mail infra).
- Refresh tokens (Slice C.4).
- Rate limiting / captcha on the request endpoint (noted as a future hardening item).

## Security invariants (non-negotiable)

1. **No user enumeration.** `POST /api/auth/forgot-password` always returns `200` with a generic
   body, whether or not the email maps to an account. Never reveal existence.
2. **Store only a hash of the token.** The raw token travels only in the reset link (via the mail
   port). The DB persists `SHA-256(token)`; a DB leak cannot be replayed.
3. **Single-use.** Consuming a token stamps `used_at`; a second use is rejected.
4. **Time-limited.** Tokens expire after **30 minutes** (`expires_at`). Expired tokens are rejected.
5. **Opaque, high-entropy token.** 256-bit random, base64url-encoded (same generator style as the
   existing recipe share token).
6. **Reset endpoints are public** (added to the `permitAll` list) but leak nothing on failure —
   invalid/expired/used token all return the same `400` shape.
7. **Password reuse of the change-password rules.** New password validated `min 8, max 100`
   (mirrors `ChangePasswordRequest` / `RegisterRequest`).

## Architecture

New package: `com.brewdeck.brewdeck_api.auth.reset` (keeps the reset flow cohesive and out of the
already-busy `AuthService`).

```
PasswordResetController
  → PasswordResetService (orchestration; @Transactional)
      → PasswordResetTokenRepository (JPA)
      → UserRepository (existing)
      → PasswordEncoder (existing bean)
      → PasswordResetMailPort (delivery)
```

### Mail port (delivery abstraction)

```java
public interface PasswordResetMailPort {
  void sendResetLink(String email, String rawToken);
}
```

- `LoggingPasswordResetMailAdapter` — **default** (`@ConditionalOnProperty` false/missing). Logs the
  reset URL at INFO: `Password reset link for {email}: {frontendUrl}/reset-password?token={rawToken}`.
  Lets the whole flow run + be tested in CI with zero SMTP.
- `SmtpPasswordResetMailAdapter` — activated by `brewdeck.mail.enabled=true`. Stub in this slice
  (throws `UnsupportedOperationException` or logs a TODO); real `JavaMailSender` wiring is a
  follow-up. Kept so the seam exists and the config point is documented.
- `MailProperties` record: `@ConfigurationProperties(prefix = "brewdeck.mail")` →
  `(boolean enabled, String frontendBaseUrl)`. `frontendBaseUrl` defaults to the dev frontend origin
  and builds the reset link.

### Data model — Flyway `V9__create_password_reset_tokens.sql`

```sql
CREATE TABLE password_reset_tokens (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT NOT NULL REFERENCES users(id),
    token_hash VARCHAR(64) NOT NULL UNIQUE,   -- hex SHA-256
    expires_at TIMESTAMP NOT NULL,
    used_at    TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX idx_password_reset_tokens_user ON password_reset_tokens(user_id);
```

Entity `PasswordResetToken` (Lombok, same style as `User`). No DTO exposure — tokens never leave
the backend except as the raw value in the mail link.

## API

### `POST /api/auth/forgot-password` (public)
Request: `ForgotPasswordRequest(@NotBlank @Email String email)`
Behavior:
1. Look up user by email.
2. If found: generate raw token, persist `SHA-256(token)` with 30-min expiry, invalidate any prior
   unused tokens for that user (defensive — stamp `used_at=now()` on outstanding ones), call
   `mailPort.sendResetLink(email, rawToken)`.
3. Always return `200 { "message": "If that email exists, a reset link has been sent." }`.

Never returns 404. The user-lookup miss path is a silent no-op.

### `POST /api/auth/reset-password` (public)
Request: `ResetPasswordRequest(@NotBlank String token, @NotBlank @Size(min=8,max=100) String newPassword)`
Behavior:
1. Hash the supplied token, look up by `token_hash`.
2. Reject if missing, `used_at != null`, or `expires_at < now()` → `400` via new
   `InvalidResetTokenException` mapped in `GlobalExceptionHandler` (generic message
   "Reset token is invalid or has expired"; no distinction between the failure modes).
3. On success: re-encode the new password onto the user, stamp `used_at=now()`, save. Return `204`.

Both routes added to `SecurityConfig` `permitAll` alongside `register`/`login`.

## Error handling

- New `InvalidResetTokenException extends RuntimeException` → `400` in `GlobalExceptionHandler`
  (same construction as `InvalidCurrentPasswordException`), generic sanitized message.
- Bean-validation failures → existing `MethodArgumentNotValidException` → `400 validationErrors`.
- `forgot-password` never errors on unknown email (invariant #1).

## Frontend

Two **public** pages (added to the public-path allowlist in `apiFetch`/route guard, next to
`/login`, `/register`, `/share`):

- `src/app/forgot-password/page.tsx` + `components/auth/ForgotPasswordForm.tsx` — email field →
  `POST /forgot-password`. Always shows the same success confirmation (no enumeration on the UI
  either). Link from the login page ("Forgot password?").
- `src/app/reset-password/page.tsx` + `components/auth/ResetPasswordForm.tsx` — reads `token` from
  the query string, new-password + confirm fields (reuse a `resetPasswordSchema` shaped like
  `changePasswordSchema` minus `currentPassword`), → `POST /reset-password`. On `204` show success
  + link to `/login`; on `400` show "This reset link is invalid or has expired."

API client: add `forgotPassword` + `resetPassword` to `src/lib/api/auth.ts`. Zod schemas in
`authSchema.ts`. No `AuthProvider` change (unauthenticated flow, no Context user).

## Testing

**Backend**
- `PasswordResetServiceTest` (Mockito): token generated + hashed + persisted with future expiry;
  mail port invoked; unknown email = no-op no-throw; reset happy path re-encodes + stamps `used_at`;
  reject expired / used / unknown token → `InvalidResetTokenException`; prior tokens invalidated.
- `PasswordResetControllerTest` (standalone MockMvc + real `GlobalExceptionHandler`):
  forgot returns `200` for known + unknown email (identical body); reset `204`; bad token `400`;
  short password `400`.
- `PasswordResetTokenRepositoryTest`: `findByTokenHash`.
- Integration (`PasswordResetIntegrationTest`, Testcontainers, extends `PostgresIntegrationTest`):
  register → forgot-password (200) → pull the raw token from the logging adapter (expose it via a
  test-scoped capture or a spy bean) → reset-password (204) → login with new password (200) + old
  rejected (401) → replay same token (400) → expired-token path (persist a token with past
  `expires_at`, assert 400). Adapter/SMTP live path excluded from coverage like the AI adapter.

**Frontend**
- `ForgotPasswordForm.test.tsx`: submits email, shows generic confirmation, error path.
- `ResetPasswordForm.test.tsx`: reads token from query, validates min-length + confirm-match,
  submits, success on 204, "invalid or expired" on 400.
- Mock the api-client layer.

## Docs & artifacts (same PR)

- `docs/api/README.md`, `docs/api/openapi.yaml` — two new public auth endpoints.
- Postman: add `POST - Forgot password` + `POST - Reset password` to the Auth folder.
- `.env.example` (backend + web): document `BREWDECK_MAIL_ENABLED`, `BREWDECK_MAIL_FRONTEND_BASE_URL`.
- `.claude/project-state.md` + `.claude/roadmap.md`: mark C.2 done, point to C.3.
- ADR (optional, `docs/decisions/`): "Password reset via hashed single-use tokens + mail port."

## Commit plan

1. `feat(api): password reset via hashed single-use tokens and mail port (Slice C.2)` — migration,
   entity/repo, service, controller, mail port + adapters, exception + handler, security matchers,
   tests, API docs + Postman.
2. `feat(web): forgot-password and reset-password pages (Slice C.2)` — pages, forms, api client,
   schemas, login link, tests.

## Verification

- Backend: `./mvnw spotless:apply && ./mvnw clean verify && sh mvnw pmd:check`
- Frontend: `npm run test && npm run type-check && npm run lint && npm run build`

## Open questions / assumptions

- **Assumption:** 30-min TTL and 256-bit tokens are acceptable defaults (industry-standard).
- **Assumption:** logging adapter is acceptable as the shipped default for a portfolio app; real
  SMTP is a documented follow-up, not part of C.2.
- **TODO (future):** rate-limit `forgot-password` to blunt abuse/enumeration-by-timing.
