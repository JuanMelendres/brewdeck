# ADR-007: PostgreSQL-backed feature flags

## Status
Accepted

## Date
2026-07-16

## Context
BrewDeck needs to merge and deploy complex or incomplete work (AI, risky workflows, multi-PR
features) without exposing it to all users in production. We want per-environment control, a runtime
kill/release switch that does not require a redeploy, and a single source of truth the backend can
enforce. We already run PostgreSQL + Flyway and a single modular-monolith API; we do not want to add
an external provider (LaunchDarkly, Unleash, Flagsmith) or new infrastructure (Redis) at this stage.

## Decision
Store flags in a **`feature_flags`** table (one row per `feature_key` + `environment`, unique
together) and evaluate them through a **`FeatureFlagService`** abstraction with a database-backed
implementation. Key choices:

- **Backend is the source of truth.** Protected operations call `requireEnabled(key)` at the service
  layer before any side effect; the frontend only hides UI.
- **Long identity PK**, JSONB `configuration`, `LocalDateTime` timestamps — consistent with every
  other BrewDeck table (the reference UUID design was adapted to the project's conventions).
- **Caffeine cache** (new `spring-boot-starter-cache`) with a 45s TTL in front of the table so the
  hot path avoids a query per request; admin mutations evict immediately. No Redis — a single
  instance with a short TTL is sufficient; the short TTL also bounds cross-instance staleness.
- **Fail-safe evaluation.** Unknown flag, disabled flag, rollout exclusion, or datastore error all
  resolve to *disabled*. An unknown feature is never enabled automatically.
- **Environment** derived from the active Spring profile (`local/dev/test/staging/prod`), with an
  optional override, defaulting to `prod` (most restrictive) when unrecognized.
- **Deterministic rollout** (`hash(key:subject) % 100`) is modeled now but seeded at 0/100 only.
- **No admin HTTP surface.** BrewDeck has no role/authorization model yet, so flag mutation lives in
  an internal `FeatureFlagAdminService` (used by seeds/tests) rather than an unauthenticated
  endpoint. A protected admin API is a follow-up, gated on RBAC.
- **Disabled-feature status by flag type:** RELEASE/EXPERIMENT/PERMISSION → `404` (not
  discoverable), OPERATIONAL/KILL_SWITCH → `503`. Never `500`.

## Consequences
- **Positive:** runtime, per-environment toggles with no redeploy; one enforcement point; reuses
  existing Postgres/Flyway; frontend and backend share one contract via `/api/feature-flags`.
- **Positive:** ships dark safely — production defaults incomplete features to disabled.
- **Negative:** adds a cache dependency and a small evaluation path to protected operations.
- **Negative:** flags are per-instance cached; a change can take up to the TTL to fully propagate
  across instances (acceptable at current single-instance scale).
- **Negative:** temporary flags are tech debt — each must carry an owner, expiry, and removal
  condition, and be cleaned up (see the registry in `docs/development/feature-flags.md`).

## Alternatives Considered
- **External provider (LaunchDarkly/Unleash/Flagsmith)** — rejected: new vendor/infra, overkill for
  current scale.
- **Spring profiles / env vars only** — rejected: not runtime-changeable per user, no gradual
  rollout, no single queryable source of truth. Env vars are retained only as a possible emergency
  global override.
- **UUID PK per the reference schema** — rejected: the whole schema uses `Long` identity; a lone
  UUID PK would break full-stack consistency.
- **Redis cache** — rejected: not otherwise part of the architecture; Caffeine + short TTL suffices.

## Notes
First protected feature: the **AI recipe assistant** (`brew-recipe-ai-assistant`), migrating the
ad-hoc `AI_ENABLED` property gate to a DB flag (strangler-fig). `AI_ENABLED` still governs provider
wiring (the hexagonal port from [ADR-006](ADR-006-ai-integration-hexagonal-port.md)); the flag
governs user-facing release. Revisit the admin API and cross-instance cache invalidation when RBAC
and multi-instance deployment land.
