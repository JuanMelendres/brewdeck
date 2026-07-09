# BrewDeck Coffees List — Design

- **Date:** 2026-07-03
- **Status:** Approved (pending written-spec review)
- **Branch:** `feature/coffees-list` (off `feature/frontend-kickoff`, whose foundation is not yet merged)
- **Scope:** A paginated, filterable Coffees list page consuming `GET /api/coffees`. List only — no detail, create, edit, or delete.

## 1. Goal

Deliver the second frontend vertical slice: browse coffees in a paginated table
with server-side filters. This exercises the `PageResponse<T>` envelope,
server-driven pagination, and filter query construction — the patterns every
later list screen (recipes, sessions) will reuse. The kickoff foundation
(`apiFetch`, TanStack Query provider, app shell, UI primitives) is reused as-is.

## 2. Decisions (locked)

- **Scope:** list only. Detail, create/edit/delete, column sorting, and saved
  filters are deferred to later specs.
- **Presentation:** MUI `Table` + `TablePagination` (no `@mui/x-data-grid`).
- **Filters:** all four `CoffeeFilter` fields — `name` (contains), `origin`,
  `roastLevel`, `process` (exact match). Debounced; changing any filter resets
  the page to 0.
- **Sorting:** fixed default `sort=id,asc`; interactive column sorting deferred.
- **Stack:** unchanged from the kickoff — Next.js App Router, TypeScript strict,
  MUI 9, TanStack Query, Vitest + RTL.

## 3. Backend contract (already implemented)

`GET /api/coffees?page={p}&size={s}&sort=id,asc&name=&origin=&roastLevel=&process=`
returns `PageResponse<CoffeeResponse>`. Pagination is 0-based. Filters are
optional; blank values must be omitted from the query string. `CoffeeResponse`
fields: `id, name, brand, origin, region, farm, producer, variety, process,
roastLevel, notesPrimary, notesSecondary, acidity, body, sweetness, bitterness,
description, createdAt, updatedAt`.

## 4. Types (`src/lib/api/types.ts`, additions)

```ts
export type Coffee = {
  id: number;
  name: string;
  brand: string | null;
  origin: string | null;
  region: string | null;
  farm: string | null;
  producer: string | null;
  variety: string | null;
  process: string | null;
  roastLevel: string | null;
  notesPrimary: string | null;
  notesSecondary: string | null;
  acidity: string | null;
  body: string | null;
  sweetness: string | null;
  bitterness: string | null;
  description: string | null;
  createdAt: string;
  updatedAt: string | null;
};

export type CoffeeFilters = {
  name?: string;
  origin?: string;
  roastLevel?: string;
  process?: string;
};
```

## 5. API module (`src/lib/api/coffees.ts`)

```ts
type ListCoffeesParams = {
  page: number;
  size: number;
  sort?: string;              // default 'id,asc'
  filters?: CoffeeFilters;
};

listCoffees(params: ListCoffeesParams): Promise<PageResponse<Coffee>>
```

Builds a query string from `page`, `size`, `sort` (default `id,asc`), and any
filter values that are non-empty after trimming (blank/undefined omitted), then
calls `apiFetch<PageResponse<Coffee>>('/api/coffees?...')`. This is the only new
place that assembles query parameters.

## 6. Data layer

- `src/lib/query/keys.ts`: add `coffees.list(params)` returning a stable key
  that includes page, size, sort, and the filters object, so each distinct query
  is cached separately.
- `src/hooks/useCoffees.ts`: `useCoffees(params)` wraps `useQuery` with
  `queryFn: () => listCoffees(params)` and `placeholderData: keepPreviousData`
  so the table does not flash empty while the next page/filter loads.
- `src/hooks/useDebounce.ts`: generic `useDebounce<T>(value: T, delayMs: number): T`.

## 7. Components (`src/components/coffees/`)

- **`CoffeesView.tsx`** (client): owns state — `page`, `size`, and a `filters`
  object. Debounces `filters` (300 ms) before passing to `useCoffees`. Changing
  any filter resets `page` to 0. Renders `<CoffeeFilters>`, then one of:
  Spinner (initial load), `ErrorState` + retry (error), `EmptyState`
  ("No coffees found") when `content` is empty, else `<CoffeesTable>` +
  `<TablePagination>`. Uses `keepPreviousData` so pagination stays stable.
- **`CoffeeFilters.tsx`**: controlled filter bar with four MUI `TextField`s
  (Name, Origin, Roast Level, Process). Props: `value: CoffeeFilters`,
  `onChange: (next: CoffeeFilters) => void`.
- **`CoffeesTable.tsx`**: MUI `Table` with columns Name, Brand, Origin, Roast,
  Process. Props: `coffees: Coffee[]`. Null cell values render as an em dash "—".
- **`src/app/coffees/page.tsx`**: server shell rendering `<CoffeesView/>`.

## 8. Pagination

MUI `TablePagination`: `count={totalElements}`, `page={page}` (0-based, direct
match to backend), `rowsPerPage={size}`, `rowsPerPageOptions={[10, 20, 50]}`,
`onPageChange` updates `page`, `onRowsPerPageChange` sets `size` and resets
`page` to 0.

## 9. Navigation

Enable the Coffees entry in `src/components/layout/AppShell.tsx`: set
`{ label: 'Coffees', href: '/coffees', enabled: true }` and render it as a
`Link` like Dashboard.

## 10. States

- Initial load (no previous data): `<Spinner/>`.
- Error: `<ErrorState message="Could not load coffees." onRetry={refetch} />`.
- Success with empty `content`: `<EmptyState message="No coffees found." />`.
- Success with rows: table + pagination. During a background refetch
  (`keepPreviousData`), the previous page stays visible.

## 11. Testing

- `listCoffees`: builds the correct query string — includes page/size/sort,
  omits blank filter values, includes non-blank ones (mock `fetch`, assert URL).
- `useDebounce`: returns the initial value immediately and the updated value only
  after the delay (fake timers).
- `CoffeesTable`: renders a row per coffee with the right cells; renders "—" for
  null values.
- `CoffeeFilters`: typing in a field calls `onChange` with the merged filters.
- `CoffeesView`: loading, error, empty, and success states with `useCoffees`
  mocked; changing a filter resets page to 0 (assert the params passed to the
  mocked hook / the page state).

Mock the API client or the `useCoffees` hook — no real network in tests.

## 12. Out of scope (future specs)

Coffee detail page, create/edit/delete forms, interactive column sorting, saved
filter presets, and URL-synced filter state.

## 13. Acceptance criteria

- `npm run build` clean; `npm test` passes (new tests included).
- With the backend running, `/coffees` shows a paginated table; pagination and
  each filter update results; blank filters are omitted from the request.
- Loading, error, and empty states are each reachable and handled.
- The Coffees nav item is enabled and routes to `/coffees`.
