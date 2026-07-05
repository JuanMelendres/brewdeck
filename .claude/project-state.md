# BrewDeck Project State

## Last Updated

2026-07-05

## Current Phase

Phase 4 (Next.js frontend) in progress. Backend Phases 1-3 complete. Coffee CRUD UI merged (PR #32); Recipe CRUD UI in progress on branch feature/recipe-crud.

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
- Coffee CRUD UI (list/table/create/edit/delete dialogs, mutation hooks) — PR #32 merged
- Recipe CRUD UI in progress (API funcs, mutation hooks, form schema, form/delete dialogs, table actions)
- brew-methods and coffee/method options frontend modules

## Recently Worked On

- Recipe CRUD UI on branch feature/recipe-crud: wired create/edit/delete dialogs into recipes list, actions column, form dialog, options hooks
- Coffee CRUD UI (merged)
- Frontend API client layer (recipes, brew-methods)

## Known Rules

- Collection GET endpoints return PageResponse.
- GET by ID returns DTO.
- Integration tests must use explicit page/size/sort.
- Avoid assuming only one record exists in shared integration test DB.
- Avoid special characters in validation messages because responses are sanitized.

## Immediate Next Steps

1. Finish Recipe CRUD UI on feature/recipe-crud; open PR.
2. ~~Backend: recipe stats endpoint~~ — Done (GET /api/recipes/{id}/stats: totalSessions, averageRating, lastBrewedAt).
3. Implement frontend recipe detail page consuming stats + dashboard summary.
4. Build brew sessions list/create UI.
5. Review JaCoCo and SonarCloud after backend stats change.
