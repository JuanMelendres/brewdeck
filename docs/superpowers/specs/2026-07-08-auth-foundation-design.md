# Auth Foundation (Slice A) — Design Spec

**Date:** 2026-07-08
**Status:** Approved (design)
**Slice:** Phase 6, sub-slice A. Full-stack. Introduces authentication to a
currently-open API. First of three planned auth sub-slices.

## Goal

Introduce authentication to BrewDeck: users register and log in, receive a
stateless JWT, and every existing `/api/**` endpoint requires that token —
except the public share-link endpoints, which stay open. This is an
all-or-nothing gate: any authenticated user still sees all data. Per-user
ownership and data partitioning are deferred to Slice B.

## Context

The BrewDeck API has **no Spring Security on the classpath** today — every
`/api/**` endpoint is unauthenticated and open, and there is no `User` concept.
The frontend `apiFetch` sends no auth header. This slice adds the security
layer greenfield.

Slice A is deliberately scoped to identity + authentication only. It does not
attach an owner to any domain row, does not filter data per user, and requires
no backfill of existing global rows — those become visible to any
authenticated user until Slice B introduces ownership.

## Scope boundary (sub-slices)

- **Slice A (this spec)** — identity + authentication foundation: `User`,
  register/login, JWT, gate existing endpoints. All-or-nothing.
- **Slice B (future)** — ownership: owner FK on Coffee/Recipe/BrewSession,
  per-user filtering + enforcement, migration of existing rows.
- **Slice C (future)** — account UX: email verification, password reset,
  refresh tokens, logout-everywhere, profile.

## Non-Goals

- No per-row ownership, no per-user data filtering (Slice B).
- No refresh tokens, no email verification, no password reset (Slice C).
- No roles / authorities beyond "authenticated" (single implicit user role).
- No OAuth / social login / SSO.
- No change to the public share-link endpoints beyond keeping them open.

## Decisions

1. **Session mechanism:** stateless JWT bearer token. Signed HMAC-SHA256,
   self-contained, no server session state. Frontend stores the token and sends
   `Authorization: Bearer <token>` on every request.
2. **Registration:** open self-registration — public `POST /api/auth/register`
   creates an account and returns a token. No email verification in this slice.
3. **Password hashing:** BCrypt (`BCryptPasswordEncoder`).
4. **Token TTL:** single access token, default 24h (`brewdeck.auth.token-ttl`),
   no refresh. Re-login on expiry.
5. **User fields:** `email` + `passwordHash` only. No roles column, no display
   name (YAGNI for Slice A).
6. **Token storage (frontend):** `localStorage`. Accepted trade-off: reachable
   by JS (XSS). Chosen for the header-based cross-origin client and simple UX;
   hardening (HttpOnly cookies / refresh) is a later concern.
7. **Secret management:** `BREWDECK_JWT_SECRET` from env; app fails to start if
   missing outside dev. Dev default lives in `application-dev`.

## Architecture

### Backend — new `auth` domain package

- **Dependencies** (`pom.xml`): `spring-boot-starter-security`; `jjwt-api`,
  `jjwt-impl`, `jjwt-jackson` (0.12.x).
- **`V5__create_users.sql`** (Flyway):
  ```sql
  CREATE TABLE users (
      id            BIGSERIAL PRIMARY KEY,
      email         VARCHAR(255) NOT NULL UNIQUE,
      password_hash VARCHAR(255) NOT NULL,
      created_at    TIMESTAMP NOT NULL DEFAULT now()
  );
  ```
- **`User`** (entity) — `id`, `email` (unique, not null), `passwordHash`
  (not null), `createdAt`. No roles column.
- **`UserRepository`** — `Optional<User> findByEmail(String)`,
  `boolean existsByEmail(String)`.
- **`JwtService`** — HMAC-SHA256 with `BREWDECK_JWT_SECRET`; TTL from
  `brewdeck.auth.token-ttl` (default 24h).
  - `String generateToken(User user)` — subject = email, issued/expiry claims.
  - `String validateAndGetSubject(String token)` — throws on
    expired/tampered/wrong-secret.
- **`JwtAuthenticationFilter`** (`OncePerRequestFilter`) — reads
  `Authorization: Bearer`, validates via `JwtService`, loads the `User`, sets an
  authenticated `SecurityContext`. Missing/invalid token → proceed as anonymous
  (the authorization rules reject protected routes downstream).
- **`SecurityConfig`** — `SessionCreationPolicy.STATELESS`; CSRF disabled
  (bearer, no cookies); CORS delegated to the existing `WebConfig`
  configuration source; `JwtAuthenticationFilter` registered before
  `UsernamePasswordAuthenticationFilter`; `BCryptPasswordEncoder` bean; custom
  `AuthenticationEntryPoint` returning **401 JSON** in the `ErrorResponse`
  shape. Authorization rules:
  - **permitAll:** `/api/auth/register`, `/api/auth/login`, `/api/public/**`,
    swagger/openapi paths (`/swagger-ui/**`, `/v3/api-docs/**`),
    `/actuator/health`.
  - **authenticated:** all other requests, including `GET /api/auth/me` (so it
    returns 401 without a token).
- **`AuthService`** —
  - `AuthResponse register(RegisterRequest)` — 409 (`EmailAlreadyUsedException`)
    if `existsByEmail`; else hash password, save, generate token.
  - `AuthResponse login(LoginRequest)` — load by email, verify BCrypt; 401
    (`BadCredentialsException` → entry point / handler) on mismatch or unknown
    email.
  - `UserResponse me(String email)` — current user from the security context.
- **`AuthController`** — `POST /api/auth/register` (201),
  `POST /api/auth/login` (200), `GET /api/auth/me` (200).
- **DTOs (records):**
  - `RegisterRequest(@Email @NotBlank String email, @Size(min = 8) String password)`
  - `LoginRequest(@NotBlank String email, @NotBlank String password)`
  - `AuthResponse(String token, Instant expiresAt, String email)`
  - `UserResponse(Long id, String email, LocalDateTime createdAt)`
  - Never expose `User` / `passwordHash`.
- **`GlobalExceptionHandler`** — map `EmailAlreadyUsedException` → 409. 401s
  (bad credentials, missing/invalid token) flow through the
  `AuthenticationEntryPoint` as `ErrorResponse` JSON.
- **`OpenApiConfig`** — add a Bearer JWT security scheme so Swagger UI can
  authorize.

### Frontend — `auth` feature

- **`src/lib/api/types.ts`** — add `AuthResponse` (`token`, `expiresAt`,
  `email`) and `UserResponse` (`id`, `email`, `createdAt`).
- **`src/lib/api/auth.ts`** — `register(body): Promise<AuthResponse>`,
  `login(body): Promise<AuthResponse>`, `getMe(): Promise<UserResponse>`.
- **`src/lib/auth/tokenStore.ts`** — `localStorage` wrapper: `getToken`,
  `setToken`, `clearToken` (key `brewdeck.token`), SSR-guarded. Single source of
  truth for the raw token, importable by `apiFetch` without React.
- **`apiFetch`** (`src/lib/api/client.ts`, modified) — inject
  `Authorization: Bearer <token>` from `tokenStore` when present. On **401**:
  `clearToken()` and redirect to `/login`, unless the current path is `/login`,
  `/register`, or under `/share/` (redirect-loop guard). Public share fetch
  carries no token and is unaffected.
- **`AuthProvider`** (`src/lib/auth/AuthProvider.tsx`) — React Context for DI.
  Holds `user: UserResponse | null` and `status: 'loading' | 'authenticated' |
  'anonymous'`. On mount, if a token exists, hydrates via `getMe()` (invalid →
  clear + anonymous). Exposes `login`, `register`, `logout`. `login`/`register`
  are TanStack Query mutations; `getMe` a query. Wraps the app inside the
  existing `QueryClientProvider`.
- **`RequireAuth`** (`src/components/auth/RequireAuth.tsx`) — `anonymous` →
  redirect `/login`; `loading` → `Spinner`; `authenticated` → children. Applied
  in the authenticated layout segment.
- **Route structure** — the authenticated layout segment (dashboard, coffees,
  recipes, brew-sessions, brew-methods) is wrapped by `RequireAuth`. `/login`,
  `/register`, and `/share/[token]` live outside the guard.
- **Pages** (`export default`): `src/app/login/page.tsx`,
  `src/app/register/page.tsx` — React Hook Form + Zod
  (`src/lib/validation/authSchema.ts`: email format, password min 8, mirrors
  backend). Map server-400 `validationErrors` to fields; 409 / 401 → form-level
  Alert.
- **Layout** — logout button + current user email in the existing app shell.
  `logout()` clears the token, resets the query cache, redirects `/login`.

### Data flow

```
Register:  POST /api/auth/register {email,password}
  409 email exists           -> form Alert
  201 {token,expiresAt,email}-> setToken -> AuthProvider user set -> /dashboard

Login:     POST /api/auth/login {email,password}
  401 bad creds              -> form Alert
  200 {token,expiresAt,email}-> setToken -> /dashboard

Any /api/** call: apiFetch adds Bearer
  401 (missing/expired/tampered) -> clearToken -> /login (loop-guarded)

App boot (guarded segment): token present -> getMe()
  200 -> authenticated ;  401 -> clearToken -> /login

/share/[token]: no token, no guard -> public GET unaffected
```

## Error handling

- **401 (missing/invalid/expired token, bad credentials):** returned as
  `ErrorResponse` JSON by the `AuthenticationEntryPoint`; frontend `apiFetch`
  clears the token and redirects to `/login` (loop-guarded on
  `/login`, `/register`, `/share/**`).
- **409 (email already registered):** `EmailAlreadyUsedException` →
  `GlobalExceptionHandler` → `ErrorResponse`; frontend shows a form Alert.
- **400 (validation):** existing Bean Validation path → `validationErrors`;
  frontend maps to fields.
- **Missing `BREWDECK_JWT_SECRET` outside dev:** app fails to start (fail-fast).
- All failures surface as visible UI states, never `console.error`.

## Testing

### Backend
- **`JwtService` (unit):** generate→validate roundtrip returns the subject;
  expired token rejected; tampered signature rejected; wrong-secret rejected.
- **`AuthService` (unit, Mockito):** register hashes the password and persists;
  duplicate email → 409 (`EmailAlreadyUsedException`); login verifies BCrypt;
  bad password / unknown email → 401.
- **`AuthController` (MockMvc):** register 201 with non-null token; register
  duplicate 409; login 200; login bad creds 401; `/me` with valid token 200 and
  correct email; `/me` without token 401.
- **Integration (Testcontainers, real filter chain):** `GET /api/coffees` →
  401 without token, 200 with a valid token;
  `GET /api/public/recipes/{token}` → 200 **without** a token;
  `/api/auth/login` reachable without a token; full register → login →
  call-protected happy path. Control the dataset; use explicit page/size/sort on
  paginated bodies.

### Frontend
- **`auth.ts`:** correct URLs, methods, bodies; parses responses.
- **`tokenStore`:** set/get/clear; SSR-guarded (no `window` crash).
- **`apiFetch`:** injects `Authorization` when a token exists; omits it when
  absent; on 401 clears the token and redirects (and does **not** redirect on
  `/login`/`/register`/`/share`).
- **`AuthProvider`:** hydrates via `getMe` when a token exists; login sets the
  user; logout clears the token and resets the cache.
- **`RequireAuth`:** anonymous → redirect; loading → `Spinner`; authenticated →
  children.
- **Login + register forms:** Zod validation, server-error mapping (409/401 →
  Alert, 400 → field errors).
- Run the **full `vitest run`** — `apiFetch` and the root layout are shared;
  sibling tests mount them (see the sibling-test regression in project memory).

## Task decomposition (for the plan)

1. **Backend auth core** — deps, `V5` migration, `User`, `UserRepository`,
   `JwtService`, DTOs, `AuthService`, `EmailAlreadyUsedException` +
   `GlobalExceptionHandler` mapping. Unit tests. `./mvnw clean verify` green.
2. **Backend security wiring** — `JwtAuthenticationFilter`, `SecurityConfig`
   (rules, entry point, encoder, CORS), `AuthController`, `OpenApiConfig`
   bearer scheme. Controller + integration tests (protected 401/200, public
   open, auth open).
3. **Frontend auth client + state** — `AuthResponse`/`UserResponse` types,
   `auth.ts`, `tokenStore`, `apiFetch` bearer injection + 401 handling,
   `AuthProvider`. Tests (full suite — shared `apiFetch`/layout).
4. **Frontend screens + guard** — `authSchema`, `/login` + `/register` pages,
   `RequireAuth` on the authenticated layout segment, logout in the layout.
   Tests (full suite).
5. **Docs** — roadmap (open Phase 6, Auth Slice A → Done, note B/C pending),
   project-state, Postman (register / login / me requests + `{{authToken}}`
   variable; add bearer auth to protected requests).

## Global constraints

- **Backend:** organize by domain (`auth`); never return entities from
  controllers — map to records; Bean Validation on inputs; no special symbols
  like `°C` in messages (write "degrees Celsius"); single-resource GET returns
  the DTO directly (not `PageResponse`); JaCoCo/Sonar DTO-exclusion parity where
  applicable; stateless security (no server session).
- **Token:** HMAC-SHA256 via jjwt 0.12.x; secret from `BREWDECK_JWT_SECRET`
  (fail-fast when missing outside dev); default TTL 24h.
- **Passwords:** BCrypt; never log, return, or expose `passwordHash`.
- **Frontend:** strict TypeScript, no `any`; import domain types from
  `@/lib/api`; named exports everywhere except Next.js `page.tsx`/`layout.tsx`
  (which use `export default`); absolute `@/` imports; TanStack Query for server
  state (login/register mutations, `getMe` query); React Context only for DI
  (`AuthProvider`), not high-frequency state; handle loading / error / empty
  visibly (no `console.error`); token only in `tokenStore`.
- **Public endpoints:** `/api/public/**`, `/api/auth/register`, and
  `/api/auth/login` stay open (`/api/auth/me` is authenticated); the
  `/share/[token]` page stays outside the route guard.
- **Commands:** frontend from `brewdeck-web/` (`npm run test`, `type-check`,
  `build`; scope `lint:fix` to changed files); backend `./mvnw spotless:apply`
  then `./mvnw clean verify` from `brewdeck-api/`.
- Conventional Commits; scopes `api` (backend), `web` (frontend), `docs`.
