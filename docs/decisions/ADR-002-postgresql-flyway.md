# ADR-002: PostgreSQL with Flyway migrations

## Status
Accepted

## Date
2026-07-09

## Context
BrewDeck stores relational data (coffees, methods, recipes, sessions, users) with clear foreign-key relationships. The schema evolves feature by feature and must be reproducible across local, CI, and future cloud environments.

## Decision
Use **PostgreSQL 16** as the primary store and **Flyway** for versioned, forward-only migrations under `brewdeck-api/src/main/resources/db/migration/`. Local PostgreSQL runs via Docker Compose.

## Consequences
- **Positive:** deterministic schema across environments; migration history is code-reviewed.
- **Positive:** strong relational integrity and mature ecosystem.
- **Negative:** every schema change requires a new migration file (no ad-hoc DDL).
- **Negative:** forward-only means rollbacks are handled by compensating migrations.

## Alternatives Considered
- **Hibernate `ddl-auto`** — rejected: unsafe for real schema evolution and history.
- **Liquibase** — viable, but Flyway's plain-SQL migrations are simpler for this project.
- **MySQL / SQLite** — rejected: PostgreSQL's feature set (partial indexes, types) fits better.

## Notes
Partial unique index on `recipes.share_token` (V4) is an example of a PostgreSQL-specific feature relied upon.
