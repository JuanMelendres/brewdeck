# BrewDeck Project State

## Last Updated

2026-07-05

## Current Phase

Phase 5 (product improvements / analytics) in progress. Phases 1-4 complete. Three analytics slices shipped end-to-end (endpoint + dashboard widget each): top-rated recipes (PRs #38/#39), most-brewed recipes (PRs #40/#41), and brew-method usage (PRs #43/#44). Dashboard shows Top Rated, Most Brewed, and Method Usage widgets. Next: pick the next Phase 5 slice (favorite coffee stats, or rating-trend chart).

## Completed

- Java 21 Spring Boot backend
- PostgreSQL Docker setup
- Flyway migrations
- Initial brew method seed
- CRUD endpoints
- RESTful status codes
- Global exception handling
- Request validation
- Filters with Specification
- Standard PageResponse
- Pagination for collection GET endpoints
- Recipe favorites
- Service tests
- Controller tests
- Repository tests
- Specification tests
- Integration tests with Testcontainers
- JaCoCo
- Spotless
- SonarCloud
- GitHub Actions
- CORS config for Next.js
- Dashboard summary endpoint
- Structured service logs for write operations
- OpenAPI/Swagger documentation
- Next.js frontend scaffolded (Phase 4)
- Dashboard UI wired to GET /api/dashboard/summary
- Coffee CRUD UI (list/table/create/edit/delete dialogs, mutation hooks) — PR #32 merged
- Recipe CRUD UI (list/table/create/edit/delete dialogs, mutation hooks)
- Recipe stats endpoint (GET /api/recipes/{id}/stats)
- Recipe detail page consuming recipe + stats + brew history — PR #35
- Brew sessions list UI (view/table/filters) and create dialog (form + mutation)
- brew-methods and coffee/method/recipe options frontend modules
- Top-rated recipes endpoint + widget (PRs #38, #39)
- Most-brewed recipes endpoint (GET /api/recipes/most-brewed) + widget (PRs #40, #41)
- Brew-method usage endpoint (GET /api/brew-methods/usage) + widget (PRs #43, #44)

## Recently Worked On

- Brew-method usage analytics slice: endpoint + dashboard widget (PRs #43, #44)
- Most-brewed recipes analytics slice: endpoint + dashboard widget (PRs #40, #41)
- Top-rated recipes analytics slice: endpoint + dashboard widget (PRs #38, #39)

## Known Rules

- Collection GET endpoints return PageResponse.
- GET by ID returns DTO.
- Integration tests must use explicit page/size/sort.
- Avoid assuming only one record exists in shared integration test DB.
- Avoid special characters in validation messages because responses are sanitized.

## Immediate Next Steps

1. Favorite coffee stats — most-used coffees across recipes/sessions (read-only aggregate endpoint, mirrors the analytics pattern) + a dashboard widget.
2. Rating-trend-over-time chart (needs a charting lib, e.g. recharts) — larger slice.
3. Recommended grind adjustments; other Phase 5 features.
3. Review JaCoCo and SonarCloud.
