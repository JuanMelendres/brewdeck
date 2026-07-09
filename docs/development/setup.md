# Local Setup

## Requirements

- Java 21
- Docker + Docker Compose (for PostgreSQL and Testcontainers)
- Node.js (for `brewdeck-web`; see its `package.json` engines) and npm
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
npm install
npm run dev
```

- Web app: `http://localhost:3000`

## 5. Optional: AI features

AI recipe suggestions are **off by default**. To enable locally, set `AI_ENABLED=true` and provide `ANTHROPIC_API_KEY` (see [environment-variables.md](environment-variables.md)). Leave off for normal development and CI.

## Common commands

See [testing/test-plan.md](../testing/test-plan.md) for test/lint/build commands, and [contribution-guide.md](contribution-guide.md) for the workflow.
