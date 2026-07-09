# ☕ BrewDeck

BrewDeck is a personal coffee companion that helps enthusiasts track, optimize, and reproduce their brewing recipes — so a great cup is repeatable, not lucky.

Instead of notes scattered across apps and memory, BrewDeck centralizes coffees, recipes, and brew sessions into one structured, data-driven system. The long-term vision includes a physical e-paper companion device that syncs with the platform.

[![Quality gate](https://sonarcloud.io/api/project_badges/quality_gate?project=JuanMelendres_brewdeck)](https://sonarcloud.io/summary/new_code?id=JuanMelendres_brewdeck)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=JuanMelendres_brewdeck&metric=coverage)](https://sonarcloud.io/summary/new_code?id=JuanMelendres_brewdeck)

---

## 🚀 Features

- ☕ Coffee, brew-method, recipe, and brew-session management (full CRUD)
- ⭐ Favorites, filters, and pagination
- 📊 Analytics — top-rated/most-brewed recipes, most-used coffees, method usage, rating trend, tasting radar
- 🤖 AI-assisted recipe suggestions & improvement (feature-flagged)
- 🔗 Opt-in public share links for recipes
- 🔐 Auth foundation — self-registration + JWT login
- 🧱 Clean REST API + mobile-first web client

## 🛠 Tech Stack

**Backend:** Java 21 · Spring Boot 3.5 · Spring Data JPA · PostgreSQL 16 · Flyway · Maven
**Frontend:** Next.js (App Router) · React 19 · TypeScript · MUI · TanStack Query · React Hook Form + Zod
**Quality/DevOps:** JUnit 5 · Mockito · Testcontainers · JaCoCo · Spotless · PMD · OWASP Dependency Check · SonarCloud · Qodana · GitHub Actions · Docker Compose

## 🏗 Architecture

```text
Next.js Web App  ──REST/JSON──▶  Spring Boot API  ──▶  PostgreSQL 16
                                       │
                                       └─ (feature-flagged) Anthropic Claude
```

Package-by-domain modular monolith. Full detail in [`docs/architecture/`](docs/architecture/overview.md).

## ⚙️ Quick Start

```bash
git clone https://github.com/JuanMelendres/brewdeck.git
cd brewdeck

# 1. Database
docker compose up -d

# 2. Backend  →  http://localhost:8080  (Swagger at /swagger-ui/index.html)
cd brewdeck-api && ./mvnw spring-boot:run

# 3. Frontend →  http://localhost:3000
cd ../brewdeck-web && cp .env.example .env.local && npm install && npm run dev
```

Full instructions: [`docs/development/setup.md`](docs/development/setup.md).

## 🧰 Common Commands

```bash
# backend
./mvnw spotless:apply && ./mvnw clean verify   # format + build + tests
./mvnw pmd:check                               # static analysis
./mvnw dependency-check:check                  # security scan

# frontend (in brewdeck-web/)
npm run test && npm run type-check && npm run build
```

## 📚 Documentation

Docs-as-code lives in [`docs/`](docs/README.md):

- **Product** — [vision](docs/product/vision.md) · [roadmap](docs/product/roadmap.md) · [features](docs/product/features.md) · [FDDs](docs/product/fdd/)
- **Architecture** — [overview](docs/architecture/overview.md) · [technical design](docs/architecture/technical-design.md) · [database](docs/architecture/database-design.md) · [API design](docs/architecture/api-design.md) · [diagrams](docs/architecture/diagrams.md)
- **Decisions** — [ADRs](docs/decisions/)
- **API** — [reference](docs/api/README.md) · [openapi.yaml](docs/api/openapi.yaml) · [Postman](docs/api/postman/)
- **Testing** — [strategy](docs/testing/testing-strategy.md) · [plan](docs/testing/test-plan.md)
- **Development** — [setup](docs/development/setup.md) · [env vars](docs/development/environment-variables.md) · [coding standards](docs/development/coding-standards.md) · [contributing](docs/development/contribution-guide.md)

## 📊 Project Status

Phases 1–5 complete (backend, quality, backend-UX, frontend, product improvements). **Phase 6 (auth & multi-user) in progress** — auth foundation shipped; per-user ownership and account UX are next.

## 🗺 Roadmap (short)

1–5 ✅ Backend, quality, frontend, analytics, AI/sharing · **6 🔄 Auth & multi-user (in progress)** · Vision 🚀 e-paper device, offline sync, advanced analytics.

Detail: [`docs/product/roadmap.md`](docs/product/roadmap.md).

## 👨‍💻 Author

Juan Melendres — Backend Developer · Java & Spring Boot

## ⭐ Support

If you like this project, star ⭐ the repository and share ideas or improvements.
