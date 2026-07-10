# Database Design

PostgreSQL 16, schema versioned by Flyway (`brewdeck-api/src/main/resources/db/migration/`).

## Migrations

| Version | Purpose |
| ------- | ------- |
| V1 | Initial schema: `coffees`, `brew_methods`, `recipes`, `brew_sessions` |
| V2 | Seed initial brew methods |
| V3 | Replace free-text coffee tasting fields with numeric `*_score` columns |
| V4 | Add nullable `share_token` on `recipes` + partial unique index |
| V5 | Create `users` table |
| V6 | Add nullable `owner_id` FK (+ index) on `coffees`, `recipes`, `brew_sessions`; backfill to earliest user |

## Entity relationships

```mermaid
erDiagram
  COFFEES ||--o{ RECIPES : "brewed as"
  BREW_METHODS ||--o{ RECIPES : "uses"
  RECIPES ||--o{ BREW_SESSIONS : "logged as"
  USERS ||--o{ COFFEES : "owns"
  USERS ||--o{ RECIPES : "owns"
  USERS ||--o{ BREW_SESSIONS : "owns"
  USERS {
    bigint id PK
    varchar email UK
    varchar password_hash
    timestamp created_at
  }
```

> Note: Slice B.1 added nullable `owner_id` FKs on `coffees`, `recipes`, and `brew_sessions`, stamped at create time. Reads are not yet scoped by owner and `owner_id` stays nullable until Slice B.2 adds per-user filtering and enforces `NOT NULL`.

## Tables

### coffees
Coffee profiles. Key columns: `name` (required), `brand`, `origin`, `region`, `farm`, `producer`, `variety`, `process`, `roast_level`, `notes_primary`, `notes_secondary`, numeric tasting scores `acidity_score` / `body_score` / `sweetness_score` / `bitterness_score`, `description`, `owner_id → users(id)` (nullable, stamped on create), `created_at`, `updated_at`.

### brew_methods
Brewing methods. `name` (required, unique), `description`, `created_at`. Seeded in V2.

### recipes
Brewing recipes. FKs: `coffee_id → coffees(id)`, `method_id → brew_methods(id)` (both required). Params: `coffee_grams`, `water_grams`, `ratio`, `grind_setting`, `water_temp`, `brew_time`, `steps`, `expected_taste`, `favorite` (default false), `share_token` (nullable, unique when set), `owner_id → users(id)` (nullable, stamped on create), timestamps.

### brew_sessions
Brew logs. FKs: `recipe_id → recipes(id)` (required), `owner_id → users(id)` (nullable, stamped on create). `brewed_at`, actuals (`actual_grind`, `actual_temp`, `actual_time`), `taste_result`, `rating`, `adjustment_notes`.

### users
Accounts. `id`, `email` (unique), `password_hash` (BCrypt), `created_at`.

## Conventions

- Surrogate `BIGINT` identity primary keys.
- `created_at` defaults to `CURRENT_TIMESTAMP`; `updated_at` set on modification where present.
- Referential integrity enforced by FKs; partial unique index guards non-null share tokens.

> `TODO`: add indexes for high-traffic filter/sort columns once query volume justifies it.
