# BrewDeck Project State

## Last Updated

2026-07-05

## Current Phase

Phase 4 (Next.js frontend) in progress. Backend Phases 1-3 complete. Dashboard, Coffee CRUD (PR #32), Recipe CRUD, and Recipe detail + stats (PR #35) done. Next: brew session create UI.

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
- Recipe detail page consuming recipe + stats — PR #35
- Brew sessions list UI (view/table/filters)
- brew-methods and coffee/method options frontend modules

## Recently Worked On

- Recipe detail page (/recipes/[id]) with brew statistics; recipe names link to detail — PR #35
- Backend recipe stats endpoint (totalSessions, averageRating, lastBrewedAt)
- Recipe CRUD UI (merged)

## Known Rules

- Collection GET endpoints return PageResponse.
- GET by ID returns DTO.
- Integration tests must use explicit page/size/sort.
- Avoid assuming only one record exists in shared integration test DB.
- Avoid special characters in validation messages because responses are sanitized.

## Immediate Next Steps

1. Merge PR #35 (recipe detail + stats) into develop.
2. Next task: Brew session create UI. Backend POST /api/brew-sessions is ready; mirror RecipeFormDialog (react-hook-form + zod). Add brewSessionSchema, create/mutation API + hook (invalidate sessions list and the recipe stats query), and an "Add Brew Session" dialog on the sessions list. Prefill recipe from list/detail where possible.
3. Show a recipe's brew-session history on the recipe detail page (GET /api/brew-sessions/recipe/{recipeId}).
4. Brew methods list page and a dedicated Favorite recipes screen.
5. Review JaCoCo and SonarCloud.
