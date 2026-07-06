# BrewDeck Project State

## Last Updated

2026-07-05

## Current Phase

Phase 5 (product improvements / analytics) in progress. Phases 1-4 complete. First analytics slice shipped end-to-end: top-rated recipes endpoint (GET /api/recipes/top-rated, PR #38) and the dashboard "Top Rated Recipes" widget (PR #39). Next: most-brewed recipes endpoint.

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
- Top-rated recipes analytics endpoint (GET /api/recipes/top-rated, PR #38)
- Dashboard "Top Rated Recipes" widget (PR #39)

## Recently Worked On

- Top-rated recipes analytics slice: backend ranking endpoint + dashboard widget (PRs #38, #39)
- Dedicated Favorite recipes screen (/recipes/favorites) + Favorites nav entry
- Read-only Brew methods list page (/brew-methods) + nav entry

## Known Rules

- Collection GET endpoints return PageResponse.
- GET by ID returns DTO.
- Integration tests must use explicit page/size/sort.
- Avoid assuming only one record exists in shared integration test DB.
- Avoid special characters in validation messages because responses are sanitized.

## Immediate Next Steps

1. Most-brewed recipes endpoint (GET /api/recipes/most-brewed?limit=5) — recipes ranked by brew-session count; mirrors the top-rated pattern (projection + grouped aggregate + clamp 1-20). Then a matching dashboard "Most Brewed" widget.
2. Further Phase 5 analytics: favorite coffee/recipe stats, method-usage breakdown, rating-trend-over-time chart (needs a charting lib).
3. Review JaCoCo and SonarCloud.
