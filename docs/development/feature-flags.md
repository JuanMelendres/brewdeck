# Feature Flags

PostgreSQL-backed feature flags let us merge and deploy complex or incomplete work without exposing
it to all users. Design rationale: [ADR-007](../decisions/ADR-007-postgres-feature-flags.md).

## What they are

A flag is one row in `feature_flags`, scoped to a single environment (`feature_key` + `environment`
is unique). The backend evaluates flags through `FeatureFlagService` and is the **source of truth**;
the frontend only hides UI based on `/api/feature-flags`.

## When to create a flag

Create a `RELEASE` flag when a feature is incomplete, experimental, risky, multi-PR, depends on an
unfinished integration, uses an external/AI provider whose behaviour must be validated, or should
not yet be exposed in production.

**Do not** flag: small UI tweaks, refactors, formatting, bug fixes that must ship immediately,
security fixes, non-reversible DB fixes, permanent business rules, or authorization (flags are never
a substitute for authz).

## Flag types

`RELEASE` (default, most incomplete work) · `EXPERIMENT` · `OPERATIONAL` · `PERMISSION` ·
`KILL_SWITCH`. Type drives the disabled-response status: RELEASE/EXPERIMENT/PERMISSION → `404`,
OPERATIONAL/KILL_SWITCH → `503`.

## How to create / enable / disable

1. Add a Flyway migration seeding one row per environment (see `V13__seed_ai_recipe_assistant_flag.sql`).
   Production and staging default to `enabled = FALSE`.
2. Reference the key from a constant in `FeatureKeys` (never a raw string).
3. Protect the backend: call `featureFlagService.requireEnabled(FeatureKeys.X)` **before** any side
   effect (persistence, events, external calls, notifications).
4. Expose to the frontend only if the UI needs it: add it to `FrontendFeatureFlag` (backend key →
   camelCase alias) and to `FeatureFlagName` in `src/lib/api/featureFlags.ts`.
5. Gate the UI with `<FeatureFlag name="...">` / `useFeatureFlag(...)` / `<FeatureRouteGuard>`.

Enabling/disabling at runtime (until an admin API exists) is done via SQL update + cache eviction,
or programmatically through `FeatureFlagAdminService` (evicts cache and audit-logs the change). A new
Flyway migration is the durable way to change a default.

## Environments

`local`, `dev`, `test`, `staging`, `prod`, derived from the active Spring profile (override:
`brewdeck.feature-flags.environment`). Unknown/none → `prod` (fail safe). **Production defaults
incomplete features to disabled.**

## Frontend vs backend checks

The backend enforces every flag on every protected request. The frontend gate is presentation only —
hiding a button is not security. Direct URL access to a disabled feature is handled by
`FeatureRouteGuard`; the backend still returns `404`/`503`.

## Caching

Caffeine, 45s TTL, keyed by `featureKey:environment`. Reads are served from cache; admin mutations
evict immediately. On a datastore error the evaluation fails safe to disabled.

## Testing both states

- Backend: `DatabaseFeatureFlagServiceTest` (enabled/disabled/missing/rollout/datastore),
  `FeatureFlagIntegrationTest` (endpoint, cache hit + eviction, AI `404` when off → reaches business
  logic when on), `FeatureFlagRepositoryTest` (unique constraint).
- Frontend: `FeatureFlag`, `FeatureFlagProvider`, `FeatureRouteGuard`, `featureFlags` client tests
  cover enabled, disabled, loading, error, and direct-navigation.

## How to remove a flag

When the feature is stable and enabled for everyone: remove the `requireEnabled` calls and the
conditional UI, keep the final enabled implementation, delete obsolete disabled-path tests, remove
the seed row via a new migration, drop it from `FeatureKeys` / `FrontendFeatureFlag` /
`FeatureFlagName`, update this doc and the registry, and grep to confirm no references remain.

## Active flag registry

| Flag | Type | Owner | Environments (enabled) | Prod default | Expires | Removal condition |
|------|------|-------|------------------------|--------------|---------|-------------------|
| `brew-recipe-ai-assistant` | RELEASE | Backend Team | local, dev | Disabled | 2026-12-01 | AI suggest/improve output validated across brew methods and frontend UX polished; then remove the flag, its `requireEnabled` checks, the UI gating, and the seed rows. |
