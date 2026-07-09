# BrewDeck Project State

## Last Updated

2026-07-09

## Current Phase

Phase 6 (auth & multi-user) in progress: Auth foundation Slice A shipped full-stack (branch feature/auth-foundation). Phase 5 (product improvements / analytics) complete; all roadmap slices shipped end-to-end. Phases 1-4 complete. Four analytics slices shipped end-to-end (endpoint + dashboard widget each): top-rated recipes (PRs #38/#39), most-brewed recipes (PRs #40/#41), brew-method usage (PRs #43/#44), most-used coffees (PRs #46/#47). Dashboard shows all four widgets. Recipe detail also has a rating-trend line chart (recharts, PR #50) and a recommended-grind hint (PR #54). CLAUDE.md was reworked full-stack (PR #52) and web scripts added: type-check, lint:fix (PR #53). Coffee tasting-notes radar shipped full-stack (PR #57 merged): numeric 1-5 scores replaced the free-text tasting fields (backend migration + DTO validation) and a recharts radar renders on the coffee detail page. AI recipe suggestions generate slice shipped full-stack (PR #58 merged): feature-toggled POST /api/recipes/suggest calls Claude (claude-haiku-4-5, structured outputs) behind a RecipeSuggestionPort, and a "Suggest with AI" button pre-fills the recipe form. AI recipe improve-from-history slice shipped full-stack: feature-toggled POST /api/recipes/{id}/improve tunes an existing recipe from its recent rated brew sessions (extends RecipeSuggestionPort with improve, 422 when no rated history), and an "Improve with AI" button on the recipe detail page pre-fills the edit dialog. Recipe PDF export shipped (frontend-only): a client-side "Export PDF" button on the recipe detail page downloads a branded one-page recipe card built with jspdf from the loaded recipe. Public share links shipped full-stack (PR #62 merged): opt-in revocable base64url token on recipes, PATCH share/unshare, curated public GET /api/public/recipes/{token}, ShareRecipeDialog + standalone /share/[token] page. Phase 5 fully complete. Auth foundation (Slice A) then shipped: stateless JWT gate on all /api/** (public share + auth endpoints open), self-registration + login, frontend login/register screens + route guard. Next: Slice B (per-user ownership).

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
- Public share links full-stack (PR #62) — completes Phase 5
- Docs-as-code structure under docs/ (product/architecture/decisions/api/testing/development) — PR #66

## Recently Worked On

- Released develop → master (PR #67 merged, merge commit). develop had diverged from master (master held 4 older commits — dashboard summary, structured logs, OpenAPI enrich, CI optimization — already superseded in develop). Resolved by back-merging master into develop with `-X ours` (favor develop); merged tree was byte-identical to develop, so no code change. develop kept as the default/working branch.
- Docs-as-code reorg (PR #66 merged) — new docs/ tree: product/ (vision, roadmap, features, 2 FDDs), architecture/ (overview, technical-design, database-design, api-design, diagrams), decisions/ (6 ADRs), api/ (README + seed openapi.yaml; Postman stays at docs/postman/), testing/ (strategy/plan/unit/integration), development/ (setup/env/coding-standards/contribution). Existing .claude/ and docs/superpowers/ left as source of truth and linked, not moved. README refreshed (frontend shipped, MUI stack, correct roadmap, docs links), new CHANGELOG.md, docs-as-code section added to CLAUDE.md. Also merged earlier same day: write-fdd/write-tdd/write-spike skills (PR #64, four-backtick template fences) and review-skills-delivery skill (PR #65, frontmatter fix).
- Auth foundation (Slice A, full-stack) — backend: users table (Flyway V5), stateless JWT (jjwt) filter chain gating all /api/** except /api/public/** and /api/auth/{register,login}; BCrypt passwords; POST /api/auth/register (201), POST /api/auth/login (200), GET /api/auth/me (401 without token); RestAuthenticationEntryPoint returns 401 JSON; existing MockMvc suite migrated with @WithMockUser. Frontend: tokenStore (localStorage), auth api, apiFetch bearer injection + 401 redirect, AuthProvider (Context DI), /login + /register screens, RequireAuth on the (app) route-group layout; /login, /register, /share/[token] stay public. Ownership deferred to Slice B.
- Public share links (full-stack) — backend: nullable shareToken column on recipes, PATCH /api/recipes/{id}/share (generates token), PATCH /api/recipes/{id}/unshare (clears token), public GET /api/public/recipes/{token} (curated PublicRecipeResponse without id/favorite/timestamps); frontend: ShareRecipeDialog in recipe detail, Share button, standalone public /share/[token] page
- Recipe PDF export (frontend-only) — client-side "Export PDF" button on the recipe detail page; pure buildRecipePdf(recipe) → jsPDF recipe card (name, favorite, coffee, method, params, steps, expected taste), downloadRecipePdf saves a slugified filename; failures surface as an MUI Alert
- AI recipe improve-from-history slice — extends RecipeSuggestionPort with improve(ImprovementContext); POST /api/recipes/{id}/improve loads the recipe + its top-10 rated sessions, returns AI-tuned brewing params (SuggestedRecipeResponse); 404 recipe-missing, 422 no-rated-history, 503 AI-off/SDK-fail; frontend "Improve with AI" button (disabled with tooltip until a rated brew exists) opens the existing RecipeFormDialog pre-filled + rationale
- AI recipe suggestions (generate slice) — PR #58 merged: POST /api/recipes/suggest, Claude Java SDK behind a RecipeSuggestionPort with a claude-haiku-4-5 adapter (structured outputs), feature-toggled (brewdeck.ai.enabled, default off; ANTHROPIC_API_KEY from env), and a "Suggest with AI" button pre-filling the recipe form (ephemeral, not persisted); adapter Sonar-excluded, live SDK path untested by design (feature off by default)
- Coffee tasting-notes radar (full-stack) — PR #57 merged: numeric 1-5 acidity/body/sweetness/bitterness scores replaced free-text tasting fields; Flyway V3 migration, @Min/@Max DTO validation, MUI score sliders, recharts radar on coffee detail; also added sonar.coverage.exclusions to mirror JaCoCo DTO excludes
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

1. Slice B — per-user ownership: owner FK on coffees/recipes/sessions, per-user filtering + data migration.
2. Sweep deferred Minors catalogued in .superpowers/sdd/progress.md (test-coverage nits, onRetry wrappers, orphaned globals.css).
3. Review JaCoCo and SonarCloud.
