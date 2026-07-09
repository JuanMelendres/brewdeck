# Changelog

All notable changes to BrewDeck are documented here. Format loosely follows
[Keep a Changelog](https://keepachangelog.com/); the project is not yet versioned,
so entries are grouped by roadmap phase. See [`docs/product/roadmap.md`](docs/product/roadmap.md)
for status and [`.claude/project-state.md`](.claude/project-state.md) for detail.

## [Unreleased]

### Added
- Docs-as-code structure under [`docs/`](docs/README.md): product, architecture, decisions, API, testing, development, plus ADRs and a seed `openapi.yaml`.

## Phase 6 — Auth & multi-user (in progress)

### Added
- Auth foundation (Slice A): users table (Flyway V5), stateless JWT filter gating all `/api/**` except `/api/public/**` and `/api/auth/{register,login}`, BCrypt hashing, `POST /api/auth/register`, `POST /api/auth/login`, `GET /api/auth/me`; frontend login/register screens + route guard.

### Pending
- Slice B — per-user ownership. Slice C — account UX (email verification, password reset, refresh tokens).

## Phase 5 — Product improvements

### Added
- Analytics: top-rated recipes, most-brewed recipes, brew-method usage, most-used coffees (endpoints + dashboard widgets).
- Rating-trend chart and recommended-grind hint on recipe detail.
- Coffee tasting-notes radar; numeric 1–5 tasting scores replace free-text fields (Flyway V3).
- AI recipe suggestions (`POST /api/recipes/suggest`) and improve-from-history (`POST /api/recipes/{id}/improve`), feature-flagged behind a hexagonal port.
- Recipe PDF export (client-side).
- Public share links: opt-in revocable token (Flyway V4), `PATCH share/unshare`, public `GET /api/public/recipes/{token}`.

## Phase 4 — Frontend

### Added
- Next.js + React + TypeScript + MUI + TanStack Query + React Hook Form + Zod web app: dashboard, coffee/recipe/brew-session/brew-method CRUD, favorites, filters, recipe detail with stats.

## Phases 1–3 — Backend foundation, quality, and UX

### Added
- Spring Boot 3 API on Java 21, PostgreSQL 16 + Docker Compose, Flyway migrations, seed brew methods.
- Full CRUD for coffees, brew methods, recipes, brew sessions; DTOs, Bean Validation, `GlobalExceptionHandler`, RESTful status codes.
- Specification-based filters, `PageResponse<T>` pagination, favorites, dashboard summary, CORS, health probe, OpenAPI/Swagger, structured logs.
- Test suites (service, controller, repository, specification, integration via Testcontainers), JaCoCo, Spotless, SonarCloud, OWASP Dependency Check, GitHub Actions.

> This changelog was seeded from the roadmap and project state on 2026-07-09. `TODO`: adopt semantic version tags and per-release sections going forward.
