# BrewDeck Frontend Kickoff — Design

- **Date:** 2026-07-03
- **Status:** Approved (pending written-spec review)
- **Scope:** Scaffold the Next.js frontend, establish foundation (API client, data
  layer, app shell), and deliver one thin vertical slice: the Dashboard summary
  page wired end-to-end to `GET /api/dashboard/summary`.

## 1. Goal

Stand up `brewdeck-web/` and prove the full frontend stack end-to-end with the
smallest real feature. Everything after this (coffees, recipes, sessions, forms,
pagination/filter UI) is deferred to its own spec. The kickoff exercises: project
tooling, typed API client layer, TanStack Query data layer, MUI theming and app
shell, and a real page with loading/error/empty handling.

## 2. Decisions (locked)

- **Framework:** Next.js latest, App Router, TypeScript `strict`.
- **UI:** Material UI (MUI) — `@mui/material`, `@emotion/react`, `@emotion/styled`,
  `@mui/icons-material`, `@mui/material-nextjs` (App Router Emotion cache). No Tailwind.
- **Server state:** TanStack Query.
- **Client state:** Zustand — **deferred** (YAGNI; introduce when a feature needs
  shared client state).
- **Forms:** React Hook Form + Zod — deferred (no forms in this slice).
- **Testing:** Vitest + React Testing Library + jsdom.
- **Package manager:** npm (pnpm not installed).
- **Integration:** feature branch `feature/frontend-kickoff` → PR into `develop`.

## 3. Location & tooling

- App lives at `brewdeck-web/` (matches README project structure).
- Scaffold via `create-next-app` (App Router, TS, ESLint, **no** Tailwind), then
  layer conventions below.
- ESLint (`next/core-web-vitals` + TypeScript) and Prettier.
- Node `engines` pinned; npm scripts: `dev`, `build`, `start`, `lint`, `test`.
- `.env.example` committed; `.env.local` gitignored.

## 4. Structure (domain-oriented)

```
brewdeck-web/src/
  app/
    layout.tsx           # root: <Providers> + <AppShell>
    providers.tsx        # AppRouterCacheProvider → ThemeProvider → CssBaseline → QueryClientProvider
    page.tsx             # / → redirect to /dashboard
    dashboard/page.tsx   # server shell → <DashboardView/>
  components/
    layout/AppShell.tsx           # AppBar + permanent Drawer + content Box
    ui/Spinner.tsx                # CircularProgress wrapper
    ui/ErrorState.tsx             # Alert(severity=error) + retry Button
    ui/EmptyState.tsx             # Typography-based placeholder
    dashboard/DashboardView.tsx   # client component, consumes the hook
    dashboard/StatCard.tsx        # Card + CardContent + Typography
  lib/
    api/client.ts        # apiFetch<T>, ApiError
    api/types.ts         # PageResponse<T>, ErrorResponse, DashboardSummary
    api/dashboard.ts     # getDashboardSummary()
    query/provider.tsx   # QueryClientProvider (client)
    query/keys.ts        # query key factory
    theme/theme.ts       # createTheme (palette, typography)
  hooks/useDashboardSummary.ts
  config/env.ts          # reads NEXT_PUBLIC_API_BASE_URL
```

## 5. API client layer

- `apiFetch<T>(path: string, init?: RequestInit): Promise<T>` — prepends
  `NEXT_PUBLIC_API_BASE_URL`, sets JSON headers, and on a non-2xx response parses
  the backend `ErrorResponse` and throws a typed `ApiError` (status, message,
  path, validationErrors). On 2xx, parses and returns JSON as `T`. Components and
  hooks never call `fetch` directly.
- `types.ts` mirrors backend DTOs:
  ```ts
  type PageResponse<T> = {
    content: T[]; page: number; size: number;
    totalElements: number; totalPages: number; first: boolean; last: boolean;
  };
  type ErrorResponse = {
    timestamp: string; status: number; error: string; message: string;
    path: string; validationErrors?: Record<string, string>;
  };
  type DashboardSummary = {
    totalCoffees: number; totalBrewMethods: number; totalRecipes: number;
    favoriteRecipes: number; totalBrewSessions: number;
    averageSessionRating: number | null;
  };
  ```
- `dashboard.ts`: `getDashboardSummary(): Promise<DashboardSummary>` calling
  `apiFetch<DashboardSummary>('/api/dashboard/summary')`.

## 6. Data layer

- One `QueryClient`. `QueryProvider` is a client component nested inside the MUI
  providers. Provider order in `providers.tsx`:
  `AppRouterCacheProvider → ThemeProvider → CssBaseline → QueryClientProvider`.
- `useDashboardSummary()` wraps `useQuery({ queryKey: keys.dashboard.summary,
  queryFn: getDashboardSummary })`. Query keys centralized in `query/keys.ts`.

## 7. Dashboard slice

- Route `/dashboard`. `dashboard/page.tsx` is a thin server shell rendering the
  client `<DashboardView/>`.
- `DashboardView` calls `useDashboardSummary()` and renders:
  - **Loading:** `<Spinner/>` (CircularProgress).
  - **Error:** `<ErrorState/>` (Alert + retry button that refetches).
  - **Success:** a `Grid` of `StatCard`s — Coffees, Brew Methods, Recipes,
    Favorite Recipes, Brew Sessions (counts), and Average Rating
    (`averageSessionRating` formatted to one decimal, or `—` when `null`).
- Zero counts render as `0` (valid state, not empty). `EmptyState` primitive is
  provided for future list pages.
- `/` redirects to `/dashboard`.

## 8. App shell

- `AppShell`: MUI `AppBar` (title "BrewDeck") + permanent `Drawer` with nav
  entries. Only Dashboard is active; Coffees / Recipes / Sessions appear as
  disabled placeholders to signal the roadmap without dead routes.

## 9. Environment & CORS

- `NEXT_PUBLIC_API_BASE_URL` default `http://localhost:8080` (via `config/env.ts`).
- Next dev server runs on `:3000`; backend CORS already allows
  `http://localhost:3000` (`CORS_ALLOWED_ORIGINS` default). No backend change needed.

## 10. Testing

- `apiFetch`: maps a non-2xx `ErrorResponse` to `ApiError`; returns parsed body on 2xx.
- `DashboardView`: loading, error, and success states with the API client (or hook)
  mocked; RTL renders wrapped in `ThemeProvider`.
- `StatCard`: renders label and value.
- Mock the API client layer — no real network in tests.

## 11. Out of scope (future specs)

Coffees / recipes / sessions list-detail-create-edit screens, favorites UI, forms
(React Hook Form + Zod), pagination and filter UI, Zustand client state,
authentication, and web CI. This kickoff proves the stack only.

## 12. Acceptance criteria

- `brewdeck-web/` builds (`npm run build`) and lints clean.
- `npm run dev` serves `/dashboard`; with the backend running, it shows live
  counts from `GET /api/dashboard/summary`.
- Loading, error, and success states are each reachable and handled.
- Tests pass (`npm test`); API client is mocked in component tests.
- `.env.example` present; no secrets committed.
