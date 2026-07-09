# Test Plan

How to run the suites and what each guards. Commands assume the repo root unless noted.

## Backend (`brewdeck-api/`)

```bash
# format first (required by the pre-commit gate)
./mvnw spotless:apply

# full verification: unit + integration + coverage
./mvnw clean verify

# focused
./mvnw -Dtest=RecipeServiceTest test

# static analysis + security (not part of clean verify)
./mvnw pmd:check
./mvnw dependency-check:check
```

> Docker must be running for Testcontainers-based integration tests.

## Frontend (`brewdeck-web/`)

```bash
npm run test          # Vitest suite (vitest run)
npm run type-check    # strict tsc --noEmit
npm run lint          # ESLint (read-only)
npm run build         # production build (also type-checks)
```

## CI

GitHub Actions runs on PRs (see [`.github/workflows/`](../../.github/workflows/)):

- `api-ci` — backend build + tests
- `sonar` — SonarCloud analysis / quality gate
- `qodana` — JetBrains Qodana JVM inspection
- `security` — dependency / security scan

A PR should not be merged while any of these are failing.

## Pre-merge checklist

- [ ] `./mvnw spotless:apply` then `./mvnw clean verify` green
- [ ] `./mvnw pmd:check` green (not covered by `clean verify`)
- [ ] Frontend `npm run test` + `npm run type-check` green
- [ ] New/changed behavior has tests
- [ ] CI checks green on the PR
