# BrewDeck Project State

## Last Updated

2026-07-05

## Current Phase

Phase 4 (Next.js frontend) — well underway. Backend (Phases 1–3) complete and
on `develop`. Frontend read-only app + first two mutation slices built; more CRUD
slices in progress.

## Completed — Backend (on develop)

- Java 21 Spring Boot backend; PostgreSQL + Docker; Flyway; brew-method seed
- Full CRUD endpoints; RESTful status codes; GlobalExceptionHandler + ErrorResponse
- Request validation; Specification filters; PageResponse pagination; recipe favorites
- 400 handlers for malformed JSON / type mismatch / invalid sort property
- N+1 fix (@EntityGraph) + open-in-view=false + @Transactional boundaries
- CORS (env `CORS_ALLOWED_ORIGINS`), actuator `/health`, page-size cap, dashboard
  summary endpoint, structured service logs, OpenAPI/Swagger docs
- Tests (service/controller/repo/spec/integration Testcontainers), JaCoCo 80%,
  Spotless, PMD, OWASP, SonarCloud, GitHub Actions

## Completed — Frontend (`brewdeck-web/`)

- Scaffold: Next.js 16 App Router, TypeScript strict, MUI 9, TanStack Query,
  Vitest + React Testing Library; typed API client (`apiFetch`/`ApiError`)
- Dashboard slice (`/dashboard`) → `GET /api/dashboard/summary`
- Read-only list slices (paginated MUI Table + filters, `keepPreviousData`):
  Coffees, Recipes, Brew Sessions — all four nav items enabled
- **Coffee CRUD** (first mutation slice): RHF + Zod, mutation hooks invalidating
  the list, `CoffeeFormDialog`/`DeleteCoffeeDialog`, server-400 → field errors
- **Recipe CRUD**: adds FK dropdowns (coffee/method options hooks), z.coerce
  numeric fields, `RecipeFormDialog`/`DeleteRecipeDialog`

All merged to `develop` up through the read-only slices and coffee CRUD (the user
merges PRs on GitHub between sessions). Recipe CRUD is pushed as an open PR.

## Branch / PR state

- `develop`: backend + full read-only frontend + coffee CRUD.
- `feature/recipe-crud`: pushed, PR into `develop` open (recipe CRUD).
- Slices are built subagent-driven (TDD, per-task spec+quality review, final
  whole-branch review), then PR'd into `develop`. `gh` is NOT installed — PRs are
  opened via the compare URL; PR bodies staged in the scratchpad.

## Frontend patterns / conventions (learned)

- Slice = typed API module → `useX` query/mutation hooks → components → view wiring;
  mutations `invalidateQueries({ queryKey: ['<resource>'] })`.
- CRUD via MUI modal dialogs (one Form dialog for create+edit, one Delete confirm),
  rendered only-when-open so their hooks don't run closed. Server 400
  `validationErrors` map onto RHF fields via `setError`.
- FK dropdowns: lightweight options hooks fetch first 100, map to `{id,name}`;
  MUI 9 native select is `slotProps={{ select: { native: true } }}` (NOT the v5/v6
  `SelectProps`). Test selects via `getByRole('combobox')`.
- Numeric form fields use `z.coerce.number` with an `optionalNumber` preprocess
  (blank → undefined); form typed `useForm<z.input, unknown, z.output>`.
- Deps installed with `--legacy-peer-deps` (React 19 peers). `@testing-library/dom`
  must be an explicit devDep (RHF/zod install pruned it once).
- Watch the gap: schema/type-only changes pass vitest but can break `next build`
  (tsc) — run a build check, not just the test suite.
- Backend base URL via `NEXT_PUBLIC_API_BASE_URL` (default http://localhost:8080);
  Next dev on :3000 matches the backend CORS default.

## Known backend rules

- Collection GET → PageResponse; GET by id → DTO; page size capped at 100.
- Integration tests: explicit page/size/sort; don't assume single-record DB.
- Validation messages avoid special symbols (use "degrees Celsius", not the degree symbol).

## Immediate next steps

1. When the account session/rate limit resets (~1:50pm Mexico): run the deferred
   final whole-branch review for `feature/recipe-crud`.
2. Build the **Brew Session CRUD** slice (mirror recipe CRUD; one FK dropdown —
   recipe; rating numeric). Spec/plan can be written now (planning is not
   rate-limited); reuse recipe-CRUD plan with MUI 9 `slotProps` + generic-schema
   fixes baked in.
3. Later: detail pages, optimistic updates / success toasts, typeahead dropdowns,
   web CI (GitHub Actions for lint/test/build), auth.
