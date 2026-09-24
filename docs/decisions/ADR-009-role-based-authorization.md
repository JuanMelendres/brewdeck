# ADR-009: Role-based authorization (USER / ADMIN)

## Status
Accepted

## Date
2026-09-24

## Context
A backend security audit (2026-09-24) found that any authenticated user could create, rename, or delete brew methods. Brew methods are a shared catalog read by every user, so one account could rename "V60" for everyone or delete methods. Authentication existed (ADR-005) and per-user ownership existed for coffees, recipes, and sessions (Phase 6 Slice B), but there was no authorization model. `FeatureFlagAdminService` was already kept off HTTP for the same reason.

## Decision
Introduce a minimal role model:

- **Storage:** `users.role VARCHAR(20) NOT NULL DEFAULT 'USER'` with `CHECK (role IN ('USER','ADMIN'))` (Flyway V15), mapped to a `Role` enum on `User`.
- **Resolution:** `JwtAuthenticationFilter` already loads the user on every request, so it grants `ROLE_<role>` from the database. The role is **not** a JWT claim: a role change applies on the next request instead of after the token's TTL (15 min).
- **Enforcement:** `SecurityConfig` limits `POST`/`PUT`/`DELETE /api/brew-methods/**` to `ROLE_ADMIN`. `GET` stays open to any authenticated user. A `RestAccessDeniedHandler` returns a JSON `403` `ErrorResponse`; anonymous calls still get `401`.
- **Admin bootstrap:** no endpoint can grant a role. `AdminBootstrap` (an `ApplicationRunner`) promotes the existing account named by `BREWDECK_ADMIN_EMAIL` at startup. The operator registers the account first, then sets the variable and restarts. Because it never creates an account, nobody can pre-register the address to take the admin role.
- **Exposure:** `GET /api/auth/me` returns `role`, so the frontend can hide admin-only actions. Hiding is a convenience only; the backend enforces access.

## Consequences
- **Positive:** closes the audit's critical finding; unblocks a protected feature-flag admin API.
- **Positive:** takes effect immediately after a role change; no token revocation needed.
- **Negative:** removing `BREWDECK_ADMIN_EMAIL` does not demote the account; demotion is a manual SQL update.
- **Negative:** `RestAccessDeniedHandler` uses Jackson 2 `ObjectMapper`, adding one class to the Jackson 3 port (ADR-008).

## Alternatives Considered
- **Role as a JWT claim:** rejected. It saves nothing (the filter already hits the DB), and a stale token would keep a revoked admin role for up to 15 minutes.
- **Separate `user_roles` table:** deferred. Two roles do not need many-to-many; migrate if finer permissions appear.
- **Remove brew-method write endpoints:** rejected by the owner; the catalog needs to be maintained.
- **Admin grant endpoint:** rejected. It needs an existing admin, so it cannot bootstrap the first one, and it adds attack surface.

## Notes
Follow-up: user-owned private brew methods alongside the shared catalog (the owner's product intent). This ADR keeps the shared catalog admin-only; private methods will be owner-scoped like coffees and recipes.
