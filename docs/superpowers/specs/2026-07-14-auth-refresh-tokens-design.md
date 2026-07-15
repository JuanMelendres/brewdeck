# Design — Auth Refresh Tokens (Phase 6, Slice C.4)

- **Date:** 2026-07-14
- **Slice:** Phase 6 (Auth & Multi-User) — Slice C.4, closing Slice C and Phase 6.
- **Branch:** `feature/auth-refresh-tokens`
- **Status:** Approved for planning

## Problem

The app issues a single long-lived (24h) access JWT stored in `localStorage`. This forces a bad tradeoff: a long TTL means a stolen token stays valid for a day with no way to revoke it (JWT is stateless — the server can't invalidate an issued token); a short TTL means users get logged out constantly. There is also no real logout — clearing `localStorage` leaves the token valid until it expires.

## Goal

Introduce short-lived access tokens plus long-lived, single-use **rotating refresh tokens** with reuse detection, giving us:

- Short access-token exposure window (15 min) without constant re-login.
- Server-side revocation → real logout and theft containment.
- Standard OAuth-style rotation + reuse detection posture.

This is the last remaining sub-slice of Slice C; shipping it completes Phase 6.

## Non-Goals

- httpOnly cookie storage / CSRF infrastructure (explicitly rejected — see Decisions).
- "Log out everywhere" / session-management UI (future feature).
- Device/session metadata (user-agent, IP) on tokens.
- Sliding-window refresh TTL extension on rotation (refresh TTL is fixed from issue).

## Decisions (resolved during brainstorming)

1. **Transport/storage: `localStorage`, consistent with the access token.** The refresh token is returned in the JSON body, stored in `localStorage`, and sent in the request body to `/api/auth/refresh`. Rejected the httpOnly-cookie alternative: it drags in CORS `credentials`, `SameSite`/CSRF handling, and per-env cookie config — disproportionate to this project, and the access token already lives in `localStorage`, so cookies wouldn't change the app's overall XSS risk profile. The security win of this slice lives in rotation + reuse detection, not in transport.
2. **Reuse detection → revoke all the user's active refresh tokens.** Presenting an already-used (rotated) or revoked token signals theft; we revoke every active refresh token for that user, forcing re-login everywhere. Rejected "reject only the reused token" as rotation without teeth. The accidental-double-fire footgun is contained by frontend single-flight refresh.
3. **Logout revokes only the presented refresh token** (this device/session). "Log out everywhere" is a separate future feature.
4. **TTLs:** access JWT **15 min** (down from 24h); refresh token **7 days**. Both configurable, env-overridable.
5. **Refresh token is opaque** (random 256-bit, SHA-256-hashed at rest), not a JWT — reuses the existing `SecureTokens` helper and the hashed-single-use-token pattern already shipped in C.2 (password reset) and C.3 (email verification).

## Architecture

New package `com.brewdeck.brewdeck_api.auth.refresh`, mirroring the existing `auth/reset/` and `auth/verification/` packages.

- Access token: unchanged mechanism (HMAC JWT via `JwtService`), TTL shortened to 15 min.
- Refresh token: opaque high-entropy value. Only its `SHA-256` hex hash is persisted (`SecureTokens.newToken()` / `SecureTokens.sha256Hex()`, reused as-is).
- `login` and `register` now return **both** tokens.
- `POST /api/auth/refresh` rotates: validate presented refresh token → mark used → issue a fresh access+refresh pair.
- `POST /api/auth/logout` revokes the presented refresh token.

## Data — Flyway V11

`V11__create_refresh_tokens.sql`:

```sql
CREATE TABLE refresh_tokens (
  id          BIGSERIAL PRIMARY KEY,
  user_id     BIGINT NOT NULL REFERENCES users(id),
  token_hash  VARCHAR(64) NOT NULL UNIQUE,   -- hex SHA-256 of the raw token
  expires_at  TIMESTAMP NOT NULL,
  used_at     TIMESTAMP,                      -- set on rotation (single-use)
  revoked_at  TIMESTAMP,                      -- set on logout or reuse-revoke
  created_at  TIMESTAMP NOT NULL
);
CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);
```

Rotated tokens are **kept**, not deleted, so reuse detection can observe `used_at`. A token is **active** iff `used_at IS NULL AND revoked_at IS NULL AND expires_at > now()`.

### Entity — `RefreshToken`

JPA entity mapping the table (Lombok, `LocalDateTime` timestamps matching the codebase convention used by `PasswordResetToken` / `EmailVerificationToken`). Fields: `id`, `userId`, `tokenHash`, `expiresAt`, `usedAt`, `revokedAt`, `createdAt`.

### Repository — `RefreshTokenRepository`

- `Optional<RefreshToken> findByTokenHash(String tokenHash)`
- Bulk revoke of a user's active tokens (reuse containment):
  ```java
  @Modifying
  @Query("UPDATE RefreshToken t SET t.revokedAt = :now "
       + "WHERE t.userId = :userId AND t.usedAt IS NULL "
       + "AND t.revokedAt IS NULL AND t.expiresAt > :now")
  int revokeAllActiveForUser(Long userId, LocalDateTime now);
  ```

## Service — `RefreshTokenService`

Configurable refresh TTL injected via `@Value("${brewdeck.auth.refresh-ttl}")` (`Duration`). All methods `@Transactional`.

- **`String issue(User user)`** — generate raw token, persist `{userId, sha256Hex(raw), now+refreshTtl, created}`, return the raw token.
- **`RotationResult rotate(String rawToken)`** — resolve by `sha256Hex(rawToken)`:
  - not found → `InvalidRefreshTokenException`
  - `usedAt != null` OR `revokedAt != null` → **reuse detected**: `revokeAllActiveForUser(userId, now)`, then `InvalidRefreshTokenException`
  - `expiresAt <= now` → `InvalidRefreshTokenException` (expiry is not theft → no chain revoke)
  - otherwise active → set `usedAt = now`, `issue(user)` a new token, return `RotationResult(user, newRawToken)`
- **`void revoke(String rawToken, Long currentUserId)`** — resolve by hash; if present and `userId == currentUserId` and still active, set `revokedAt = now`. Idempotent: missing / already-inactive / not-owned → no-op, still succeeds (endpoint returns 204 regardless). Scoping the revoke to `currentUserId` prevents one authenticated user revoking another user's token.

`RotationResult` — small record `(User user, String rawToken)` internal to the package.

`InvalidRefreshTokenException` — mapped to **401** in `GlobalExceptionHandler` with a single generic message; no distinction between not-found / used / expired (no oracle for attackers). Mirrors the single-generic-message approach of `InvalidResetTokenException` / `InvalidVerificationTokenException` (those are 400; refresh is 401 because it is an authentication failure, not a bad request).

## Endpoints — `AuthController`

| Method | Path | Auth | Request body | Success |
|---|---|---|---|---|
| POST | `/api/auth/refresh` | public (permitAll) | `RefreshRequest { refreshToken }` | 200 `AuthResponse` (new access + refresh pair) |
| POST | `/api/auth/logout` | authenticated | `RefreshRequest { refreshToken }` | 204 No Content |

- `/api/auth/refresh` is public: the caller's access token may already be expired (that is the point), so refresh cannot require a valid access token — it authenticates via the refresh token itself.
- `/api/auth/logout` stays **authenticated** (requires a valid access token). This means an attacker holding only a stolen refresh token cannot revoke the legitimate user's session. `currentUserId` is resolved from the authenticated principal (via `CurrentUserProvider` / principal email).
- `RefreshRequest` — `record RefreshRequest(@NotBlank String refreshToken)`.

### `AuthResponse` change

```java
// before: record AuthResponse(String token, Instant expiresAt, String email)
record AuthResponse(String token, Instant expiresAt, String email, String refreshToken)
```

`AuthService.register` / `login` call `refreshTokenService.issue(user)` and populate `refreshToken`. `/api/auth/refresh` builds the same `AuthResponse` from the rotated pair. Adding a trailing field is backward-compatible for existing JSON consumers.

### `SecurityConfig`

- Add `/api/auth/refresh` to the permitAll matchers (join the existing `/api/auth/register`, `/api/auth/login`, password-reset, and `verify-email` entries).
- **Do not** add `/api/auth/logout` — it stays behind `.anyRequest().authenticated()`.
- Verify the terminal `.anyRequest().authenticated()` remains intact after the edit (napkin/C.2 regression check — a widened matcher must not slip through).

## Frontend

### `lib/auth/tokenStore.ts`

Add a second `localStorage` key `brewdeck.refreshToken`:
- `getRefreshToken()`, `setRefreshToken(token)`
- `clearTokens()` clears **both** access and refresh keys. Existing `clearToken()` stays (access only) or is folded into `clearTokens()` at call sites — implementer's choice, but every place that currently clears the access token on logout/401 must also clear the refresh token.

### `lib/api/auth.ts`

- `refresh(refreshToken: string): Promise<AuthResponse>` → `POST /api/auth/refresh`
- `logout(refreshToken: string): Promise<void>` → `POST /api/auth/logout`
- `AuthResponse` TS type gains `refreshToken: string`.

### `lib/api/client.ts` — single-flight refresh interceptor

On a `401` response, before the current clear-and-redirect logic:

1. Skip refresh entirely if: the request path is `/api/auth/refresh` (prevents infinite loop), there is no stored refresh token, or the current page is public (existing `onPublic` set).
2. Otherwise attempt a **single-flight** refresh: a module-level `let inFlight: Promise<AuthResponse> | null`. Concurrent 401s all `await` the same promise so only **one** rotation happens (this is what prevents accidental reuse-revoke from parallel requests). Clear `inFlight` in a `finally`.
3. On refresh success: store the new access + refresh tokens, then **retry the original request once** with the new access token; return its result.
4. On refresh failure: `clearTokens()` + redirect to `/login` (current behavior), and surface the error.

Guard: the retried original request does not itself recurse into another refresh attempt (retry-once flag).

### `lib/auth/AuthProvider.tsx`

- On `login` / `register`, store the refresh token (`setRefreshToken`) alongside the access token.
- `logout()` calls `auth.logout(refreshToken)` (best-effort — a network failure still proceeds to local clear), then `clearTokens()` and resets Context user.

## Error Handling & Edge Cases

- **Parallel requests after access expiry** → single-flight collapses to one rotation; the losing requests reuse the same new access token. No reuse-revoke triggered.
- **Two browser tabs** each firing refresh → only the winner rotates; the loser presents an already-used token → reuse detected → all tokens revoked → both tabs must re-login. Accepted tradeoff of Decision 2; single-flight makes it rare (per-tab, not per-request). Documented as a known behavior.
- **Logout with an already-rotated/expired/unknown token** → 204 (idempotent).
- **No refresh token stored** → client skips refresh, goes straight to the existing clear + redirect.
- **Refresh with a malformed/blank token** → `@NotBlank` 400 (validation) before service; a well-formed-but-unknown token → 401.

## Testing

### Backend

- **`RefreshTokenServiceTest`** (Mockito): issue persists hash + returns raw; rotate happy path (old marked used, new issued); rotate on used token → `revokeAllActiveForUser` invoked + throws; rotate on revoked token → same; rotate on expired → throws, **no** revoke-all; revoke sets `revokedAt` when owned + active; revoke idempotent for missing / not-owned / already-inactive.
- **`RefreshTokenRepositoryTest`** (`@DataJpaTest`): `findByTokenHash`; `revokeAllActiveForUser` flips only active rows for the target user and returns the count.
- **`AuthControllerTest`** (standalone MockMvc): `refresh` 200 returns a new pair; `refresh` invalid → 401; `logout` 204; blank `refreshToken` → 400; `login`/`register` responses now include `refreshToken`.
- **`AuthSecurityIntegrationTest`** (Testcontainers, extend existing): full flow — register/login yields both tokens → `refresh` returns a new pair and the old refresh is now rejected → presenting the old (reused) refresh returns 401 **and** the just-issued refresh is also revoked (must re-login) → `logout` then `refresh` with the logged-out token → 401.

### Frontend

- **`client` test**: N concurrent 401s trigger exactly **one** `/refresh` call (single-flight) and all retries succeed; refresh failure → `clearTokens()` + redirect `/login`; a 401 from `/api/auth/refresh` itself does **not** recurse.
- **`auth.ts`**: `refresh` / `logout` call the right paths with the right bodies.
- **`AuthProvider`**: login/register store the refresh token; `logout()` calls the endpoint then clears both tokens and the Context user; logout proceeds to local clear even if the endpoint call rejects.

## Configuration & Docs

- `application.yaml`: `brewdeck.auth.token-ttl` default `PT24H` → **`PT15M`**; add `brewdeck.auth.refresh-ttl: ${AUTH_REFRESH_TTL:P7D}`.
- `.env.example`: document `AUTH_TOKEN_TTL` (now 15m) and new `AUTH_REFRESH_TTL`.
- `docs/api/README.md` + `docs/api/openapi.yaml`: add `/api/auth/refresh` and `/api/auth/logout`; note the new `refreshToken` field on `AuthResponse`.
- Postman collection: add both requests + a `{{refreshToken}}` env var (captured from login/refresh responses via a test script); keep no real credentials.
- Update `.claude/project-state.md` and `.claude/roadmap.md` (mark C.4 Done → Phase 6 complete).

## Delivery

Subagent-driven, per-task (as C.1–C.3), on `feature/auth-refresh-tokens`. Backend before frontend. Verification gate per task:

- Backend: `./mvnw spotless:apply && ./mvnw clean verify && sh mvnw pmd:check`
- Frontend: `pnpm test && pnpm type-check && pnpm build && pnpm lint:fix -- <changed files>`

## Commit Messages (Conventional Commits)

- `feat(api): add rotating refresh tokens with reuse detection`
- `feat(web): auto-refresh access token on 401 with single-flight retry`
- Plus focused `test(api)` / `test(web)` / `docs` / `chore(api)` (config) commits as the plan splits them.
