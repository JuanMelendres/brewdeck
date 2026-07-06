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

Status: In Progress

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
- Favorite recipes (dedicated screen) — Next

## Phase 5 — Product Improvements

Potential features:

- Brew session analytics
- Average rating by recipe
- Favorite coffee/recipe stats
- Recommended grind adjustments
- Coffee tasting notes visualization
- AI-assisted recipe suggestions
- Export recipes to PDF
- Public share links