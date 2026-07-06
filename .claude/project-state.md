# BrewDeck Project State

## Last Updated

2026-07-05

## Current Phase

Phase 5 (product improvements / analytics) in progress. Phases 1-4 complete. Four analytics slices shipped end-to-end (endpoint + dashboard widget each): top-rated recipes (PRs #38/#39), most-brewed recipes (PRs #40/#41), brew-method usage (PRs #43/#44), most-used coffees (PRs #46/#47). Dashboard shows all four widgets. Recipe detail also has a rating-trend line chart (recharts, PR #50) and a recommended-grind hint (PR #54). CLAUDE.md was reworked full-stack (PR #52) and web scripts added: type-check, lint:fix (PR #53). Next: tasting-notes visualization (needs a coffee detail page), or another Phase 5 feature.

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

- Recommended-grind hint on recipe detail (best-rated session's grind; no backend) — PR #54
- Rating-trend line chart on recipe detail (recharts; reuses brew-history data) — PR #50
- CLAUDE.md reworked full-stack (PR #52); web type-check + lint:fix scripts (PR #53)
- Most-used coffees analytics slice: endpoint + dashboard widget (PRs #46, #47)
- Brew-method usage analytics slice: endpoint + dashboard widget (PRs #43, #44)

## Known Rules

- Collection GET endpoints return PageResponse.
- GET by ID returns DTO.
- Integration tests must use explicit page/size/sort.
- Avoid assuming only one record exists in shared integration test DB.
- Avoid special characters in validation messages because responses are sanitized.

## Immediate Next Steps

1. Coffee tasting-notes visualization — a radar/spider chart of the coffee's acidity/body/sweetness/bitterness. Likely needs a coffee detail page first (`/coffees/[id]`), since only a coffees list exists today.
2. Remaining Phase 5 features: AI-assisted recipe suggestions, export recipes to PDF, public share links.
3. Review JaCoCo and SonarCloud.
