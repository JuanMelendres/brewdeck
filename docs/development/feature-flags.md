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
| `brew-recipe-ai-assistant` | RELEASE | Backend Team | `local` (V26, free local model via Ollama; suspended elsewhere since V14) | Disabled | 2026-12-01 | AI suggest/improve output validated across brew methods and frontend UX polished; then remove the flag, its `requireEnabled` checks, the UI gating, and the seed rows. |
| `auth-require-email-verification` | RELEASE | Juan (product owner) | `staging` (V29, 2026-10-16); others off since V20 — enable per env after SMTP works | Disabled | 2026-12-31 | Enabled in prod and stable for 2 weeks; then make `EmailVerificationRequiredFilter` and the retention purge's account step (`RetentionPurgeService`, ADR-021) unconditional, remove the flag rows, `FeatureKeys`/`FrontendFeatureFlag` entries, and the frontend alias. Tests for both states: `EmailVerificationEnforcementIntegrationTest` (backend), `EmailVerificationGate.test.tsx` (frontend). See [ADR-012](../decisions/ADR-012-require-email-verification.md) (includes lock-out recovery). **Turning it on also starts the retention purge of unverified accounts** in that environment: reminders at once for accounts older than 5 days, deletions 2 days later ([ADR-021](../decisions/ADR-021-retention-purge.md)). |
| `auth-email-change` | RELEASE | Juan (product owner) | `local`, `staging` (V34, 2026-10-20); others off since V33 | Disabled | 2027-01-31 | Enabled in staging and prod with no issues for 2 weeks; then remove the `requireEnabled` checks in `EmailChangeService`, the flag rows, the `FeatureKeys` and `FrontendFeatureFlag` entries and the frontend alias `emailChange` (`<FeatureFlag name="emailChange">` on the account page). Cancel is never guarded. Tests for both states: `EmailChangeIntegrationTest` (backend); the account page hides `ChangeEmailForm` while off. See [ADR-019](../decisions/ADR-019-secure-email-change.md). |
| `legal-terms` | RELEASE | Juan (product owner) | none (V36, 2026-10-20); enable after the lawyer approves `brewdeck-web/content/legal/` | Disabled | 2027-03-31 | Enabled in staging and prod after legal review, stable for 2 weeks; then make the sign-up consent unconditional, drop the flag rows, `FeatureKeys.LEGAL_TERMS`, the `published` field of `GET /api/public/legal` and the web gating. Tests for both states: `LegalTermsIntegrationTest`, `SmtpMailAdapterTest` (backend); `LegalDocument.test.tsx`, `LegalLinks.test.tsx`, `RegisterForm.test.tsx` (web). |

**Retired:** `web-i18n-spanish` (RELEASE, ADR-015) removed 2026-10-14 by V28 after the owner's
Spanish review and prod enablement; English and Spanish are always available.

**Suspended (2026-09-08):** owner has paused the AI integration indefinitely (no intent to pay for
an LLM API subscription right now). Code, port, adapter, and frontend gating stay in place
untouched; `V14__suspend_ai_recipe_assistant_flag.sql` flips `local`/`dev` to disabled so the
flow can't be exercised (and can't incur API cost) until the decision is revisited. Re-enable by
flipping the flag back (new migration or admin update) once ready to resume — no code changes
needed.
