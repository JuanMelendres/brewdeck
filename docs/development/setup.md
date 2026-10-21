# Local Setup

## Requirements

- Java 21
- Docker + Docker Compose (for PostgreSQL and Testcontainers)
- Node.js 22+ (for `brewdeck-web`; see its `package.json` engines) and pnpm (`corepack enable`)
- Maven Wrapper is bundled (`./mvnw`) — no separate Maven install needed

## 1. Clone

```bash
git clone https://github.com/JuanMelendres/brewdeck.git
cd brewdeck
```

## 2. Start PostgreSQL

```bash
docker compose up -d
```

Postgres 16 listens on `localhost:5432` (db/user/password default to `brewdeck`).

## 3. Run the backend

```bash
cd brewdeck-api
./mvnw spring-boot:run
```

- API: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- Health: `http://localhost:8080/healthz` (public). The actuator itself, with Prometheus metrics, is on the management port: `http://localhost:8081/actuator/prometheus` ([ADR-023](../decisions/ADR-023-application-metrics.md)).

**Optional: local metrics dashboards.** With the API running, start Prometheus and Grafana:

```bash
docker compose --profile metrics up -d
```

- Prometheus at `http://localhost:9090` scrapes `host.docker.internal:8081` every 15 s. Under
  **Status → Targets**, `brewdeck-api` should be `UP`.
- Grafana at `http://localhost:3001` (no login) has Prometheus as its data source. Try
  `rate(http_server_requests_seconds_count[1m])`, or import the community "JVM (Micrometer)"
  dashboard.
- Stop them with `docker compose --profile metrics down`.

## 4. Run the frontend

```bash
cd brewdeck-web
cp .env.example .env.local   # sets NEXT_PUBLIC_API_BASE_URL
pnpm install
pnpm dev
```

- Web app: `http://localhost:3000`

## 5. Optional: AI features

The `local` profile runs "Suggest with AI" and "Improve with AI" on a **free local model** through Ollama ([ADR-016](../decisions/ADR-016-local-llm-ollama.md)). Other environments keep AI off.

1. Install Ollama natively (on macOS, Docker cannot use the Apple GPU): `brew install ollama`.
2. Download the model once (~5 GB): `ollama pull qwen3:8b`.
3. Start it while you work: `ollama serve` (listens on `http://localhost:11434`).

Each suggestion takes about 15–25 s on an Apple M4. Without Ollama running, the AI buttons answer `503`. To turn AI off locally, set `AI_ENABLED=false`. To use the paid Claude API instead, set `AI_PROVIDER=claude` and `ANTHROPIC_API_KEY` (see [environment-variables.md](environment-variables.md)). CI never calls a model: the tests stub it.

## 6. Optional: run the API image

Hosted environments run the API from `brewdeck-api/Dockerfile` with the `staging` profile ([ADR-017](../decisions/ADR-017-staging-hosting.md)). To try the image locally against the Compose database and Mailpit (`docker compose up -d postgres mailpit`):

```bash
docker build -t brewdeck-api brewdeck-api
docker run --rm -p 8080:8080 -e SPRING_PROFILES_ACTIVE=staging \
  -e DB_URL=jdbc:postgresql://host.docker.internal:5432/brewdeck -e DB_USER=brewdeck -e DB_PASSWORD=brewdeck \
  -e BREWDECK_JWT_SECRET="$(openssl rand -base64 48)" \
  -e BREWDECK_MAIL_ENABLED=true -e SPRING_MAIL_HOST=host.docker.internal -e SPRING_MAIL_PORT=1025 brewdeck-api
```

To deploy staging, follow [staging-deployment.md](staging-deployment.md). The `staging` profile has no dev defaults: startup fails if `DB_URL`, `DB_USER`, `DB_PASSWORD` or `BREWDECK_JWT_SECRET` is missing, or if mail is off (`BREWDECK_MAIL_ENABLED=false` would log live reset and verification links). It uses the `staging` feature-flag rows, so AI stays off.

## Common commands

See [testing/test-plan.md](../testing/test-plan.md) for test/lint/build commands, and [contribution-guide.md](contribution-guide.md) for the workflow.
