# Environment Variables

Configuration is via environment variables with safe local defaults. **No secrets are committed** — commit `.env.example`, never `.env`.

## Backend (`.env.example` at repo root)

| Variable | Default | Purpose |
| -------- | ------- | ------- |
| `SPRING_PROFILES_ACTIVE` | `local` | Active Spring profile |
| `SERVER_PORT` | `8080` | HTTP port |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:3000` | Allowed CORS origins for `/api/**` |
| `AI_ENABLED` | `false` | Toggles AI recipe suggestions/improve |
| `ANTHROPIC_API_KEY` | *(blank)* | Anthropic key; required only when `AI_ENABLED=true` |

Database connection (from the README / Spring config; Docker Compose provides matching defaults):

| Variable | Default | Purpose |
| -------- | ------- | ------- |
| `DB_URL` | `jdbc:postgresql://localhost:5432/brewdeck` | JDBC URL |
| `DB_USER` | `brewdeck` | Database user |
| `DB_PASSWORD` | `brewdeck` | Database password |

## Docker Compose (`docker-compose.yml`)

| Variable | Default | Purpose |
| -------- | ------- | ------- |
| `POSTGRES_DB` | `brewdeck` | Database name |
| `POSTGRES_USER` | `brewdeck` | Database user |
| `POSTGRES_PASSWORD` | `brewdeck` | Database password |

## Frontend (`brewdeck-web/.env.example`)

| Variable | Default | Purpose |
| -------- | ------- | ------- |
| `NEXT_PUBLIC_API_BASE_URL` | `http://localhost:8080` | Base URL of the REST API |

## Rules

- Keep `.env.example` files in sync with the code; never commit real secrets.
- The AI feature must stay off (`AI_ENABLED=false`, blank key) in CI and by default.

> `Assumption`: `DB_URL`/`DB_USER`/`DB_PASSWORD` are documented in the README but not present in the root `.env.example`; confirm the exact Spring property bindings if you change defaults.
