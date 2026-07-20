# Generic release-management prompts

## Prepare a release candidate

```text
@release-manager Prepare the release candidate for [VERSION].

Build the exact scope, validate all product, architecture, implementation,
quality, security, performance, platform, database, documentation, and final
review gates.

Produce versioning, deployment, smoke-test, monitoring, rollback, release notes,
and a GO, GO WITH CONDITIONS, NO-GO, or BLOCKED decision.

Do not tag, publish, merge, or deploy.
```

## Go/no-go review

```text
@release-manager Conduct a formal go/no-go review using the available evidence.

List every mandatory gate, missing artifact, unresolved finding, condition,
approval, migration constraint, recovery limitation, and post-release
verification requirement.
```

## Patch release

```text
@release-manager Prepare a patch release for [BUG OR CVE].

Confirm that the scope is minimal, compatibility is preserved, regression tests
exist, security evidence is current, rollback risk is understood, and release
notes are appropriate.
```

## Multi-service release

```text
@release-manager Coordinate the release plan for [SERVICES].

Define artifact identities, compatibility matrix, deployment order, migrations,
feature flags, health verification, smoke tests, rollback boundaries, and
communications.
```

# BrewDeck release prompts

## BrewSession release

```text
@release-manager Prepare the BrewDeck BrewSession release.

Verify backend, frontend, Flyway, ownership, units, validation, session history,
accessibility, tests, feature flags, documentation, smoke tests, and rollback
compatibility.
```

## AI recipe suggestions limited rollout

```text
@release-manager Prepare a limited rollout for BrewDeck AI recipe suggestions.

Require AI evaluation, prompt version, provider configuration, privacy review,
cost and latency guardrails, feature flag, kill switch, fallback, user
confirmation, observability, and rollback criteria.

Do not enable the flag.
```

## BrewDeck dependency patch

```text
@release-manager Prepare the BrewDeck security and dependency patch release.

Verify the effective dependency tree, CVE remediation, scanner evidence,
Spring Boot compatibility, Java 21, Docker image, frontend build, tests, and
whether rollback would reintroduce vulnerabilities.
```

# BrickDeck release prompts

## Rebrickable import release

```text
@release-manager Prepare the BrickDeck Rebrickable import release.

Verify API-key configuration, timeout and rate-limit handling, idempotency,
cache provenance, partial data, duplicate imports, database changes, tests,
observability, runbooks, smoke tests, and rollback.
```

## Complete theme import release

```text
@release-manager Prepare a controlled release for complete theme imports.

Define worker or API deployment order, migration ownership, feature flag,
limited rollout, progress verification, duplicate prevention, reconciliation,
capacity monitoring, and disablement criteria.
```

## Collection release

```text
@release-manager Prepare the user-collection feature release.

Verify ownership, visibility, authorization, external refresh safety, database
constraints, frontend states, tests, migration compatibility, and user-facing
release notes.
```

# Security, migrations, and operations prompts

## Migration-heavy release

```text
@release-manager Prepare a migration-heavy release.

Require migration review, existing-data compatibility, lock-risk assessment,
old/new application compatibility, deployment order, forward-fix path, data
reconciliation, verification, and rollback limitations.
```

## Security release

```text
@release-manager Prepare an urgent security release without weakening normal
quality gates.

Separate sensitive internal details from user-facing notes.
Require scanner, authorization, regression, container, deployment, and rollback
evidence.
```

## Release rollback plan

```text
@release-manager Create a rollback and roll-forward plan for [RELEASE].

Cover application artifacts, database schema and data, frontend/API
compatibility, cache, feature flags, external integrations, dependency CVEs,
AI prompt versions, verification, and incident escalation.
```

## Post-release verification

```text
@release-manager Define post-release verification for [RELEASE].

Include health, smoke tests, error rate, latency, database state, integrations,
feature flags, security alerts, user reports, monitoring duration, rollback
criteria, and completion communication.
```
