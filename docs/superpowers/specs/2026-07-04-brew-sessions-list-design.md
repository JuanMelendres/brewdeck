# BrewDeck Brew Sessions List — Design

- **Date:** 2026-07-04
- **Status:** Approved (pending written-spec review)
- **Branch:** `feature/brew-sessions-list` (stacked on `feature/recipes-list`; the whole kickoff → coffees → recipes chain is unmerged, so stacking avoids conflicts in the shared files `keys.ts`, `types.ts`, `AppShell.tsx`).
- **Scope:** A paginated, rating-filterable Brew Sessions list page consuming `GET /api/brew-sessions`. List only — no detail, create, edit, delete, or sessions-by-recipe view.

## 1. Goal

Deliver the fourth and final list vertical slice: browse brew sessions in a
paginated table with a rating filter. Completes the four-resource list set
(coffees, recipes, sessions) on the reusable list pattern. Reuses the kickoff
foundation (`apiFetch`, TanStack Query, MUI shell, UI primitives, `useDebounce`).

## 2. Decisions (locked)

- **Scope:** list only. Detail, create/edit/delete, sessions-by-recipe, a recipe
  dropdown filter, and column sorting are deferred.
- **Presentation:** MUI `Table` + `TablePagination` (matches the other slices).
- **Filter:** `rating` only — a numeric `TextField` (empty → omitted; otherwise the
  number). Changing it resets the page to 0. The `recipeId` filter is deferred
  (it would need a recipe dropdown).
- **Sorting:** fixed default `sort=id,asc`; interactive sorting deferred.
- **brewedAt formatting:** rendered as `YYYY-MM-DD HH:mm` via plain ISO-string
  operations (no `toLocaleString`), so the output is deterministic and
  test-stable regardless of locale/timezone.
- **Stack:** unchanged from the kickoff — Next.js App Router, TypeScript strict,
  MUI 9, TanStack Query, Vitest + RTL.

## 3. Backend contract (already implemented)

`GET /api/brew-sessions?page={p}&size={s}&sort=id,asc&rating=` returns
`PageResponse<BrewSessionResponse>`. Pagination is 0-based. Filters are optional;
blank/omitted values must not appear in the query string. `rating` is a numeric
query param (exact match). Backend `BrewSessionFilter` also supports `recipeId`,
which this slice does not use.

`BrewSessionResponse` fields: `id, recipeId, recipeName, brewedAt, actualGrind,
actualTemp, actualTime, tasteResult, rating, adjustmentNotes`. `brewedAt` is a
non-null ISO date-time string; `actualTemp` and `rating` are nullable numbers;
`recipeName` is a denormalized string; `actualGrind`/`actualTime`/`tasteResult`/
`adjustmentNotes` are nullable strings.

## 4. Types (`src/lib/api/types.ts`, additions)

```ts
export type BrewSession = {
  id: number;
  recipeId: number;
  recipeName: string;
  brewedAt: string;
  actualGrind: string | null;
  actualTemp: number | null;
  actualTime: string | null;
  tasteResult: string | null;
  rating: number | null;
  adjustmentNotes: string | null;
};

export type BrewSessionFilters = {
  rating?: number;
};
```

## 5. API module (`src/lib/api/brewSessions.ts`)

```ts
type ListBrewSessionsParams = {
  page: number;
  size: number;
  sort?: string;              // default 'id,asc'
  filters?: BrewSessionFilters;
};

listBrewSessions(params: ListBrewSessionsParams): Promise<PageResponse<BrewSession>>
```

Builds a query string from `page`, `size`, `sort` (default `id,asc`); appends
`rating` only when `filters.rating !== undefined`; then calls
`apiFetch<PageResponse<BrewSession>>`.

## 6. Data layer

- `src/lib/query/keys.ts`: add `brewSessions.list(params)` returning a stable key
  including page, size, sort, and the filters object.
- `src/hooks/useBrewSessions.ts`: `useBrewSessions(params)` wraps `useQuery` with
  `queryFn: () => listBrewSessions(params)` and `placeholderData: keepPreviousData`.
- Reuses the existing `src/hooks/useDebounce.ts`.

## 7. Components (`src/components/brew-sessions/`)

- **`BrewSessionsView.tsx`** (client): owns `page`, `size`, and `filters`
  (`BrewSessionFilters`). Debounces `filters` (300 ms) before passing to
  `useBrewSessions`. Changing the filter resets `page` to 0. Renders
  `<BrewSessionFilters>`, then one of: Spinner (`isLoading && !data`), `ErrorState`
  + retry (`isError || !data`), `EmptyState` ("No brew sessions found.") when
  `content` is empty, else `<BrewSessionsTable>` + `<TablePagination>`.
- **`BrewSessionFilters.tsx`**: a single numeric `TextField` labeled "Rating".
  Props: `value: BrewSessionFilters`, `onChange: (next: BrewSessionFilters) =>
  void`. On change: empty string → `onChange({ ...value, rating: undefined })`;
  otherwise `onChange({ ...value, rating: Number(raw) })`.
- **`BrewSessionsTable.tsx`**: MUI `Table` with columns Recipe, Brewed At, Rating,
  Actual Temp, Actual Time, Taste. Props: `sessions: BrewSession[]`. Renders
  `recipeName`; `brewedAt` via `formatDateTime` (`iso.replace('T', ' ').slice(0,
  16)` → `YYYY-MM-DD HH:mm`); null cell values (including `rating`, `actualTemp`,
  `actualTime`, `tasteResult`) render `—`.
- **`src/app/brew-sessions/page.tsx`**: server shell rendering `<BrewSessionsView/>`.

## 8. Pagination

MUI `TablePagination`: `count={totalElements}`, `page={page}` (0-based),
`rowsPerPage={size}`, `rowsPerPageOptions={[10, 20, 50]}`, `onPageChange` updates
`page`, `onRowsPerPageChange` sets `size` and resets `page` to 0.

## 9. Navigation

Enable the Brew Sessions entry in `src/components/layout/AppShell.tsx`: set
`{ label: 'Brew Sessions', href: '/brew-sessions', enabled: true }` and render it
as a `Link`. After this slice all four nav items are enabled.

## 10. States

- Initial load (no previous data): `<Spinner/>`.
- Error: `<ErrorState message="Could not load brew sessions." onRetry={refetch} />`.
- Success with empty `content`: `<EmptyState message="No brew sessions found." />`.
- Success with rows: table + pagination; `keepPreviousData` keeps the prior page
  visible during a background refetch.

## 11. Testing

- `listBrewSessions`: builds the correct query string — includes page/size/sort,
  omits `rating` when undefined, includes it when set (mock `fetch`, assert URL).
- `useBrewSessions`: returns the paginated sessions (mock `listBrewSessions`).
- `BrewSessionsTable`: renders a row per session with `recipeName`, the formatted
  `brewedAt` (`YYYY-MM-DD HH:mm`), rating, actualTemp, actualTime, taste; `—` for
  null cells.
- `BrewSessionFilters`: typing a number calls `onChange` with `{ rating: N }`;
  clearing the field calls `onChange` with `rating: undefined`.
- `BrewSessionsView`: loading, error, empty, and success states with
  `useBrewSessions` mocked; changing the filter resets page to 0 (click next →
  page 1, change filter → page 0, asserted on the mocked hook's call args).

Mock the API client or the hook — no real network in tests.

## 12. Out of scope (future specs)

Brew session detail page, create/edit/delete forms, a sessions-by-recipe view, a
recipe dropdown filter, interactive column sorting, URL-synced filter state.

## 13. Acceptance criteria

- `npm run build` clean; `npm test` passes (new tests included).
- With the backend running, `/brew-sessions` shows a paginated table; the rating
  filter updates results; a blank rating is omitted from the request.
- Loading, error, and empty states are each reachable and handled.
- The Brew Sessions nav item is enabled and routes to `/brew-sessions`.
