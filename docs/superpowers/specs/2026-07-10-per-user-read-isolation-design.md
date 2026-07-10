# Per-User Read Isolation (Phase 6 Slice B.2)

- **Date:** 2026-07-10
- **Status:** Approved (design)
- **Phase:** 6 — Auth & Multi-User
- **Builds on:** Slice B.1 (owner_id FK + create-time stamping, PR #71)

## Goal

Complete per-user data isolation. Every read — collection lists, get-by-id,
update/delete guards, favorites, recipe stats, analytics rankings, and the
dashboard summary — is scoped to the authenticated user's owned rows. Close the
nullable gap left by B.1 by enforcing `owner_id NOT NULL`.

After this slice a user can only see and mutate data they own; cross-user rows
are invisible.

## Non-Goals

- Sharing/collaboration between users (public share links already cover the
  one intentional cross-user path and are unchanged).
- Roles/permissions/admin. Every authenticated user is a plain owner.
- Ownership transfer or re-assignment.
- Any frontend change. The API contract (shapes, status codes) is unchanged;
  only the row set each user sees narrows. Frontend work, if any, is a separate
  slice.

## Ownership Model

| Resource        | Owned? | Notes |
| --------------- | ------ | ----- |
| `coffees`       | Yes    | `owner_id` stamped on create (B.1). |
| `recipes`       | Yes    | `owner_id` stamped on create (B.1). |
| `brew_sessions` | Yes    | `owner_id` stamped on create (B.1). |
| `brew_methods`  | **No** | Shared, seeded reference data. No owner column; lists stay global. |

Public share endpoints (`GET /api/public/recipes/{token}`, share/unshare) remain
token-based and cross-user — an explicit, intentional bypass of ownership.

## Design Decisions (locked)

1. **Analytics + dashboard: per-user.** Rankings and counts reflect only the
   current user's data. No cross-user aggregates.
2. **Cross-user access by id: 404 Not Found.** A row owned by someone else is
   treated as non-existent. Reuses the existing `EntityNotFoundException` →
   `GlobalExceptionHandler` 404 path; no new exception type. Hides existence.
3. **`owner_id NOT NULL`: enforce now (V7).** All existing rows were backfilled
   in B.1 and every create stamps an owner, so the constraint is safe to add.

## Architecture

### Current-user resolution

`CurrentUserProvider.require()` (from B.1) returns the authenticated `User`.
Read paths use `currentUserProvider.require().getId()` as the `ownerId`. Services
that previously had no dependency on it (analytics, stats, dashboard) gain the
injection.

### 1. Collection lists (Specifications)

Add `hasOwner(Long ownerId)` to `CoffeeSpecification`, `RecipeSpecification`,
and `BrewSessionSpecification`. Each `*Service.search(...)` `.and()`s it onto the
existing filter chain, so the owner predicate always applies regardless of the
user-supplied filters.

### 2. Get-by-id / update / delete / favorite / stats

Replace ownership-agnostic lookups with owner-scoped ones:

- `findById(id)` → `findByIdAndOwnerId(id, ownerId)` (Spring Data derived query).
- `existsById(id)` → `existsByIdAndOwnerId(id, ownerId)`.

Applies to:

- `CoffeeService.findById/update/delete`.
- `RecipeService.findById/update/delete/favorite/unfavorite`.
- `BrewSessionService.findById/update/delete`.
- `RecipeStatsService` existence guard.
- `BrewSessionService.create` recipe lookup — the parent recipe must be owned by
  the caller (can't log a session against another user's recipe) → owner-scoped
  recipe lookup, 404 when not owned.
- `RecipeService.create` coffee/method lookups — coffee must be owned; method is
  shared, so the method lookup stays global.

Not-owned → `EntityNotFoundException` → 404 (decision 2).

### 3. Derived finders

Owner-scoped variants:

- `findByFavoriteTrue(Pageable)` → `findByFavoriteTrueAndOwnerId(ownerId, Pageable)`.
- `countByFavoriteTrue()` → `countByFavoriteTrueAndOwnerId(ownerId)` (dashboard).
- `findByCoffeeId` / `findByMethodId` (and their `Page` overloads) →
  `...AndOwnerId` where used by owner-scoped flows.

### 4. Analytics queries (per-user)

Add an `:ownerId` parameter and owner predicate to each `@Query`:

- `RecipeRepository.findMostUsedCoffees` — `where r.owner.id = :ownerId`.
- `BrewSessionRepository.findAverageRating` — `and s.owner.id = :ownerId`.
- `BrewSessionRepository.findStatsByRecipeId` — `and s.owner.id = :ownerId`.
- `BrewSessionRepository.findTopRated` — `and s.owner.id = :ownerId`.
- `BrewSessionRepository.findMostBrewed` — `where s.owner.id = :ownerId`.
- `BrewMethodRepository.findUsage` — move the owner filter into the join so
  shared methods still appear with a per-user (possibly zero) count:
  `left join Recipe r on r.method = m and r.owner.id = :ownerId`.

The corresponding services (`CoffeeService.getMostUsed`, recipe analytics,
method usage, `RecipeStatsService`) inject `CurrentUserProvider` and pass the id.

### 5. Dashboard summary

`DashboardService` counts (coffees, recipes, sessions, favorites, average
rating) become per-user by delegating to the owner-scoped repository methods /
counts above.

### 6. AI paths

- `improve` loads the target recipe by id → owner-scoped (404 when not owned).
- `suggest` produces an ephemeral recipe from a prompt with no persisted id →
  unchanged.

### 7. NOT NULL enforcement (V7)

```sql
ALTER TABLE coffees       ALTER COLUMN owner_id SET NOT NULL;
ALTER TABLE recipes       ALTER COLUMN owner_id SET NOT NULL;
ALTER TABLE brew_sessions ALTER COLUMN owner_id SET NOT NULL;
```

Entities: `@JoinColumn(name = "owner_id", nullable = false)` on the three
`owner` associations.

## Error Handling

No new error surface. Owner-scoped lookups that miss throw the existing
`EntityNotFoundException`, already mapped to 404 by `GlobalExceptionHandler`.
Validation and other paths are unchanged. `CurrentUserProvider.require()`
throwing `IllegalStateException` remains an internal-invariant failure (a genuine
misconfiguration), not an expected client outcome behind the auth gate.

## Testing

### Unit (service)

- By-id tests: stub `findByIdAndOwnerId(id, ownerId)` and
  `existsByIdAndOwnerId`; add a not-owned → 404 case per resource.
- Analytics / stats / dashboard tests: inject and stub `CurrentUserProvider`,
  assert the owner id is threaded into the repository call
  (`verify(...).findTopRated(ownerId, ...)` etc.).
- Specification tests: cover `hasOwner`.

### Repository (`@DataJpaTest`)

- New owner-scoped derived finders and the `AndOwnerId` variants: seed rows for
  two owners, assert only the target owner's rows return.
- Analytics `@Query`s: seed two owners, assert rankings/counts include only the
  target owner's data; method-usage still lists shared methods with a per-user
  count.

### Integration (Testcontainers) — the heavy part

`PostgresIntegrationTest` already seeds a `user` (email `user`, the
`@WithMockUser` default). Existing analytics/dashboard integration tests that
seed rows directly via `repository.save(...)` currently persist them with a
**null owner**; after this slice those rows fall outside the owner filter and
assertions break.

Fix: seed owned data. Give the base class a helper that returns the seeded
`User`, and have each integration test set `owner` on the coffees/recipes/
sessions it persists (or create them through the API, which stamps owner
automatically). Add a focused cross-user test: data owned by a second user must
not appear in the first user's lists, analytics, dashboard, or by-id lookups
(404).

## Migration & Rollout

- Additive read filtering + a `NOT NULL` tightening. No data migration beyond
  B.1's backfill.
- Backward compatible at the API contract level (same shapes/status codes); the
  visible row set narrows per user. Acceptable and intended for a multi-user app
  still pre-GA.

## Scope & Commit Breakdown

One atomic PR — partial isolation (some resources scoped, others not) would be
an inconsistent, confusing half-state. Suggested commits within the PR:

1. Owner-scoped lists + by-id/update/delete/favorite (coffees, recipes,
   sessions) + specs, with unit/repository tests.
2. Per-user analytics + dashboard + recipe stats + AI improve, with tests.
3. V7 `NOT NULL` + `nullable = false` entities.
4. Integration-test ownership seeding + cross-user isolation test.
5. Docs (project-state, roadmaps, database-design).

## Verification

- `./mvnw spotless:apply`
- `./mvnw clean verify` (all suites green)
- `sh mvnw pmd:check` (napkin: `verify` skips PMD/Sonar)

## Follow-ups (out of scope)

- Slice C (account UX: email verification, password reset, refresh tokens,
  profile).
- Frontend surfacing of "your data only", if any UX copy needs to change.
