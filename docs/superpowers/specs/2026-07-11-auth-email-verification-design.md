# Slice C.3 — Email Verification (design)

**Date:** 2026-07-11
**Phase:** 6 (Auth & Multi-User) · Slice C (Account UX) · sub-slice C.3
**Branch:** `feature/auth-email-verification`
**Depends on:** Slice A (JWT auth), Slice C.2 (password reset — mail port + `MailProperties` + token-model pattern)

## Goal

Confirm a user controls the email they registered with. On registration the backend issues a
verification token and emails a link; clicking it marks the account verified. Verification is a
**soft gate**: unverified users can still log in and use the app, but `GET /api/auth/me` exposes
`emailVerified` and the frontend shows a dismissible "verify your email" banner with a resend action.
Delivery reuses the C.2 mail infrastructure via a parallel `EmailVerificationMailPort`.

## Non-goals (deferred)

- **Hard-gating** login or any route on verification (deliberately soft this slice; can tighten later).
- Real transactional email (SMTP adapter stays a stub behind `brewdeck.mail.enabled`, as in C.2).
- Refresh tokens (Slice C.4).
- Rate limiting on resend (noted as future hardening).
- Re-verification when a user changes their email (no email-change flow exists yet).

## Decisions (locked)

1. **Soft gate.** Login unaffected; `emailVerified` surfaced on `/me`; frontend banner + resend.
2. **Backfill existing = verified.** Existing rows (and the seed/test users) are grandfathered
   verified; only accounts registered after this slice start unverified. Mirrors the V6 owner_id
   backfill approach.
3. **Parallel port + own token table.** New `EmailVerificationMailPort` (logging default + SMTP
   stub) reusing the existing `MailProperties` / `brewdeck.mail` toggle; new
   `email_verification_tokens` table (hashed, single-use, 24h TTL) mirroring `password_reset_tokens`.
   No refactor of shipped C.2 code.

## Security & correctness invariants

1. **Store only the token hash** (hex `SHA-256`, `VARCHAR(64)`); the raw token leaves the backend
   only via the mail port. (Same as C.2.)
2. **Single-use** (`used_at`) and **24-hour TTL** (`expires_at`); an unknown/used/expired token maps
   to one generic `400` (`InvalidVerificationTokenException`) — indistinguishable failures.
3. **Token entropy:** 32 `SecureRandom` bytes, base64url without padding (same generator as C.2).
4. **Resend does not enumerate.** `POST /api/auth/resend-verification` is authenticated (acts on the
   current principal only), so there is no email-guessing surface. If the account is already
   verified it is a silent no-op returning the same 200.
5. **Verify endpoint is public** (`permitAll`) but leaks nothing — same generic 400 on any bad token.
6. **Idempotent verify.** Verifying an already-verified user via a still-valid token is harmless;
   once `email_verified` is true, the flag never flips back here.
7. **Registration still succeeds even if mail sending throws** — the account is created and the
   token persisted; a mail failure must not 500 the registration (log and continue). The logging
   adapter never throws, so this only matters for the SMTP path.

## Architecture

New package: `com.brewdeck.brewdeck_api.auth.verification` (cohesive, parallel to `auth.reset`).

```
EmailVerificationController   (POST /verify-email, POST /resend-verification)
  → EmailVerificationService  (@Transactional: issueFor(user), verify(token), resendFor(email))
      → EmailVerificationTokenRepository (JPA)
      → UserRepository (existing)
      → EmailVerificationMailPort (delivery)
AuthService.register(...)     → calls EmailVerificationService.issueFor(savedUser) after save
```

### Mail port

```java
public interface EmailVerificationMailPort {
  void sendVerificationLink(String email, String rawToken);
}
```

- `LoggingEmailVerificationMailAdapter` — **default** (`@ConditionalOnProperty brewdeck.mail.enabled
  false, matchIfMissing=true`). Logs `{frontendBaseUrl}/verify-email?token={rawToken}`.
- `SmtpEmailVerificationMailAdapter` — `havingValue="true"`, stub throwing
  `UnsupportedOperationException` (parallels the C.2 SMTP stub).
- Reuses the existing `MailProperties(enabled, frontendBaseUrl)` bean from `auth.reset` — imported,
  not duplicated. (One shared config record; two feature ports.)

### Data model — Flyway `V10`

```sql
-- email_verified flag on users; existing rows grandfathered verified.
ALTER TABLE users ADD COLUMN email_verified BOOLEAN NOT NULL DEFAULT false;
UPDATE users SET email_verified = true;

CREATE TABLE email_verification_tokens (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT NOT NULL REFERENCES users(id),
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    used_at    TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX idx_email_verification_tokens_user ON email_verification_tokens (user_id);
```

`User` gains `private boolean emailVerified;` (`@Column(name="email_verified", nullable=false)`).
New rows: the entity default is `false`, so registrations created through `AuthService` start
unverified (V10 default matches). `UserResponse` gains `emailVerified`.

Entity `EmailVerificationToken` mirrors `PasswordResetToken` field-for-field.

## API

### `POST /api/auth/verify-email` (public)
Request `VerifyEmailRequest(@NotBlank String token)`.
1. Hash the token, look up by `token_hash`.
2. Reject unknown / `used_at != null` / expired → `400 InvalidVerificationTokenException`.
3. Set the user's `emailVerified = true`, stamp `used_at`, save. Return `204`.

### `POST /api/auth/resend-verification` (authenticated)
No body. Acts on the current principal (via the existing `CurrentUserProvider` / principal email).
1. If already verified → silent no-op, `200`.
2. Else invalidate prior unused verification tokens for the user, issue a new one, mail it. `200`
   with a generic `{ "message": "Verification email sent." }`.

### Registration hook
`AuthService.register` — after `userRepository.save`, call
`emailVerificationService.issueFor(savedUser)` (persists a token + sends the link). Wrapped so a
send failure does not fail registration (invariant 7). The returned `AuthResponse` is unchanged.

`verify-email` added to `SecurityConfig` `permitAll`; `resend-verification` stays authenticated
(default). No other matcher changes.

## Error handling

- `InvalidVerificationTokenException extends RuntimeException` → `400` in `GlobalExceptionHandler`
  (generic "Verification token is invalid or has expired"), same construction as
  `InvalidResetTokenException`.
- Bean-validation → existing `MethodArgumentNotValidException` → `400 validationErrors`.

## Frontend

- **Type + client:** `UserResponse.emailVerified: boolean`; `verifyEmail(token)` and
  `resendVerification()` in `src/lib/api/auth.ts`.
- **Banner:** `EmailVerificationBanner` (MUI `Alert` severity="warning") rendered in the `(app)`
  shell / dashboard when `useAuth().user?.emailVerified === false`. "Resend link" button calls
  `resendVerification`, shows a success/error inline. Dismissible for the session (local state).
- **Verify page:** public `src/app/verify-email/page.tsx` reading `token` from the query
  (`useSearchParams`, wrapped in `<Suspense>`), auto-calls `verifyEmail(token)` on mount; shows
  loading → success ("Email verified — continue to the app") or "This verification link is invalid
  or has expired." Added to the `client.ts` `onPublic` allowlist (like `/reset-password`).
- **AuthProvider:** after a successful `verifyEmail`, refresh the current user (re-`getMe`) so the
  banner disappears — reuse the existing Context user, no new query. Expose a `refreshUser()` or
  fold into the verify page via `getMe`.

## Testing

**Backend**
- `EmailVerificationTokenRepositoryTest` (@DataJpaTest): `findByTokenHash`,
  `findByUserIdAndUsedAtIsNull`.
- `LoggingEmailVerificationMailAdapterTest`: logs, does not throw.
- `EmailVerificationServiceTest` (Mockito): `issueFor` persists a hashed token + mails link;
  `verify` valid → sets `emailVerified` + stamps `used_at`; unknown/used/expired → exception;
  `resendFor` already-verified → no-op no-mail; `resendFor` unverified → invalidates prior + issues.
- `EmailVerificationControllerTest` (standalone MockMvc + real `GlobalExceptionHandler`):
  verify 204; bad token 400; resend 200; unauthenticated resend 401 (covered by security test).
- `AuthServiceTest`: register now also invokes `issueFor` (verify the token is issued); register
  still returns a token when mail send throws (invariant 7).
- Integration (`EmailVerificationIntegrationTest`, Testcontainers, `@MockitoSpyBean` on the mail
  port): register → capture raw token → verify-email 204 → `/me` shows `emailVerified:true`;
  replay token → 400; resend (authenticated) issues a new token; expired token → 400; and confirm a
  freshly registered user starts `emailVerified:false` while a backfilled/seed user is `true`.

**Frontend**
- `EmailVerificationBanner.test.tsx`: shows when unverified, hidden when verified; resend calls the
  api and shows confirmation; dismiss hides it.
- `verify-email` page/component test: reads token, calls `verifyEmail`, success and invalid states.
- Mock the api-client layer / `useAuth`.

## Docs & artifacts (same PR)

- `docs/api/README.md`, `docs/api/openapi.yaml` — two new endpoints; note `emailVerified` on the
  user response.
- Postman: `POST - Verify email` + `POST - Resend verification` in the Auth folder.
- `.claude/project-state.md` + `.claude/roadmap.md`: C.3 done, next C.4.

## Commit plan

1. `feat(api): email verification via hashed tokens and mail port (Slice C.3)` — Flyway V10, entity/
   repo, service, controller, mail port + adapters, exception + handler, register hook, security
   matcher, tests, API docs + Postman.
2. `feat(web): email verification banner and verify-email page (Slice C.3)` — type/client, banner,
   verify page, AuthProvider refresh, onPublic allowlist, tests.

## Verification

- Backend: `./mvnw spotless:apply && ./mvnw clean verify && sh mvnw pmd:check`
- Frontend: `npm run test && npm run type-check && npm run lint && npm run build`

## Open questions / assumptions

- **Assumption:** 24-hour TTL for verification tokens (longer than the 30-min reset TTL, standard
  for verification links).
- **Assumption:** soft gate is acceptable product behavior for now; hard-gating is a future slice if
  desired.
- **Assumption:** the register→issue hook reuses the existing register transaction; the token is
  persisted in the same commit, mail send is best-effort.
