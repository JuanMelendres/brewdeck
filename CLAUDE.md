# BrewDeck — Claude Project Context

## Role

Act as a senior full-stack software engineer with strong experience in:
- Java 21 / Spring Boot 3 / Spring Data JPA
- PostgreSQL / Flyway / Testcontainers
- REST API design (RESTful status codes, JSON validation, pagination)
- Next.js (App Router) / React / TypeScript
- Material UI (MUI)
- TanStack Query, React Hook Form, Zod
- CI/CD & Quality: GitHub Actions, SonarCloud, OWASP Dependency Check

Prioritize maintainability, strict type safety, correctness, behavioral UI testing, small incremental changes, and production-ready conventions.

## Project Summary

BrewDeck is a coffee brewing application.

- **Backend:** A Spring Boot REST API managing coffees, brew methods, recipes, and brew sessions.
- **Frontend:** A Next.js (App Router) app that consumes the REST API — dashboard, CRUD screens, and analytics.

Both backend and frontend are active. Prioritize full-stack consistency: keep endpoints and interfaces structured, predictable, and fully tested.

## Backend Stack & Architecture

### Stack
Java 21, Spring Boot 3, Maven Wrapper, PostgreSQL 16, Docker Compose, Flyway, Spring Data JPA, Hibernate, Bean Validation, Testcontainers, JUnit 5, Mockito, MockMvc, JaCoCo, Spotless, OWASP Dependency Check, SonarCloud, GitHub Actions, Springdoc OpenAPI / Swagger.

### Packages & Resources
- Main packages: `coffee`, `method`, `recipe`, `session`, `common`, `integration`
- API resources: `/api/coffees`, `/api/brew-methods`, `/api/recipes`, `/api/brew-sessions`, `/api/dashboard`

### Completed backend features
- Full CRUD for Coffee, BrewMethod, Recipe, BrewSession
- PostgreSQL + Docker Compose, Flyway migrations, initial brew-method seed
- `GlobalExceptionHandler` + `ErrorResponse`; RESTful status codes (POST 201, DELETE 204, GET/PUT/PATCH 200)
- Recipe favorites: `PATCH /api/recipes/{id}/favorite`, `PATCH /api/recipes/{id}/unfavorite`, `GET /api/recipes/favorites`
- Filters via Specification: `CoffeeFilter`, `RecipeFilter`, `BrewSessionFilter`
- Standard paginated `PageResponse`; request validation on all `*Request` records
- Dashboard summary endpoint; structured service logs; CORS for the frontend; OpenAPI/Swagger
- Analytics (read-only): `GET /api/recipes/{id}/stats`, `GET /api/recipes/top-rated`, `GET /api/recipes/most-brewed`, `GET /api/coffees/most-used`, `GET /api/brew-methods/usage`
- Tests: service, controller (MockMvc), repository, specification, integration (Testcontainers); JaCoCo coverage

## Response & Pagination Rules

All GET endpoints that return collections must return `PageResponse<T>`:
```json
{
  "content": [],
  "page": 0,
  "size": 10,
  "totalElements": 0,
  "totalPages": 0,
  "first": true,
  "last": true
}
```
GET-by-id returns the DTO directly. Bounded analytics rankings (top-rated / most-brewed / most-used / usage) return a plain `List<T>`, not `PageResponse` — they are top-N, not browsable collections.

Collection GET endpoints support `page`, `size`, `sort`. Prefer a safe default while the project evolves:
```java
@PageableDefault(size = 10, sort = "id", direction = Sort.Direction.ASC)
```
In integration tests, use explicit pagination params:
```java
.param("page", "0").param("size", "10").param("sort", "id,asc")
```

## Frontend Stack & Architecture

### Stack (in use)
Next.js (App Router) + React 19 + TypeScript, MUI, **TanStack Query** (server state), **React Hook Form + Zod** (forms/validation), a `fetch`-based API client, **Vitest + React Testing Library**. No axios; no Formik/Yup; no Redux.

### Directory structure (as built — match it)
- `src/app/` — App Router routes and `page.tsx`/`layout.tsx`. **Pages/layouts use `export default`** (required by Next.js).
- `src/components/<feature>/` — feature-organized components (`coffees/`, `recipes/`, `brew-sessions/`, `brew-methods/`, `dashboard/`, `ui/`, `layout/`).
- `src/hooks/` — custom hooks (`useRecipes.ts`, `useMethodUsage.ts`, ...).
- `src/lib/api/` — API client (`client.ts` → `apiFetch`) and per-resource modules (`recipes.ts`, `coffees.ts`, ...). **Domain types live here** (`src/lib/api/types.ts` and per-module exports like `MostUsedCoffee`).
- `src/lib/query/` — TanStack Query keys (`keys.ts`) and provider.
- `src/lib/validation/` — Zod schemas (`recipeSchema.ts`, `brewSessionSchema.ts`).

### Naming conventions (as built)
- **Components:** PascalCase files (`RecipeFormDialog.tsx`, `StatCard.tsx`).
- **Hooks:** camelCase, `use` prefix (`useRecipe.ts`, `useMostUsedCoffees.ts`) — NOT kebab-case.
- **Feature directories:** kebab-case for multi-word (`brew-sessions/`, `brew-methods/`).
- **Colocation:** keep `*.test.tsx` next to the component/hook it covers.

### React / TypeScript rules
- **Strict types:** never use `any`; use precise interfaces or `unknown` + guards. Import domain types from `src/lib/api` (not a `src/types/` folder — there isn't one).
- **Functional components + hooks only.** No class components.
- **Exports:** named exports for components/hooks (`export function X`); `export default` **only** for Next.js `page.tsx`/`layout.tsx`.
- **Imports:** absolute paths via the `@/` alias; group React → third-party → internal.
- **Server state:** use **TanStack Query** (`useQuery`/`useMutation`) for all API data — never store server data in global client state. Centralize query keys in `src/lib/query/keys.ts`; invalidate the right key prefixes in mutation `onSuccess`.
- **Client state:** `useState` for local, `useReducer` for complex local; React Context only for DI (theme/providers), not high-frequency updates.
- **Forms:** React Hook Form + `zodResolver`; Zod schema in `src/lib/validation/`. Map server-400 `validationErrors` back onto fields.
- **Performance:** memoize heavy work with `useMemo`/`useCallback`; avoid needless inline objects/handlers in hot render paths.
- **UX:** always handle loading, error, and empty states (reuse `Spinner`, `ErrorState`, `EmptyState`).

## Validation & Error Handling

- **Backend:** Bean Validation on request records. Avoid special symbols like `°C` in messages (responses are sanitized → `&deg;`); write `degrees Celsius`.
- **Frontend:** Zod schema mirrors backend limits; surface API failures as user-visible states, not `console.error`.

Validation errors conform to `GlobalExceptionHandler`:
```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/example",
  "validationErrors": { "field": "message" }
}
```

## Testing Standards

Every meaningful backend or frontend change includes or updates tests. Follow Arrange–Act–Assert.

### Backend
Service (business logic), controller (HTTP/validation/status via MockMvc), repository (custom queries), specification (filters), integration (workflows via Testcontainers). Don't assume the shared integration DB has one record — assert `greaterThanOrEqualTo` or fully control the dataset. For paginated bodies assert `$.content`, `$.content[0].id`, `$.page`, `$.size`, `$.totalElements` — never `$[0]`.

### Frontend
Vitest + React Testing Library. Test behavior, not implementation. Prefer accessible queries (`getByRole`, `getByText`, `getByLabelText`); avoid `getByTestId` unless necessary. Mock the API-client layer / hooks (not child components, generally); wrap the query hooks with a `QueryClientProvider` in hook tests. For components that render server data, mock the corresponding hook.

## Quality & Verification Commands

Run before considering a task done.

### Backend (Maven Wrapper)
- macOS/Linux: `./mvnw spotless:apply` then `./mvnw clean verify`
- Windows: `.\mvnw.cmd spotless:apply` then `.\mvnw.cmd clean verify`
- Focused: `./mvnw -Dtest=ClassName test` (or `.\mvnw.cmd -Dtest=ClassName test`)

### Frontend (npm, in `brewdeck-web/`)
- `npm run dev` — local dev server
- `npm run test` — Vitest suite (`vitest run`)
- `npm run type-check` — strict TypeScript (`tsc --noEmit`), no emit
- `npm run lint` — ESLint (whole project, read-only)
- `npm run lint:fix` — ESLint autofix. **Scope it to the files you changed** — never fix the whole repo: `npm run lint:fix -- src/path/to/file.tsx`
- `npm run build` — production build (also type-checks)

## Git Rules

Conventional Commits; provide exactly one message per commit. Scopes: `api` (backend), `web` (frontend), plus `docs`/`test`/`ci`/`chore`/`refactor`/`style`/`fix`/`perf`.
Examples:
- `feat(api): add most-used coffees analytics endpoint`
- `feat(web): add top-rated recipes widget to the dashboard`
- `fix(web): guard against a null average rating`
- `test(api): cover recipe stats not-found path`

## Postman

Collection: `docs/postman/brewdeck.postman_collection.json`, env: `docs/postman/brewdeck.local.postman_environment.json`.
Keep requests aligned with the API; collection GETs carry `page`/`size`/`sort`. Use Long ID vars (`{{coffeeId}}`, `{{methodId}}`, `{{recipeId}}`, `{{sessionId}}`), not `{{$guid}}`. Base URL from the environment (`http://localhost:8080`). No real credentials/tokens. Update the collection when endpoints change.

## Security & Quality

Never hardcode secrets — use environment variables; commit `.env.example`, not `.env`. Keep OWASP Dependency Check and SonarCloud passing unless working on a known false positive.

## Critical Anti-Patterns (what NOT to do)

1. **Don't leak entities.** Never return Hibernate/JPA entities from controllers — map to explicit DTOs/records.
2. **Don't use `any`.** Use precise types or `unknown` + guards.
3. **Don't store server data in global client state.** Use TanStack Query and invalidate the right keys.
4. **Don't ban default exports for Next.js pages.** `page.tsx`/`layout.tsx` require `export default`; use named exports everywhere else.
5. **Don't assert `$[0]` on paginated bodies.** Use `$.content[0]`.
6. **Don't put special symbols in validation messages** (`°C` → write `degrees Celsius`).
7. **Don't run `lint:fix` on the whole repo.** Scope it to changed files (`npm run lint:fix -- <path>`) so it doesn't silently rewrite unrelated files.
8. **Don't hardcode secrets.** Use env vars and `.env.example`.

## Important Instruction

Before making changes:
1. Inspect current files.
2. Identify the smallest safe change.
3. Explain what will change.
4. Apply code changes.
5. Update or add tests.
6. Run or suggest the exact verification command.
7. Provide a Conventional Commit message.
