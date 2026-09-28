# Environment Variables

Configuration is via environment variables with safe local defaults. **No secrets are committed** — commit `.env.example`, never `.env`.

## Backend

General settings live in the root `.env.example`; auth, mail, and hardening settings in `brewdeck-api/.env.example`. Defaults come from `application.yaml`, and `application-prod.yml` overrides where noted.

| Variable | Default | Purpose |
| -------- | ------- | ------- |
| `SPRING_PROFILES_ACTIVE` | `local` | Active Spring profile |
| `SERVER_PORT` | `8080` | HTTP port |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:3000` | Comma-separated origins allowed to call `/api/**` from a browser. Only `Authorization` and `Content-Type` request headers are accepted; no credentials/cookies |
| `AI_ENABLED` | `false` | Toggles AI recipe suggestions/improve |
| `ANTHROPIC_API_KEY` | *(blank)* | Anthropic key; required only when `AI_ENABLED=true` |
| `BREWDECK_JWT_SECRET` | dev-only placeholder; **required in `prod`** | HMAC key for access tokens (≥ 32 bytes) |
| `AUTH_TOKEN_TTL` | `PT15M` | Access-token lifetime (ISO-8601) |
| `AUTH_REFRESH_TTL` | `P7D` | Refresh-token lifetime (ISO-8601) |
| `BREWDECK_MAIL_ENABLED` | `false` | Real mail delivery (the SMTP adapter is not implemented yet) |
| `BREWDECK_MAIL_FRONTEND_BASE_URL` | `http://localhost:3000` | Base URL for links in emails |
| `BREWDECK_ADMIN_EMAIL` | *(blank)* | Existing account promoted to `ADMIN` at startup ([ADR-009](../decisions/ADR-009-role-based-authorization.md)) |
| `RATE_LIMIT_ENABLED` | `true` | Auth endpoint rate limiting ([ADR-011](../decisions/ADR-011-in-memory-auth-rate-limiting.md)); keep on outside automated tests |
| `API_DOCS_ENABLED` | `false` in `prod` only | Serve `/v3/api-docs` and Swagger UI in production; enable only temporarily |

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
