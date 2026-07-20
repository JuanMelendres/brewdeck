# Generic DevOps and platform prompts

## Initial pull-request CI

```text
@devops-platform-engineer Create or improve the pull-request CI pipeline.

Use repository scripts to validate formatting, lint, type checking, unit tests,
integration tests, migration validation, build, dependency scanning, secret
scanning, and container validation where applicable.

Use least-privilege permissions and pinned actions.
Do not modify application code or deploy.
```

## Docker and Compose

```text
@devops-platform-engineer Review and improve Dockerfiles and Docker Compose.

Focus on multi-stage builds, supported images, non-root runtime, caching, health
checks, secrets, PostgreSQL persistence, migration ordering, minimal ports, and
reproducible local startup.

Do not delete volumes or connect to shared databases.
```

## Release preparation

```text
@devops-platform-engineer Design and implement release preparation with
immutable artifacts, version metadata, SBOM, scanning, retention, approvals,
verification, and recovery guidance.

Do not publish or deploy.
```

# BrewDeck platform prompts

## Local environment

```text
@devops-platform-engineer Improve BrewDeck's Docker Compose environment for the
Spring Boot API, Next.js frontend, PostgreSQL 16, health checks, Flyway ordering,
persistent local data, safe placeholders, and troubleshooting.

Provide startup and shutdown commands that do not delete data.
```

## Monorepo CI

```text
@devops-platform-engineer Create or improve BrewDeck's monorepo CI.

Include Java 21, Gradle checks, frontend lint and type-check, tests,
Testcontainers or PostgreSQL readiness, Flyway validation, dependency scanning,
container validation, and clear failure summaries.

Do not deploy.
```

## Feature-flag environments

```text
@devops-platform-engineer Define BrewDeck feature-flag configuration for local,
CI, preview, staging, and production.

Separate frontend-public flags from backend-private configuration, validate
required values, and keep incomplete features disabled by default.
```

# BrickDeck platform prompts

## Local environment

```text
@devops-platform-engineer Improve BrickDeck's local Compose environment with the
Spring Boot API, PostgreSQL, a Rebrickable key placeholder, persistent user data,
health checks, Flyway ordering, safe shutdown, and import troubleshooting.

Never expose or read the real API key.
```

## CI and image validation

```text
@devops-platform-engineer Create or improve BrickDeck CI with Java 21, Gradle,
tests, migration validation, dependency scanning, container build validation,
SBOM generation, and artifact reporting.

Do not publish or deploy.
```

## Import observability

```text
@devops-platform-engineer Define platform observability for BrickDeck imports:
duration, success, partial failure, upstream 401, rate limiting, timeout, retry,
and correlation identifiers.

Create application-agent handoffs for missing code instrumentation.
```

# Security, release, and operations prompts

## GitHub Actions security

```text
@devops-platform-engineer Review all GitHub Actions workflows for token
permissions, action pinning, untrusted input, fork secrets, cache poisoning,
artifacts, concurrency, environment protections, and publishing boundaries.

Implement platform-only remediations and do not weaken required checks.
```

## Container security and SBOM

```text
@devops-platform-engineer Add Docker build validation, image scanning, SBOM
generation, artifact retention, and actionable reporting.

Do not suppress vulnerabilities merely to pass CI. Coordinate triage with
@security-auditor.
```

## Deployment and recovery plan

```text
@devops-platform-engineer Prepare a deployment and recovery plan for [SERVICE]
covering artifact identity, variables, migration ordering, health verification,
smoke tests, rollback, roll-forward, database limitations, observability,
approvals, and audit evidence.

Do not deploy.
```
