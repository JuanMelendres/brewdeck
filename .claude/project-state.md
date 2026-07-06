# BrewDeck Project State

## Last Updated

2026-07-05

## Current Phase

Phase 4 (Next.js frontend) in progress. Backend Phases 1-3 complete. Dashboard, Coffee CRUD (PR #32), Recipe CRUD, Recipe detail + stats + brew history, and Brew session create all done (PR #35). Next: brew methods list page.

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

## Recently Worked On

- Brew session history on recipe detail page (GET /api/brew-sessions/recipe/{id})
- Brew session create dialog (POST /api/brew-sessions; invalidates sessions, recipes, dashboard)
- Recipe detail page (/recipes/[id]) with brew statistics — PR #35

## Known Rules

- Collection GET endpoints return PageResponse.
- GET by ID returns DTO.
- Integration tests must use explicit page/size/sort.
- Avoid assuming only one record exists in shared integration test DB.
- Avoid special characters in validation messages because responses are sanitized.

## Immediate Next Steps

1. Merge PR #35 into develop.
2. Next task: Brew methods list page. Add a /brew-methods route + view (list/table) using the existing listBrewMethods API and PageResponse pagination; mirror the coffees/recipes list views. Add a "Brew Methods" nav entry in AppShell. CRUD optional/later; start read-only list.
3. Dedicated Favorite recipes screen (GET /api/recipes/favorites).
4. Review JaCoCo and SonarCloud.
