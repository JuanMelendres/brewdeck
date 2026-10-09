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
- Health: `http://localhost:8080/actuator/health`

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

## Common commands

See [testing/test-plan.md](../testing/test-plan.md) for test/lint/build commands, and [contribution-guide.md](contribution-guide.md) for the workflow.
