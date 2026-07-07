# BrewDeck Roadmap

## Phase 1 — Backend Foundation

Status: Completed

- Spring Boot API
- PostgreSQL
- Docker Compose
- Flyway
- CRUD resources
- DTOs
- Validations
- Error handling
- RESTful status codes

## Phase 2 — Backend Quality

Status: Mostly Completed

- Unit tests
- Controller tests
- Repository tests
- Integration tests
- Testcontainers
- JaCoCo
- Spotless
- SonarCloud
- Dependency Check
- GitHub Actions

## Phase 3 — Backend UX for Frontend

Status: Completed

- PageResponse for lists
- Query filters
- Favorites
- CORS
- Dashboard summary
- Better Swagger/OpenAPI docs
- Basic logs

## Phase 4 — Frontend

Status: Completed

Stack (in use):

- Next.js
- React
- TypeScript
- Tailwind / shadcn/ui
- API client layer
- Forms with validation (zod schemas)
- TanStack Query mutation hooks
- Vitest

Screens:

- Dashboard — Done
- Coffees list/create/edit/delete — Done (PR #32)
- Recipes list/create/edit/delete — Done
- Recipes detail (+ brew stats) — Done (PR #35)
- Brew sessions list — Done
- Brew sessions create — Done
- Brew session history by recipe — Done
- Brew methods list — Done
- Favorite recipes (dedicated screen) — Done

## Phase 5 — Product Improvements

Status: Completed

- Top-rated recipes (endpoint + dashboard widget) — Done (PRs #38, #39)
- Most-brewed recipes (endpoint + widget) — Done (PRs #40, #41)
- Brew method usage breakdown (endpoint + widget) — Done (PRs #43, #44)
- Most-used coffees (endpoint + widget) — Done (PRs #46, #47)
- Rating trend over time (chart, recipe detail) — Done (PR #50)
- Recommended grind adjustments (recipe detail hint) — Done (PR #54)
- Coffee tasting notes visualization (radar chart on coffee detail) — Done (PR #57)
- AI-assisted recipe suggestions — Done: generate slice (PR #58) + improve-from-history slice (POST /api/recipes/{id}/improve, "Improve with AI" on recipe detail)
- Export recipes to PDF — Done: client-side "Export PDF" on recipe detail (jspdf recipe card)
- Public share links (opt-in revocable token, public /share/[token] page) — Done