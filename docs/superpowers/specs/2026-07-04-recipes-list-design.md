# BrewDeck Recipes List — Design

- **Date:** 2026-07-04
- **Status:** Approved (pending written-spec review)
- **Branch:** `feature/recipes-list` (stacked on `feature/coffees-list`; both parent branches are unmerged, so stacking avoids conflicts in the shared files `keys.ts`, `types.ts`, `AppShell.tsx`).
- **Scope:** A paginated, filterable Recipes list page consuming `GET /api/recipes`. List only — no detail, create, edit, delete, or favorite mutations.

## 1. Goal

Deliver the third frontend vertical slice: browse recipes in a paginated table
with a name search and a favorites filter. Mirrors the coffees slice structure
and reuses the kickoff foundation (`apiFetch`, TanStack Query, MUI shell, UI
primitives, `useDebounce`). This is the reusable list pattern applied to a second
resource, confirming the pattern generalizes before the sessions slice.

## 2. Decisions (locked)

- **Scope:** list only. Detail, create/edit/delete, favorite/unfavorite from the
  list, coffee/method dropdown filters, and column sorting are deferred.
- **Presentation:** MUI `Table` + `TablePagination` (matches the coffees slice).
- **Filters:** `name` (contains, debounced) and `favorite` (a "Favorites only"
  checkbox → `favorite: true`; unchecked omits the param). Changing either
  resets the page to 0.
- **Sorting:** fixed default `sort=id,asc`; interactive sorting deferred.
- **Favorite cell:** renders `★` when `favorite` is true, `—` when false.
- **Stack:** unchanged from the kickoff — Next.js App Router, TypeScript strict,
  MUI 9, TanStack Query, Vitest + RTL.

## 3. Backend contract (already implemented)

`GET /api/recipes?page={p}&size={s}&sort=id,asc&name=&favorite=` returns
`PageResponse<RecipeResponse>`. Pagination is 0-based. Filters are optional;
blank/omitted values must not appear in the query string. `favorite` is a boolean
query param (send `favorite=true` only when filtering to favorites). Backend
`RecipeFilter` also supports `coffeeId`/`methodId`, which this slice does not use.

`RecipeResponse` fields: `id, coffeeId, coffeeName, methodId, methodName, name,
coffeeGrams, waterGrams, ratio, grindSetting, waterTemp, brewTime, steps,
expectedTaste, favorite, createdAt, updatedAt`. `coffeeGrams`/`waterGrams` are
JSON numbers (backend `BigDecimal`), `waterTemp` a nullable number, `favorite` a
boolean, `coffeeName`/`methodName` are denormalized strings.

## 4. Types (`src/lib/api/types.ts`, additions)

```ts
export type Recipe = {
  id: number;
  coffeeId: number;
  coffeeName: string;
  methodId: number;
  methodName: string;
  name: string;
  coffeeGrams: number | null;
  waterGrams: number | null;
  ratio: string | null;
  grindSetting: string | null;
  waterTemp: number | null;
  brewTime: string | null;
  steps: string | null;
  expectedTaste: string | null;
  favorite: boolean;
  createdAt: string;
  updatedAt: string | null;
};

export type RecipeFilters = {
  name?: string;
  favorite?: boolean;
};
```

## 5. API module (`src/lib/api/recipes.ts`)

```ts
type ListRecipesParams = {
  page: number;
  size: number;
  sort?: string;              // default 'id,asc'
  filters?: RecipeFilters;
};

listRecipes(params: ListRecipesParams): Promise<PageResponse<Recipe>>
```

Builds a query string from `page`, `size`, `sort` (default `id,asc`); appends
`name` when non-empty after trimming; appends `favorite=true` only when
`filters.favorite === true`; then calls `apiFetch<PageResponse<Recipe>>`.

## 6. Data layer

- `src/lib/query/keys.ts`: add `recipes.list(params)` returning a stable key that
  includes page, size, sort, and the filters object.
- `src/hooks/useRecipes.ts`: `useRecipes(params)` wraps `useQuery` with
  `queryFn: () => listRecipes(params)` and `placeholderData: keepPreviousData`.
- Reuses the existing `src/hooks/useDebounce.ts`.

## 7. Components (`src/components/recipes/`)

- **`RecipesView.tsx`** (client): owns `page`, `size`, and `filters`
  (`RecipeFilters`). Debounces `filters` (300 ms) before passing to `useRecipes`.
  Changing any filter resets `page` to 0. Renders `<RecipeFilters>`, then one of:
  Spinner (initial load, `isLoading && !data`), `ErrorState` + retry
  (`isError || !data`), `EmptyState` ("No recipes found.") when `content` is
  empty, else `<RecipesTable>` + `<TablePagination>`.
- **`RecipeFilters.tsx`**: a name `TextField` and a MUI `Checkbox` labeled
  "Favorites only". Props: `value: RecipeFilters`,
  `onChange: (next: RecipeFilters) => void`. Name change → `onChange({ ...value,
  name })`; checkbox → `onChange({ ...value, favorite: checked ? true : undefined })`.
- **`RecipesTable.tsx`**: MUI `Table` with columns Name, Coffee, Method, Ratio,
  Water Temp, Favorite. Props: `recipes: Recipe[]`. Renders `coffeeName` /
  `methodName`; null cell values render `—`; the Favorite cell renders `★` when
  `favorite` is true, `—` when false.
- **`src/app/recipes/page.tsx`**: server shell rendering `<RecipesView/>`.

## 8. Pagination

MUI `TablePagination`: `count={totalElements}`, `page={page}` (0-based),
`rowsPerPage={size}`, `rowsPerPageOptions={[10, 20, 50]}`, `onPageChange` updates
`page`, `onRowsPerPageChange` sets `size` and resets `page` to 0.

## 9. Navigation

Enable the Recipes entry in `src/components/layout/AppShell.tsx`: set
`{ label: 'Recipes', href: '/recipes', enabled: true }` and render it as a `Link`.

## 10. States

- Initial load (no previous data): `<Spinner/>`.
- Error: `<ErrorState message="Could not load recipes." onRetry={refetch} />`.
- Success with empty `content`: `<EmptyState message="No recipes found." />`.
- Success with rows: table + pagination; `keepPreviousData` keeps the prior page
  visible during a background refetch.

## 11. Testing

- `listRecipes`: builds the correct query string — includes page/size/sort, omits
  blank `name`, includes `favorite=true` only when the filter is set (mock
  `fetch`, assert URL).
- `useRecipes`: returns the paginated recipes (mock `listRecipes`).
- `RecipesTable`: renders a row per recipe with `coffeeName`/`methodName`/ratio/
  waterTemp; `★` for a favorite row and `—` for a non-favorite; `—` for a null
  cell.
- `RecipeFilters`: typing in Name calls `onChange` with merged filters; checking
  "Favorites only" calls `onChange` with `favorite: true`.
- `RecipesView`: loading, error, empty, and success states with `useRecipes`
  mocked; changing a filter resets page to 0 (click next → page 1, change filter
  → page 0, asserted on the mocked hook's call args).

Mock the API client or the hook — no real network in tests.

## 12. Out of scope (future specs)

Recipe detail page, create/edit/delete forms, favorite/unfavorite mutations from
the list, coffee/method dropdown filters, interactive column sorting, URL-synced
filter state.

## 13. Acceptance criteria

- `npm run build` clean; `npm test` passes (new tests included).
- With the backend running, `/recipes` shows a paginated table; the name filter
  and the Favorites-only checkbox both update results; blank/false filters are
  omitted from the request.
- Loading, error, and empty states are each reachable and handled.
- The Recipes nav item is enabled and routes to `/recipes`.
