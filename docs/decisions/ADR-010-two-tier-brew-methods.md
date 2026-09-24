# ADR-010: Two-tier brew methods (shared catalog + private per user)

## Status
Accepted

## Date
2026-09-24

## Context
Coffees, recipes, and brew sessions are private to their owner (Phase 6 Slice B), and others can read a recipe only through a share link. Brew methods were a single global list. ADR-009 made that list admin-only, which stopped cross-user tampering but also stopped regular users from adding methods. The owner's product intent: a **shared catalog** everyone can use, plus **methods each user creates** that are theirs alone.

## Decision
- **Schema (Flyway V16):** nullable `brew_methods.owner_id` FK to `users`. `NULL` means shared catalog. Every existing row, including the V2 seed, stays shared.
- **Uniqueness:** the global `UNIQUE(name)` is replaced by two partial unique indexes: `name` among shared methods, and `(owner_id, name)` among private ones. Two users may each own "My V60", and a private method may reuse a catalog name.
- **Visibility:** a user sees the shared catalog plus their own methods (`findVisibleTo` / `findVisibleById`). Another user's private method returns `404`, as it does for coffees and recipes. This applies to list/get, usage analytics, the dashboard count, recipe create/update, and AI suggest.
- **User API (`/api/brew-methods`):** `POST` creates a private method owned by the caller. `PUT`/`DELETE` work only on the caller's private methods: `403` on a shared method (it exists but is read-only), `404` on another user's.
- **Admin API (`/api/admin/brew-methods`):** `POST`/`PUT`/`DELETE` on the shared catalog only. `SecurityConfig` guards `/api/admin/**` with `ROLE_ADMIN`. A private method returns `404` there, so admins cannot edit users' private data through the catalog.
- **Response:** `BrewMethodResponse.shared` lets the client know which methods it may edit.
- **Errors:** the service layer throws Spring Security's `AccessDeniedException` for the shared-method case, and `GlobalExceptionHandler` maps it to the same JSON `403` as `RestAccessDeniedHandler`.

## Consequences
- **Positive:** matches the ownership model of the other resources; users regain self-service methods without being able to affect anyone else.
- **Positive:** separate admin endpoints keep the two rule sets apart instead of branching on role inside one endpoint.
- **Negative:** methods that users created before V16 became shared catalog entries. An admin may need to clean up the catalog.
- **Negative:** deleting a method that recipes still use fails with the generic `409` (unchanged). A clearer message is tracked in the audit backlog.

## Alternatives Considered
- **One endpoint that branches on role** (admin `POST` creates shared, user `POST` creates private): rejected. The same request would mean different things depending on who sends it, and an admin could not create a private method.
- **Copy the catalog into each user's account:** rejected. It duplicates data, and catalog fixes would never reach users.
- **Keep ADR-009's admin-only writes:** rejected; users need their own methods.
