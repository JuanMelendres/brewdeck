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
| `BREWDECK_MAIL_ENABLED` | `false` | `true` sends real emails over SMTP (`SPRING_MAIL_*` below); `false` only logs the links |
| `BREWDECK_MAIL_FRONTEND_BASE_URL` | `http://localhost:3000` | Base URL for links in emails |
| `BREWDECK_MAIL_FROM` | `BrewDeck <no-reply@brewdeck.local>` | Sender address (must be allowed by your provider) |
| `SPRING_MAIL_HOST` | *(unset)* | SMTP host; **required** when `BREWDECK_MAIL_ENABLED=true` (startup fails without it) |
| `SPRING_MAIL_PORT` | `25` | SMTP port (usually `587` with STARTTLS, `1025` for local Mailpit) |
| `SPRING_MAIL_USERNAME` / `SPRING_MAIL_PASSWORD` | *(unset)* | SMTP credentials (secret: set in the environment, never commit) |
| `SPRING_MAIL_SMTP_AUTH` | `false` | `true` for any real provider |
| `SPRING_MAIL_SMTP_STARTTLS` | `false` | `true` for port 587 providers |
| `BREWDECK_ADMIN_EMAIL` | *(blank)* | Existing account promoted to `ADMIN` at startup ([ADR-009](../decisions/ADR-009-role-based-authorization.md)) |
| `RATE_LIMIT_ENABLED` | `true` | Auth endpoint rate limiting ([ADR-011](../decisions/ADR-011-in-memory-auth-rate-limiting.md)); keep on outside automated tests |
| `API_DOCS_ENABLED` | `false` in `prod` only | Serve `/v3/api-docs` and Swagger UI in production; enable only temporarily |

Database connection (from the README / Spring config; Docker Compose provides matching defaults):

| Variable | Default | Purpose |
| -------- | ------- | ------- |
| `DB_URL` | `jdbc:postgresql://localhost:5432/brewdeck` | JDBC URL |
| `DB_USER` | `brewdeck` | Database user |
| `DB_PASSWORD` | `brewdeck` | Database password |

### Email providers

Any SMTP provider works; nothing is provider-specific in code. Examples:

| Provider | Host | Port | Auth / STARTTLS | Username / password |
|---|---|---|---|---|
| **Local (Mailpit, `docker compose up mailpit`)** | `localhost` | `1025` | `false` / `false` | none; open http://localhost:8025 to read mail |
| Gmail | `smtp.gmail.com` | `587` | `true` / `true` | your address / a Google **app password** (needs 2FA) |
| Resend | `smtp.resend.com` | `587` | `true` / `true` | `resend` / your API key (needs a verified domain for `BREWDECK_MAIL_FROM`) |

Emails are sent after the database transaction commits, on a background thread. A slow or failing SMTP server never fails or delays the request; failures are logged as `Failed to send ... email`.

## Docker Compose (`docker-compose.yml`)

| Variable | Default | Purpose |
| -------- | ------- | ------- |
| `POSTGRES_DB` | `brewdeck` | Database name |
| `POSTGRES_USER` | `brewdeck` | Database user |
| `POSTGRES_PASSWORD` | `brewdeck` | Database password |

The `mailpit` service (SMTP on `1025`, web UI on `8025`) catches local emails.

## Frontend (`brewdeck-web/.env.example`)

| Variable | Default | Purpose |
| -------- | ------- | ------- |
| `NEXT_PUBLIC_API_BASE_URL` | `http://localhost:8080` | Base URL of the REST API |

## Rules

- Keep `.env.example` files in sync with the code; never commit real secrets.
- The AI feature must stay off (`AI_ENABLED=false`, blank key) in CI and by default.

> `Assumption`: `DB_URL`/`DB_USER`/`DB_PASSWORD` are documented in the README but not present in the root `.env.example`; confirm the exact Spring property bindings if you change defaults.
