# ADR-003: Testcontainers for integration testing

## Status
Accepted

## Date
2026-07-09

## Context
Integration tests must exercise real SQL (Flyway migrations, Specifications, constraints) rather than an in-memory substitute that behaves differently from production PostgreSQL.

## Decision
Use **Testcontainers** to spin up a real PostgreSQL 16 container for integration tests, alongside JUnit 5, Mockito, and MockMvc for the unit/controller layers. Coverage is measured with JaCoCo.

## Consequences
- **Positive:** integration tests run against the same engine as production; migrations are validated.
- **Positive:** no dialect drift between test and prod.
- **Negative:** requires Docker available in local and CI environments.
- **Negative:** slower than in-memory DBs; mitigated by container reuse.

## Alternatives Considered
- **H2 in-memory** — rejected: dialect differences hide real bugs (types, partial indexes).
- **Shared external test DB** — rejected: flaky, shared-state coupling between tests.

## Notes
Integration tests must use explicit `page`/`size`/`sort` and must not assume a single record exists in a shared dataset (assert `>=`).
