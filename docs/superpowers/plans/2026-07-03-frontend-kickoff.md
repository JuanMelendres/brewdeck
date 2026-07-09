# Frontend Kickoff Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Scaffold `brewdeck-web/` (Next.js) and deliver the Dashboard summary page wired end-to-end to `GET /api/dashboard/summary`.

**Architecture:** Next.js App Router app under `brewdeck-web/`. A typed API client (`apiFetch`) is the only place that calls `fetch`; TanStack Query hooks consume it; MUI provides theming, the app shell, and UI. One vertical slice (Dashboard) proves the stack; other screens are deferred.

**Tech Stack:** Next.js (App Router), TypeScript (strict), MUI + Emotion, TanStack Query, Vitest + React Testing Library + jsdom, npm.

## Global Constraints

- App directory: `brewdeck-web/`, source under `brewdeck-web/src/`.
- Import alias: `@/*` → `brewdeck-web/src/*`.
- TypeScript `strict` — no `any` in committed code.
- No `fetch` calls outside `src/lib/api/client.ts`.
- Backend base URL from `NEXT_PUBLIC_API_BASE_URL`, default `http://localhost:8080`.
- Backend `DashboardSummaryResponse` fields (exact): `totalCoffees`, `totalBrewMethods`, `totalRecipes`, `favoriteRecipes`, `totalBrewSessions`, `averageSessionRating` (number or `null`).
- All commands run from `brewdeck-web/` unless noted.
- Conventional Commits; commit at the end of each task.

---

### Task 1: Scaffold project and test tooling

**Files:**
- Create: `brewdeck-web/` (via `create-next-app`)
- Create: `brewdeck-web/vitest.config.ts`
- Create: `brewdeck-web/vitest.setup.ts`
- Create: `brewdeck-web/src/test/renderWithTheme.tsx`
- Create: `brewdeck-web/.env.example`
- Create: `brewdeck-web/src/lib/sanity.test.ts`
- Modify: `brewdeck-web/package.json` (add `test` script)

**Interfaces:**
- Produces: a buildable Next.js app; `npm test` runs Vitest; `renderWithTheme(ui)` helper for component tests.

- [ ] **Step 1: Scaffold the app (run from repo root `/Users/jvilla/Documents/brewdeck`)**

```bash
npx create-next-app@latest brewdeck-web \
  --ts --app --src-dir --eslint --no-tailwind \
  --import-alias "@/*" --use-npm --no-turbopack
```

If any prompt still appears, accept these answers: TypeScript **yes**, ESLint **yes**, Tailwind **no**, `src/` **yes**, App Router **yes**, import alias `@/*`, Turbopack **no**.

- [ ] **Step 2: Install runtime and test dependencies (run from `brewdeck-web/`)**

```bash
cd brewdeck-web
npm install @mui/material @emotion/react @emotion/styled @mui/icons-material @mui/material-nextjs @tanstack/react-query
npm install -D vitest @vitejs/plugin-react vite-tsconfig-paths jsdom @testing-library/react @testing-library/jest-dom @testing-library/user-event
```

- [ ] **Step 3: Add the `test` script to `package.json`**

In `brewdeck-web/package.json`, add to `"scripts"`:

```json
"test": "vitest run",
"test:watch": "vitest"
```

- [ ] **Step 4: Create `vitest.config.ts`**

```ts
import { defineConfig } from 'vitest/config';
import react from '@vitejs/plugin-react';
import tsconfigPaths from 'vite-tsconfig-paths';

export default defineConfig({
  plugins: [react(), tsconfigPaths()],
  test: {
    environment: 'jsdom',
    globals: true,
    setupFiles: './vitest.setup.ts',
  },
});
```

- [ ] **Step 5: Create `vitest.setup.ts`**

```ts
import '@testing-library/jest-dom/vitest';
```

- [ ] **Step 6: Create `src/test/renderWithTheme.tsx`**

```tsx
import { render } from '@testing-library/react';
import { ThemeProvider } from '@mui/material/styles';
import type { ReactElement } from 'react';
import { theme } from '@/lib/theme/theme';

export function renderWithTheme(ui: ReactElement) {
  return render(<ThemeProvider theme={theme}>{ui}</ThemeProvider>);
}
```

> Note: this imports `@/lib/theme/theme`, created in Task 4. It is unused until then; that is fine — it is not run by the Task 1 sanity test.

- [ ] **Step 7: Create `.env.example`**

```
NEXT_PUBLIC_API_BASE_URL=http://localhost:8080
```

- [ ] **Step 8: Write a sanity test — `src/lib/sanity.test.ts`**

```ts
import { describe, expect, it } from 'vitest';

describe('tooling', () => {
  it('runs vitest', () => {
    expect(1 + 1).toBe(2);
  });
});
```

- [ ] **Step 9: Run the sanity test**

Run: `npm test`
Expected: PASS (1 test).

- [ ] **Step 10: Verify the app builds**

Run: `npm run build`
Expected: build completes with no type or lint errors.

- [ ] **Step 11: Commit (run from repo root)**

```bash
cd /Users/jvilla/Documents/brewdeck
git add brewdeck-web
git commit -m "feat(web): scaffold Next.js app with MUI and Vitest tooling"
```

---

### Task 2: API client layer

**Files:**
- Create: `brewdeck-web/src/config/env.ts`
- Create: `brewdeck-web/src/lib/api/types.ts`
- Create: `brewdeck-web/src/lib/api/client.ts`
- Create: `brewdeck-web/src/lib/api/dashboard.ts`
- Test: `brewdeck-web/src/lib/api/client.test.ts`

**Interfaces:**
- Produces:
  - `API_BASE_URL: string`
  - `type PageResponse<T>`, `type ErrorResponse`, `type DashboardSummary`
  - `class ApiError extends Error { status: number; path?: string; validationErrors?: Record<string,string> }`
  - `apiFetch<T>(path: string, init?: RequestInit): Promise<T>`
  - `getDashboardSummary(): Promise<DashboardSummary>`

- [ ] **Step 1: Create `src/config/env.ts`**

```ts
export const API_BASE_URL =
  process.env.NEXT_PUBLIC_API_BASE_URL ?? 'http://localhost:8080';
```

- [ ] **Step 2: Create `src/lib/api/types.ts`**

```ts
export type PageResponse<T> = {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
};

export type ErrorResponse = {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  path: string;
  validationErrors?: Record<string, string>;
};

export type DashboardSummary = {
  totalCoffees: number;
  totalBrewMethods: number;
  totalRecipes: number;
  favoriteRecipes: number;
  totalBrewSessions: number;
  averageSessionRating: number | null;
};
```

- [ ] **Step 3: Write the failing test — `src/lib/api/client.test.ts`**

```ts
import { afterEach, describe, expect, it, vi } from 'vitest';
import { ApiError, apiFetch } from './client';

function mockFetchOnce(body: unknown, init: { ok: boolean; status: number }) {
  vi.stubGlobal(
    'fetch',
    vi.fn().mockResolvedValue({
      ok: init.ok,
      status: init.status,
      statusText: 'Status',
      json: () => Promise.resolve(body),
    }),
  );
}

afterEach(() => vi.unstubAllGlobals());

describe('apiFetch', () => {
  it('returns parsed JSON on a 2xx response', async () => {
    mockFetchOnce({ value: 42 }, { ok: true, status: 200 });

    const result = await apiFetch<{ value: number }>('/api/thing');

    expect(result).toEqual({ value: 42 });
  });

  it('throws ApiError with the backend message on a non-2xx response', async () => {
    mockFetchOnce(
      {
        status: 400,
        error: 'Bad Request',
        message: 'Malformed request body',
        path: '/api/thing',
      },
      { ok: false, status: 400 },
    );

    await expect(apiFetch('/api/thing')).rejects.toMatchObject({
      name: 'ApiError',
      status: 400,
      message: 'Malformed request body',
      path: '/api/thing',
    });

    await expect(apiFetch('/api/thing')).rejects.toBeInstanceOf(ApiError);
  });
});
```

- [ ] **Step 4: Run test to verify it fails**

Run: `npm test -- src/lib/api/client.test.ts`
Expected: FAIL (cannot find `./client`).

- [ ] **Step 5: Create `src/lib/api/client.ts`**

```ts
import { API_BASE_URL } from '@/config/env';
import type { ErrorResponse } from './types';

export class ApiError extends Error {
  status: number;
  path?: string;
  validationErrors?: Record<string, string>;

  constructor(
    status: number,
    message: string,
    path?: string,
    validationErrors?: Record<string, string>,
  ) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.path = path;
    this.validationErrors = validationErrors;
  }
}

export async function apiFetch<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...init,
    headers: { 'Content-Type': 'application/json', ...init?.headers },
  });

  if (!response.ok) {
    let body: Partial<ErrorResponse> = {};
    try {
      body = (await response.json()) as ErrorResponse;
    } catch {
      // non-JSON error body; fall back to status text
    }
    throw new ApiError(
      response.status,
      body.message ?? response.statusText,
      body.path,
      body.validationErrors,
    );
  }

  if (response.status === 204) {
    return undefined as T;
  }

  return (await response.json()) as T;
}
```

- [ ] **Step 6: Run test to verify it passes**

Run: `npm test -- src/lib/api/client.test.ts`
Expected: PASS.

- [ ] **Step 7: Create `src/lib/api/dashboard.ts`**

```ts
import { apiFetch } from './client';
import type { DashboardSummary } from './types';

export function getDashboardSummary(): Promise<DashboardSummary> {
  return apiFetch<DashboardSummary>('/api/dashboard/summary');
}
```

- [ ] **Step 8: Commit (run from repo root)**

```bash
cd /Users/jvilla/Documents/brewdeck
git add brewdeck-web/src/config brewdeck-web/src/lib/api
git commit -m "feat(web): add typed API client layer and dashboard endpoint"
```

---

### Task 3: Data layer (TanStack Query)

**Files:**
- Create: `brewdeck-web/src/lib/query/keys.ts`
- Create: `brewdeck-web/src/lib/query/provider.tsx`
- Create: `brewdeck-web/src/hooks/useDashboardSummary.ts`
- Test: `brewdeck-web/src/hooks/useDashboardSummary.test.tsx`

**Interfaces:**
- Consumes: `getDashboardSummary()`, `DashboardSummary` (Task 2).
- Produces:
  - `keys.dashboard.summary` (readonly tuple)
  - `QueryProvider({ children })` client component
  - `useDashboardSummary()` returning TanStack Query's `UseQueryResult<DashboardSummary, Error>`

- [ ] **Step 1: Create `src/lib/query/keys.ts`**

```ts
export const keys = {
  dashboard: {
    summary: ['dashboard', 'summary'] as const,
  },
};
```

- [ ] **Step 2: Create `src/lib/query/provider.tsx`**

```tsx
'use client';

import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { useState, type ReactNode } from 'react';

export function QueryProvider({ children }: { children: ReactNode }) {
  const [client] = useState(() => new QueryClient());
  return <QueryClientProvider client={client}>{children}</QueryClientProvider>;
}
```

- [ ] **Step 3: Write the failing test — `src/hooks/useDashboardSummary.test.tsx`**

```tsx
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { renderHook, waitFor } from '@testing-library/react';
import type { ReactNode } from 'react';
import { describe, expect, it, vi } from 'vitest';
import { useDashboardSummary } from './useDashboardSummary';
import * as dashboardApi from '@/lib/api/dashboard';

function wrapper({ children }: { children: ReactNode }) {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return <QueryClientProvider client={client}>{children}</QueryClientProvider>;
}

describe('useDashboardSummary', () => {
  it('returns the dashboard summary from the API', async () => {
    vi.spyOn(dashboardApi, 'getDashboardSummary').mockResolvedValue({
      totalCoffees: 5,
      totalBrewMethods: 4,
      totalRecipes: 10,
      favoriteRecipes: 3,
      totalBrewSessions: 20,
      averageSessionRating: 4.25,
    });

    const { result } = renderHook(() => useDashboardSummary(), { wrapper });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data?.totalCoffees).toBe(5);
  });
});
```

- [ ] **Step 4: Run test to verify it fails**

Run: `npm test -- src/hooks/useDashboardSummary.test.tsx`
Expected: FAIL (cannot find `./useDashboardSummary`).

- [ ] **Step 5: Create `src/hooks/useDashboardSummary.ts`**

```ts
'use client';

import { useQuery } from '@tanstack/react-query';
import { getDashboardSummary } from '@/lib/api/dashboard';
import { keys } from '@/lib/query/keys';

export function useDashboardSummary() {
  return useQuery({
    queryKey: keys.dashboard.summary,
    queryFn: getDashboardSummary,
  });
}
```

- [ ] **Step 6: Run test to verify it passes**

Run: `npm test -- src/hooks/useDashboardSummary.test.tsx`
Expected: PASS.

- [ ] **Step 7: Commit (run from repo root)**

```bash
cd /Users/jvilla/Documents/brewdeck
git add brewdeck-web/src/lib/query brewdeck-web/src/hooks
git commit -m "feat(web): add query provider and dashboard summary hook"
```

---

### Task 4: Theme, providers, layout, and app shell

**Files:**
- Create: `brewdeck-web/src/lib/theme/theme.ts`
- Create: `brewdeck-web/src/app/providers.tsx`
- Create: `brewdeck-web/src/components/layout/AppShell.tsx`
- Modify: `brewdeck-web/src/app/layout.tsx`
- Replace: `brewdeck-web/src/app/page.tsx`

**Interfaces:**
- Consumes: `QueryProvider` (Task 3).
- Produces: `theme`, `Providers({ children })`, `AppShell({ children })`; root layout renders `<Providers><AppShell>{children}</AppShell></Providers>`; `/` redirects to `/dashboard`.

- [ ] **Step 1: Create `src/lib/theme/theme.ts`**

```ts
'use client';

import { createTheme } from '@mui/material/styles';

export const theme = createTheme({
  palette: {
    mode: 'light',
    primary: { main: '#6f4e37' },
  },
});
```

- [ ] **Step 2: Create `src/app/providers.tsx`**

```tsx
'use client';

import { AppRouterCacheProvider } from '@mui/material-nextjs/v15-appRouter';
import { ThemeProvider } from '@mui/material/styles';
import CssBaseline from '@mui/material/CssBaseline';
import type { ReactNode } from 'react';
import { theme } from '@/lib/theme/theme';
import { QueryProvider } from '@/lib/query/provider';

export function Providers({ children }: { children: ReactNode }) {
  return (
    <AppRouterCacheProvider>
      <ThemeProvider theme={theme}>
        <CssBaseline />
        <QueryProvider>{children}</QueryProvider>
      </ThemeProvider>
    </AppRouterCacheProvider>
  );
}
```

> Note: the import path segment `v15-appRouter` must match the installed `@mui/material-nextjs` version and the Next major version. If `npm run build` reports the module path is not found, list `node_modules/@mui/material-nextjs/` and use the matching `vNN-appRouter` entry.

- [ ] **Step 3: Create `src/components/layout/AppShell.tsx`**

```tsx
'use client';

import AppBar from '@mui/material/AppBar';
import Box from '@mui/material/Box';
import Drawer from '@mui/material/Drawer';
import List from '@mui/material/List';
import ListItemButton from '@mui/material/ListItemButton';
import ListItemText from '@mui/material/ListItemText';
import Toolbar from '@mui/material/Toolbar';
import Typography from '@mui/material/Typography';
import Link from 'next/link';
import type { ReactNode } from 'react';

const DRAWER_WIDTH = 220;

const NAV = [
  { label: 'Dashboard', href: '/dashboard', enabled: true },
  { label: 'Coffees', href: '/coffees', enabled: false },
  { label: 'Recipes', href: '/recipes', enabled: false },
  { label: 'Brew Sessions', href: '/brew-sessions', enabled: false },
];

export function AppShell({ children }: { children: ReactNode }) {
  return (
    <Box sx={{ display: 'flex' }}>
      <AppBar
        position="fixed"
        sx={{ zIndex: (t) => t.zIndex.drawer + 1 }}
      >
        <Toolbar>
          <Typography variant="h6" noWrap>
            BrewDeck
          </Typography>
        </Toolbar>
      </AppBar>
      <Drawer
        variant="permanent"
        sx={{
          width: DRAWER_WIDTH,
          flexShrink: 0,
          [`& .MuiDrawer-paper`]: { width: DRAWER_WIDTH, boxSizing: 'border-box' },
        }}
      >
        <Toolbar />
        <List>
          {NAV.map((item) =>
            item.enabled ? (
              <ListItemButton key={item.href} component={Link} href={item.href}>
                <ListItemText primary={item.label} />
              </ListItemButton>
            ) : (
              <ListItemButton key={item.href} disabled>
                <ListItemText primary={item.label} />
              </ListItemButton>
            ),
          )}
        </List>
      </Drawer>
      <Box component="main" sx={{ flexGrow: 1, p: 3 }}>
        <Toolbar />
        {children}
      </Box>
    </Box>
  );
}
```

- [ ] **Step 4: Replace `src/app/layout.tsx`**

```tsx
import type { Metadata } from 'next';
import type { ReactNode } from 'react';
import { Providers } from './providers';
import { AppShell } from '@/components/layout/AppShell';

export const metadata: Metadata = {
  title: 'BrewDeck',
  description: 'Coffee brewing companion',
};

export default function RootLayout({ children }: { children: ReactNode }) {
  return (
    <html lang="en">
      <body>
        <Providers>
          <AppShell>{children}</AppShell>
        </Providers>
      </body>
    </html>
  );
}
```

> Note: remove any font imports or `className` on `<body>` that `create-next-app` added and that reference deleted files. Keep `<body>` plain.

- [ ] **Step 5: Replace `src/app/page.tsx`**

```tsx
import { redirect } from 'next/navigation';

export default function Home() {
  redirect('/dashboard');
}
```

- [ ] **Step 6: Delete leftover scaffold styles**

Remove `src/app/page.module.css` if present, and any `import` of it. Leave `src/app/globals.css` but empty its body except for `html, body { margin: 0; padding: 0; }` if `create-next-app` filled it with demo styles.

- [ ] **Step 7: Verify build**

Run (from `brewdeck-web/`): `npm run build`
Expected: build succeeds. `/dashboard` will 404 until Task 7 — that is expected; the build does not fail on a missing not-yet-created route.

- [ ] **Step 8: Commit (run from repo root)**

```bash
cd /Users/jvilla/Documents/brewdeck
git add brewdeck-web/src
git commit -m "feat(web): add MUI theme, providers, layout and app shell"
```

---

### Task 5: UI state primitives

**Files:**
- Create: `brewdeck-web/src/components/ui/Spinner.tsx`
- Create: `brewdeck-web/src/components/ui/ErrorState.tsx`
- Create: `brewdeck-web/src/components/ui/EmptyState.tsx`
- Test: `brewdeck-web/src/components/ui/ErrorState.test.tsx`

**Interfaces:**
- Produces:
  - `Spinner()` — centered `CircularProgress`
  - `ErrorState({ message, onRetry }: { message: string; onRetry?: () => void })`
  - `EmptyState({ message }: { message: string })`

- [ ] **Step 1: Create `src/components/ui/Spinner.tsx`**

```tsx
'use client';

import Box from '@mui/material/Box';
import CircularProgress from '@mui/material/CircularProgress';

export function Spinner() {
  return (
    <Box sx={{ display: 'flex', justifyContent: 'center', p: 4 }} role="status" aria-label="Loading">
      <CircularProgress />
    </Box>
  );
}
```

- [ ] **Step 2: Create `src/components/ui/EmptyState.tsx`**

```tsx
'use client';

import Typography from '@mui/material/Typography';

export function EmptyState({ message }: { message: string }) {
  return (
    <Typography variant="body1" color="text.secondary" sx={{ p: 4, textAlign: 'center' }}>
      {message}
    </Typography>
  );
}
```

- [ ] **Step 3: Write the failing test — `src/components/ui/ErrorState.test.tsx`**

```tsx
import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { renderWithTheme } from '@/test/renderWithTheme';
import { ErrorState } from './ErrorState';

describe('ErrorState', () => {
  it('shows the message', () => {
    renderWithTheme(<ErrorState message="Something failed" />);
    expect(screen.getByText('Something failed')).toBeInTheDocument();
  });

  it('calls onRetry when the retry button is clicked', async () => {
    const onRetry = vi.fn();
    renderWithTheme(<ErrorState message="Failed" onRetry={onRetry} />);

    await userEvent.click(screen.getByRole('button', { name: /retry/i }));

    expect(onRetry).toHaveBeenCalledTimes(1);
  });
});
```

- [ ] **Step 4: Run test to verify it fails**

Run: `npm test -- src/components/ui/ErrorState.test.tsx`
Expected: FAIL (cannot find `./ErrorState`).

- [ ] **Step 5: Create `src/components/ui/ErrorState.tsx`**

```tsx
'use client';

import Alert from '@mui/material/Alert';
import Button from '@mui/material/Button';

export function ErrorState({
  message,
  onRetry,
}: {
  message: string;
  onRetry?: () => void;
}) {
  return (
    <Alert
      severity="error"
      action={
        onRetry ? (
          <Button color="inherit" size="small" onClick={onRetry}>
            Retry
          </Button>
        ) : undefined
      }
    >
      {message}
    </Alert>
  );
}
```

- [ ] **Step 6: Run test to verify it passes**

Run: `npm test -- src/components/ui/ErrorState.test.tsx`
Expected: PASS (2 tests).

- [ ] **Step 7: Commit (run from repo root)**

```bash
cd /Users/jvilla/Documents/brewdeck
git add brewdeck-web/src/components/ui
git commit -m "feat(web): add spinner, error and empty state primitives"
```

---

### Task 6: StatCard component

**Files:**
- Create: `brewdeck-web/src/components/dashboard/StatCard.tsx`
- Test: `brewdeck-web/src/components/dashboard/StatCard.test.tsx`

**Interfaces:**
- Produces: `StatCard({ label, value }: { label: string; value: string | number })`

- [ ] **Step 1: Write the failing test — `src/components/dashboard/StatCard.test.tsx`**

```tsx
import { screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { renderWithTheme } from '@/test/renderWithTheme';
import { StatCard } from './StatCard';

describe('StatCard', () => {
  it('renders the label and value', () => {
    renderWithTheme(<StatCard label="Coffees" value={5} />);
    expect(screen.getByText('Coffees')).toBeInTheDocument();
    expect(screen.getByText('5')).toBeInTheDocument();
  });

  it('renders a string value such as an em dash', () => {
    renderWithTheme(<StatCard label="Average Rating" value="—" />);
    expect(screen.getByText('—')).toBeInTheDocument();
  });
});
```

- [ ] **Step 2: Run test to verify it fails**

Run: `npm test -- src/components/dashboard/StatCard.test.tsx`
Expected: FAIL (cannot find `./StatCard`).

- [ ] **Step 3: Create `src/components/dashboard/StatCard.tsx`**

```tsx
'use client';

import Card from '@mui/material/Card';
import CardContent from '@mui/material/CardContent';
import Typography from '@mui/material/Typography';

export function StatCard({ label, value }: { label: string; value: string | number }) {
  return (
    <Card variant="outlined">
      <CardContent>
        <Typography variant="body2" color="text.secondary" gutterBottom>
          {label}
        </Typography>
        <Typography variant="h4" component="p">
          {value}
        </Typography>
      </CardContent>
    </Card>
  );
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `npm test -- src/components/dashboard/StatCard.test.tsx`
Expected: PASS (2 tests).

- [ ] **Step 5: Commit (run from repo root)**

```bash
cd /Users/jvilla/Documents/brewdeck
git add brewdeck-web/src/components/dashboard/StatCard.tsx brewdeck-web/src/components/dashboard/StatCard.test.tsx
git commit -m "feat(web): add dashboard StatCard component"
```

---

### Task 7: DashboardView and dashboard route

**Files:**
- Create: `brewdeck-web/src/components/dashboard/DashboardView.tsx`
- Create: `brewdeck-web/src/app/dashboard/page.tsx`
- Test: `brewdeck-web/src/components/dashboard/DashboardView.test.tsx`

**Interfaces:**
- Consumes: `useDashboardSummary()` (Task 3), `Spinner`/`ErrorState` (Task 5), `StatCard` (Task 6).
- Produces: `DashboardView()` client component; `/dashboard` route rendering it.

- [ ] **Step 1: Write the failing test — `src/components/dashboard/DashboardView.test.tsx`**

```tsx
import { screen } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { renderWithTheme } from '@/test/renderWithTheme';
import { DashboardView } from './DashboardView';
import * as hook from '@/hooks/useDashboardSummary';

type HookReturn = ReturnType<typeof hook.useDashboardSummary>;

function mockHook(value: Partial<HookReturn>) {
  vi.spyOn(hook, 'useDashboardSummary').mockReturnValue(value as HookReturn);
}

afterEach(() => vi.restoreAllMocks());

describe('DashboardView', () => {
  it('shows a spinner while loading', () => {
    mockHook({ isLoading: true, isError: false, data: undefined });
    renderWithTheme(<DashboardView />);
    expect(screen.getByRole('status', { name: /loading/i })).toBeInTheDocument();
  });

  it('shows an error state on failure', () => {
    mockHook({ isLoading: false, isError: true, error: new Error('boom'), refetch: vi.fn() });
    renderWithTheme(<DashboardView />);
    expect(screen.getByText(/could not load/i)).toBeInTheDocument();
  });

  it('renders stat cards on success, with an em dash for a null rating', () => {
    mockHook({
      isLoading: false,
      isError: false,
      data: {
        totalCoffees: 5,
        totalBrewMethods: 4,
        totalRecipes: 10,
        favoriteRecipes: 3,
        totalBrewSessions: 20,
        averageSessionRating: null,
      },
    });
    renderWithTheme(<DashboardView />);

    expect(screen.getByText('Coffees')).toBeInTheDocument();
    expect(screen.getByText('5')).toBeInTheDocument();
    expect(screen.getByText('Average Rating')).toBeInTheDocument();
    expect(screen.getByText('—')).toBeInTheDocument();
  });
});
```

- [ ] **Step 2: Run test to verify it fails**

Run: `npm test -- src/components/dashboard/DashboardView.test.tsx`
Expected: FAIL (cannot find `./DashboardView`).

- [ ] **Step 3: Create `src/components/dashboard/DashboardView.tsx`**

```tsx
'use client';

import Grid from '@mui/material/Grid';
import Typography from '@mui/material/Typography';
import { useDashboardSummary } from '@/hooks/useDashboardSummary';
import { Spinner } from '@/components/ui/Spinner';
import { ErrorState } from '@/components/ui/ErrorState';
import { StatCard } from './StatCard';

export function DashboardView() {
  const { data, isLoading, isError, refetch } = useDashboardSummary();

  if (isLoading) {
    return <Spinner />;
  }

  if (isError || !data) {
    return <ErrorState message="Could not load dashboard summary." onRetry={() => refetch()} />;
  }

  const cards: Array<{ label: string; value: string | number }> = [
    { label: 'Coffees', value: data.totalCoffees },
    { label: 'Brew Methods', value: data.totalBrewMethods },
    { label: 'Recipes', value: data.totalRecipes },
    { label: 'Favorite Recipes', value: data.favoriteRecipes },
    { label: 'Brew Sessions', value: data.totalBrewSessions },
    {
      label: 'Average Rating',
      value: data.averageSessionRating === null ? '—' : data.averageSessionRating.toFixed(1),
    },
  ];

  return (
    <>
      <Typography variant="h5" component="h1" gutterBottom>
        Dashboard
      </Typography>
      <Grid container spacing={2}>
        {cards.map((card) => (
          <Grid key={card.label} size={{ xs: 12, sm: 6, md: 4 }}>
            <StatCard label={card.label} value={card.value} />
          </Grid>
        ))}
      </Grid>
    </>
  );
}
```

> Note: this uses MUI Grid v2 (`size={{ ... }}`), the default in current MUI. If the installed MUI uses the legacy Grid API, the build will flag `size`; switch to `<Grid item xs={12} sm={6} md={4}>` in that case.

- [ ] **Step 4: Run test to verify it passes**

Run: `npm test -- src/components/dashboard/DashboardView.test.tsx`
Expected: PASS (3 tests).

- [ ] **Step 5: Create `src/app/dashboard/page.tsx`**

```tsx
import { DashboardView } from '@/components/dashboard/DashboardView';

export default function DashboardPage() {
  return <DashboardView />;
}
```

- [ ] **Step 6: Run the full test suite and build**

Run (from `brewdeck-web/`): `npm test && npm run build`
Expected: all tests pass; build succeeds; `/dashboard` route present.

- [ ] **Step 7: Commit (run from repo root)**

```bash
cd /Users/jvilla/Documents/brewdeck
git add brewdeck-web/src/components/dashboard brewdeck-web/src/app/dashboard
git commit -m "feat(web): add dashboard view wired to the summary endpoint"
```

---

### Task 8: Manual verification and pull request

**Files:** none (verification + PR).

- [ ] **Step 1: Start the backend (from repo root)**

```bash
cd /Users/jvilla/Documents/brewdeck
docker compose up -d
cd brewdeck-api && sh ./mvnw spring-boot:run
```

Leave it running; wait for "Started BrewdeckApiApplication".

- [ ] **Step 2: Start the frontend (new shell, from `brewdeck-web/`)**

```bash
cd /Users/jvilla/Documents/brewdeck/brewdeck-web
cp .env.example .env.local
npm run dev
```

- [ ] **Step 3: Verify in the browser**

Open `http://localhost:3000`. Expected: redirect to `/dashboard`; six stat cards render with live counts; no CORS error in the browser console.

- [ ] **Step 4: Verify the error state**

Stop the backend (Ctrl-C in its shell), reload `/dashboard`. Expected: the error alert with a Retry button appears. Restart the backend, click Retry. Expected: cards load.

- [ ] **Step 5: Confirm `.env.local` is ignored**

Run (from `brewdeck-web/`): `git status --short`
Expected: `.env.local` does NOT appear (Next's `.gitignore` ignores `.env*.local`).

- [ ] **Step 6: Push the branch and open the PR (from repo root)**

```bash
cd /Users/jvilla/Documents/brewdeck
git push -u origin feature/frontend-kickoff
gh pr create --base develop --head feature/frontend-kickoff \
  --title "feat(web): frontend kickoff — scaffold and dashboard slice" \
  --body "Scaffolds brewdeck-web (Next.js App Router, TypeScript, MUI, TanStack Query) and delivers the dashboard summary page wired to GET /api/dashboard/summary. Implements the design in docs/superpowers/specs/2026-07-03-frontend-kickoff-design.md.

🤖 Generated with [Claude Code](https://claude.com/claude-code)"
```

- [ ] **Step 7: Confirm the PR opened**

Run: `gh pr view --web`
Expected: the PR page opens against `develop`.

---

## Notes for the implementer

- Run test/build/lint commands from `brewdeck-web/`; run `git` commands from the repo root so paths resolve.
- If `create-next-app` scaffolds a demo `globals.css`/`page.module.css`, strip demo content (Task 4, Step 6) so the build has no dangling references.
- Two version-sensitive spots are called out inline: the `@mui/material-nextjs/vNN-appRouter` import path (Task 4) and the MUI Grid API (Task 7). Adjust to the installed versions if the build flags them.
