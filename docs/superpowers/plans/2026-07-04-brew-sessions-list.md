# Brew Sessions List Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a paginated, rating-filterable Brew Sessions list page at `/brew-sessions` consuming `GET /api/brew-sessions`.

**Architecture:** Mirror the recipes slice. `listBrewSessions` builds the query string; `useBrewSessions` (with `keepPreviousData`) feeds a client `BrewSessionsView` that owns page/size/filter state and renders an MUI Table + TablePagination + a numeric rating filter. Reuses the existing `useDebounce` and UI primitives. List only.

**Tech Stack:** Next.js App Router, TypeScript strict, MUI 9, TanStack Query, Vitest + React Testing Library.

## Global Constraints

- App under `brewdeck-web/`, source `brewdeck-web/src/`, alias `@/*` → `src/*`.
- TypeScript strict — no `any` in committed code.
- No `fetch` outside `src/lib/api/client.ts`; new API modules call `apiFetch`.
- Backend `GET /api/brew-sessions` returns `PageResponse<BrewSessionResponse>`; pagination 0-based; blank/omitted filter values must not appear in the query string; `rating` sent only when set.
- Client components using hooks/state/MUI start with `'use client'`; route `page.tsx` is a plain server component.
- Null table cell values render as an em dash "—". `brewedAt` renders as `YYYY-MM-DD HH:mm` via plain ISO-string ops (no locale formatting).
- Run npm commands from `brewdeck-web/`; run `git` from repo root `/Users/jvilla/Documents/brewdeck`.
- Conventional Commits; commit at the end of each task.

---

### Task 1: BrewSession types and listBrewSessions API

**Files:**
- Modify: `brewdeck-web/src/lib/api/types.ts` (append)
- Create: `brewdeck-web/src/lib/api/brewSessions.ts`
- Test: `brewdeck-web/src/lib/api/brewSessions.test.ts`

**Interfaces:**
- Produces:
  - `type BrewSession` (all `BrewSessionResponse` fields, nullable where the DTO is)
  - `type BrewSessionFilters = { rating?: number }`
  - `type ListBrewSessionsParams = { page: number; size: number; sort?: string; filters?: BrewSessionFilters }`
  - `listBrewSessions(params: ListBrewSessionsParams): Promise<PageResponse<BrewSession>>`

- [ ] **Step 1: Append types to `src/lib/api/types.ts`**

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

- [ ] **Step 2: Write the failing test — `src/lib/api/brewSessions.test.ts`**

```ts
import { afterEach, describe, expect, it, vi } from 'vitest';
import { listBrewSessions } from './brewSessions';

function stubFetch() {
  const fetchMock = vi.fn().mockResolvedValue({
    ok: true,
    status: 200,
    json: () => Promise.resolve({ content: [], page: 0, size: 10, totalElements: 0, totalPages: 0, first: true, last: true }),
  });
  vi.stubGlobal('fetch', fetchMock);
  return fetchMock;
}

afterEach(() => vi.unstubAllGlobals());

describe('listBrewSessions', () => {
  it('includes page/size/default sort and omits an unset rating', async () => {
    const fetchMock = stubFetch();

    await listBrewSessions({ page: 2, size: 20 });

    const url = String(fetchMock.mock.calls[0][0]);
    expect(url).toContain('/api/brew-sessions?');
    expect(url).toContain('page=2');
    expect(url).toContain('size=20');
    expect(url).toContain('sort=id%2Casc');
    expect(url).not.toContain('rating=');
  });

  it('includes a rating and a custom sort when provided', async () => {
    const fetchMock = stubFetch();

    await listBrewSessions({ page: 0, size: 10, sort: 'brewedAt,desc', filters: { rating: 9 } });

    const url = String(fetchMock.mock.calls[0][0]);
    expect(url).toContain('sort=brewedAt%2Cdesc');
    expect(url).toContain('rating=9');
  });
});
```

- [ ] **Step 3: Run test to verify it fails**

Run: `npm test -- src/lib/api/brewSessions.test.ts`
Expected: FAIL (cannot find `./brewSessions`).

- [ ] **Step 4: Create `src/lib/api/brewSessions.ts`**

```ts
import { apiFetch } from './client';
import type { BrewSession, BrewSessionFilters, PageResponse } from './types';

export type ListBrewSessionsParams = {
  page: number;
  size: number;
  sort?: string;
  filters?: BrewSessionFilters;
};

export function listBrewSessions(
  params: ListBrewSessionsParams,
): Promise<PageResponse<BrewSession>> {
  const query = new URLSearchParams();
  query.set('page', String(params.page));
  query.set('size', String(params.size));
  query.set('sort', params.sort ?? 'id,asc');

  const filters = params.filters ?? {};
  if (filters.rating !== undefined) {
    query.set('rating', String(filters.rating));
  }

  return apiFetch<PageResponse<BrewSession>>(`/api/brew-sessions?${query.toString()}`);
}
```

- [ ] **Step 5: Run test to verify it passes**

Run: `npm test -- src/lib/api/brewSessions.test.ts`
Expected: PASS (2 tests).

- [ ] **Step 6: Commit (from repo root)**

```bash
cd /Users/jvilla/Documents/brewdeck
git add brewdeck-web/src/lib/api/types.ts brewdeck-web/src/lib/api/brewSessions.ts brewdeck-web/src/lib/api/brewSessions.test.ts
git commit -m "feat(web): add brew session types and listBrewSessions API"
```

---

### Task 2: Query key and useBrewSessions hook

**Files:**
- Modify: `brewdeck-web/src/lib/query/keys.ts`
- Create: `brewdeck-web/src/hooks/useBrewSessions.ts`
- Test: `brewdeck-web/src/hooks/useBrewSessions.test.tsx`

**Interfaces:**
- Consumes: `listBrewSessions`, `ListBrewSessionsParams` (Task 1).
- Produces:
  - `keys.brewSessions.list(params: ListBrewSessionsParams)` → stable query key
  - `useBrewSessions(params: ListBrewSessionsParams)` → `UseQueryResult<PageResponse<BrewSession>, Error>`

- [ ] **Step 1: Replace `src/lib/query/keys.ts`**

The current file (from the recipes slice) is:

```ts
import type { ListCoffeesParams } from '@/lib/api/coffees';
import type { ListRecipesParams } from '@/lib/api/recipes';

export const keys = {
  dashboard: {
    summary: ['dashboard', 'summary'],
  },
  coffees: {
    list: (params: ListCoffeesParams) => ['coffees', 'list', params] as const,
  },
  recipes: {
    list: (params: ListRecipesParams) => ['recipes', 'list', params] as const,
  },
} as const;
```

Replace its whole contents with (adds the `brewSessions` group and its import):

```ts
import type { ListBrewSessionsParams } from '@/lib/api/brewSessions';
import type { ListCoffeesParams } from '@/lib/api/coffees';
import type { ListRecipesParams } from '@/lib/api/recipes';

export const keys = {
  dashboard: {
    summary: ['dashboard', 'summary'],
  },
  coffees: {
    list: (params: ListCoffeesParams) => ['coffees', 'list', params] as const,
  },
  recipes: {
    list: (params: ListRecipesParams) => ['recipes', 'list', params] as const,
  },
  brewSessions: {
    list: (params: ListBrewSessionsParams) => ['brew-sessions', 'list', params] as const,
  },
} as const;
```

- [ ] **Step 2: Write the failing test — `src/hooks/useBrewSessions.test.tsx`**

```tsx
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { renderHook, waitFor } from '@testing-library/react';
import type { ReactNode } from 'react';
import { describe, expect, it, vi } from 'vitest';
import { useBrewSessions } from './useBrewSessions';
import * as sessionsApi from '@/lib/api/brewSessions';

function wrapper({ children }: { children: ReactNode }) {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return <QueryClientProvider client={client}>{children}</QueryClientProvider>;
}

describe('useBrewSessions', () => {
  it('returns the paginated brew sessions from the API', async () => {
    vi.spyOn(sessionsApi, 'listBrewSessions').mockResolvedValue({
      content: [
        {
          id: 1, recipeId: 1, recipeName: 'Mezcla AeroPress', brewedAt: '2026-01-01T10:30:00',
          actualGrind: null, actualTemp: 90, actualTime: '2:30', tasteResult: 'Clean',
          rating: 9, adjustmentNotes: null,
        },
      ],
      page: 0, size: 10, totalElements: 1, totalPages: 1, first: true, last: true,
    });

    const { result } = renderHook(() => useBrewSessions({ page: 0, size: 10 }), { wrapper });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data?.content[0].recipeName).toBe('Mezcla AeroPress');
    expect(result.current.data?.totalElements).toBe(1);
  });
});
```

- [ ] **Step 3: Run test to verify it fails**

Run: `npm test -- src/hooks/useBrewSessions.test.tsx`
Expected: FAIL (cannot find `./useBrewSessions`).

- [ ] **Step 4: Create `src/hooks/useBrewSessions.ts`**

```ts
'use client';

import { keepPreviousData, useQuery } from '@tanstack/react-query';
import { listBrewSessions, type ListBrewSessionsParams } from '@/lib/api/brewSessions';
import { keys } from '@/lib/query/keys';

export function useBrewSessions(params: ListBrewSessionsParams) {
  return useQuery({
    queryKey: keys.brewSessions.list(params),
    queryFn: () => listBrewSessions(params),
    placeholderData: keepPreviousData,
  });
}
```

- [ ] **Step 5: Run test to verify it passes**

Run: `npm test -- src/hooks/useBrewSessions.test.tsx`
Expected: PASS.

- [ ] **Step 6: Commit (from repo root)**

```bash
cd /Users/jvilla/Documents/brewdeck
git add brewdeck-web/src/lib/query/keys.ts brewdeck-web/src/hooks/useBrewSessions.ts brewdeck-web/src/hooks/useBrewSessions.test.tsx
git commit -m "feat(web): add brew sessions query key and useBrewSessions hook"
```

---

### Task 3: BrewSessionsTable component

**Files:**
- Create: `brewdeck-web/src/components/brew-sessions/BrewSessionsTable.tsx`
- Test: `brewdeck-web/src/components/brew-sessions/BrewSessionsTable.test.tsx`

**Interfaces:**
- Consumes: `BrewSession` (Task 1).
- Produces: `BrewSessionsTable({ sessions }: { sessions: BrewSession[] })`

- [ ] **Step 1: Write the failing test — `src/components/brew-sessions/BrewSessionsTable.test.tsx`**

```tsx
import { screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { renderWithTheme } from '@/test/renderWithTheme';
import { BrewSessionsTable } from './BrewSessionsTable';
import type { BrewSession } from '@/lib/api/types';

const base: BrewSession = {
  id: 1, recipeId: 1, recipeName: 'Mezcla AeroPress', brewedAt: '2026-01-01T10:30:00',
  actualGrind: 'S3 5.5', actualTemp: 90, actualTime: '2:30', tasteResult: 'Clean',
  rating: 9, adjustmentNotes: null,
};

const empty: BrewSession = {
  ...base, id: 2, recipeName: 'Plain V60', brewedAt: '2026-02-02T08:15:00',
  actualTemp: null, actualTime: null, tasteResult: null, rating: null,
};

describe('BrewSessionsTable', () => {
  it('renders session rows with recipe name, formatted brewedAt, and dashes for nulls', () => {
    renderWithTheme(<BrewSessionsTable sessions={[base, empty]} />);

    expect(screen.getByText('Mezcla AeroPress')).toBeInTheDocument();
    expect(screen.getByText('2026-01-01 10:30')).toBeInTheDocument();
    expect(screen.getByText('9')).toBeInTheDocument();
    expect(screen.getByText('90')).toBeInTheDocument();
    expect(screen.getByText('2:30')).toBeInTheDocument();
    expect(screen.getByText('Clean')).toBeInTheDocument();

    expect(screen.getByText('2026-02-02 08:15')).toBeInTheDocument();
    // 'empty' row has null rating, actualTemp, actualTime, tasteResult → four dashes
    expect(screen.getAllByText('—').length).toBeGreaterThanOrEqual(4);
  });
});
```

- [ ] **Step 2: Run test to verify it fails**

Run: `npm test -- src/components/brew-sessions/BrewSessionsTable.test.tsx`
Expected: FAIL (cannot find `./BrewSessionsTable`).

- [ ] **Step 3: Create `src/components/brew-sessions/BrewSessionsTable.tsx`**

```tsx
'use client';

import Paper from '@mui/material/Paper';
import Table from '@mui/material/Table';
import TableBody from '@mui/material/TableBody';
import TableCell from '@mui/material/TableCell';
import TableContainer from '@mui/material/TableContainer';
import TableHead from '@mui/material/TableHead';
import TableRow from '@mui/material/TableRow';
import type { BrewSession } from '@/lib/api/types';

function orDash(value: string | number | null): string {
  if (value === null) {
    return '—';
  }
  const text = String(value);
  return text.trim() !== '' ? text : '—';
}

function formatDateTime(iso: string): string {
  return iso.replace('T', ' ').slice(0, 16);
}

export function BrewSessionsTable({ sessions }: { sessions: BrewSession[] }) {
  return (
    <TableContainer component={Paper} variant="outlined">
      <Table>
        <TableHead>
          <TableRow>
            <TableCell>Recipe</TableCell>
            <TableCell>Brewed At</TableCell>
            <TableCell>Rating</TableCell>
            <TableCell>Actual Temp</TableCell>
            <TableCell>Actual Time</TableCell>
            <TableCell>Taste</TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          {sessions.map((session) => (
            <TableRow key={session.id}>
              <TableCell>{session.recipeName}</TableCell>
              <TableCell>{formatDateTime(session.brewedAt)}</TableCell>
              <TableCell>{orDash(session.rating)}</TableCell>
              <TableCell>{orDash(session.actualTemp)}</TableCell>
              <TableCell>{orDash(session.actualTime)}</TableCell>
              <TableCell>{orDash(session.tasteResult)}</TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </TableContainer>
  );
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `npm test -- src/components/brew-sessions/BrewSessionsTable.test.tsx`
Expected: PASS.

- [ ] **Step 5: Commit (from repo root)**

```bash
cd /Users/jvilla/Documents/brewdeck
git add brewdeck-web/src/components/brew-sessions/BrewSessionsTable.tsx brewdeck-web/src/components/brew-sessions/BrewSessionsTable.test.tsx
git commit -m "feat(web): add BrewSessionsTable component"
```

---

### Task 4: BrewSessionFilters component

**Files:**
- Create: `brewdeck-web/src/components/brew-sessions/BrewSessionFilters.tsx`
- Test: `brewdeck-web/src/components/brew-sessions/BrewSessionFilters.test.tsx`

**Interfaces:**
- Consumes: `BrewSessionFilters` type (Task 1).
- Produces: `BrewSessionFilters({ value, onChange }: { value: BrewSessionFilters; onChange: (next: BrewSessionFilters) => void })` (component named `BrewSessionFilters`; import the type under an alias to avoid a name clash).

- [ ] **Step 1: Write the failing test — `src/components/brew-sessions/BrewSessionFilters.test.tsx`**

```tsx
import { fireEvent, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { renderWithTheme } from '@/test/renderWithTheme';
import { BrewSessionFilters } from './BrewSessionFilters';

describe('BrewSessionFilters', () => {
  it('calls onChange with a numeric rating when a value is entered', () => {
    const onChange = vi.fn();
    renderWithTheme(<BrewSessionFilters value={{}} onChange={onChange} />);

    fireEvent.change(screen.getByLabelText('Rating'), { target: { value: '7' } });

    expect(onChange).toHaveBeenCalledWith({ rating: 7 });
  });

  it('calls onChange with rating undefined when the field is cleared', () => {
    const onChange = vi.fn();
    renderWithTheme(<BrewSessionFilters value={{ rating: 7 }} onChange={onChange} />);

    fireEvent.change(screen.getByLabelText('Rating'), { target: { value: '' } });

    expect(onChange).toHaveBeenCalledWith({ rating: undefined });
  });
});
```

- [ ] **Step 2: Run test to verify it fails**

Run: `npm test -- src/components/brew-sessions/BrewSessionFilters.test.tsx`
Expected: FAIL (cannot find `./BrewSessionFilters`).

- [ ] **Step 3: Create `src/components/brew-sessions/BrewSessionFilters.tsx`**

```tsx
'use client';

import Box from '@mui/material/Box';
import TextField from '@mui/material/TextField';
import type { ChangeEvent } from 'react';
import type { BrewSessionFilters as Filters } from '@/lib/api/types';

export function BrewSessionFilters({
  value,
  onChange,
}: {
  value: Filters;
  onChange: (next: Filters) => void;
}) {
  const handleRating = (event: ChangeEvent<HTMLInputElement>) => {
    const raw = event.target.value;
    onChange({ ...value, rating: raw === '' ? undefined : Number(raw) });
  };

  return (
    <Box sx={{ display: 'flex', gap: 2, alignItems: 'center', flexWrap: 'wrap', mb: 2 }}>
      <TextField
        label="Rating"
        type="number"
        size="small"
        value={value.rating ?? ''}
        onChange={handleRating}
      />
    </Box>
  );
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `npm test -- src/components/brew-sessions/BrewSessionFilters.test.tsx`
Expected: PASS (2 tests).

- [ ] **Step 5: Commit (from repo root)**

```bash
cd /Users/jvilla/Documents/brewdeck
git add brewdeck-web/src/components/brew-sessions/BrewSessionFilters.tsx brewdeck-web/src/components/brew-sessions/BrewSessionFilters.test.tsx
git commit -m "feat(web): add BrewSessionFilters component"
```

---

### Task 5: BrewSessionsView, route, and nav

**Files:**
- Create: `brewdeck-web/src/components/brew-sessions/BrewSessionsView.tsx`
- Create: `brewdeck-web/src/app/brew-sessions/page.tsx`
- Modify: `brewdeck-web/src/components/layout/AppShell.tsx` (enable Brew Sessions nav)
- Test: `brewdeck-web/src/components/brew-sessions/BrewSessionsView.test.tsx`

**Interfaces:**
- Consumes: `useBrewSessions` (Task 2), `useDebounce` (existing), `BrewSessionFilters` component (Task 4), `BrewSessionsTable` (Task 3), `Spinner`/`ErrorState`/`EmptyState` (existing), `BrewSessionFilters` type (Task 1).
- Produces: `BrewSessionsView()` client component; `/brew-sessions` route.

- [ ] **Step 1: Write the failing test — `src/components/brew-sessions/BrewSessionsView.test.tsx`**

```tsx
import { fireEvent, screen } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { renderWithTheme } from '@/test/renderWithTheme';
import { BrewSessionsView } from './BrewSessionsView';
import * as sessionsHook from '@/hooks/useBrewSessions';
import type { BrewSession, PageResponse } from '@/lib/api/types';

type HookReturn = ReturnType<typeof sessionsHook.useBrewSessions>;

function mockHook(value: Partial<HookReturn>) {
  return vi.spyOn(sessionsHook, 'useBrewSessions').mockReturnValue(value as HookReturn);
}

function page(content: BrewSession[], totalElements: number): PageResponse<BrewSession> {
  return { content, page: 0, size: 10, totalElements, totalPages: 1, first: true, last: true };
}

const session: BrewSession = {
  id: 1, recipeId: 1, recipeName: 'Mezcla AeroPress', brewedAt: '2026-01-01T10:30:00',
  actualGrind: null, actualTemp: 90, actualTime: '2:30', tasteResult: 'Clean',
  rating: 9, adjustmentNotes: null,
};

afterEach(() => vi.restoreAllMocks());

describe('BrewSessionsView', () => {
  it('shows a spinner while loading', () => {
    mockHook({ isLoading: true, isError: false, data: undefined });
    renderWithTheme(<BrewSessionsView />);
    expect(screen.getByRole('status', { name: /loading/i })).toBeInTheDocument();
  });

  it('shows an error state on failure', () => {
    mockHook({ isLoading: false, isError: true, data: undefined, refetch: vi.fn() });
    renderWithTheme(<BrewSessionsView />);
    expect(screen.getByText(/could not load brew sessions/i)).toBeInTheDocument();
  });

  it('shows an empty state when there are no sessions', () => {
    mockHook({ isLoading: false, isError: false, data: page([], 0) });
    renderWithTheme(<BrewSessionsView />);
    expect(screen.getByText(/no brew sessions found/i)).toBeInTheDocument();
  });

  it('renders the table on success', () => {
    mockHook({ isLoading: false, isError: false, data: page([session], 1) });
    renderWithTheme(<BrewSessionsView />);
    expect(screen.getByText('Mezcla AeroPress')).toBeInTheDocument();
  });

  it('resets to page 0 when the filter changes', () => {
    const hookMock = mockHook({ isLoading: false, isError: false, data: page([session], 100) });
    renderWithTheme(<BrewSessionsView />);

    fireEvent.click(screen.getByRole('button', { name: /next page/i }));
    expect(hookMock).toHaveBeenLastCalledWith(expect.objectContaining({ page: 1 }));

    fireEvent.change(screen.getByLabelText('Rating'), { target: { value: '8' } });
    expect(hookMock).toHaveBeenLastCalledWith(expect.objectContaining({ page: 0 }));
  });
});
```

- [ ] **Step 2: Run test to verify it fails**

Run: `npm test -- src/components/brew-sessions/BrewSessionsView.test.tsx`
Expected: FAIL (cannot find `./BrewSessionsView`).

- [ ] **Step 3: Create `src/components/brew-sessions/BrewSessionsView.tsx`**

```tsx
'use client';

import TablePagination from '@mui/material/TablePagination';
import Typography from '@mui/material/Typography';
import { useState, type ReactNode } from 'react';
import { useBrewSessions } from '@/hooks/useBrewSessions';
import { useDebounce } from '@/hooks/useDebounce';
import { Spinner } from '@/components/ui/Spinner';
import { ErrorState } from '@/components/ui/ErrorState';
import { EmptyState } from '@/components/ui/EmptyState';
import { BrewSessionFilters } from './BrewSessionFilters';
import { BrewSessionsTable } from './BrewSessionsTable';
import type { BrewSessionFilters as Filters } from '@/lib/api/types';

export function BrewSessionsView() {
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(10);
  const [filters, setFilters] = useState<Filters>({});
  const debouncedFilters = useDebounce(filters, 300);

  const { data, isLoading, isError, refetch } = useBrewSessions({
    page,
    size,
    filters: debouncedFilters,
  });

  const handleFiltersChange = (next: Filters) => {
    setPage(0);
    setFilters(next);
  };

  let body: ReactNode;
  if (isLoading && !data) {
    body = <Spinner />;
  } else if (isError || !data) {
    body = <ErrorState message="Could not load brew sessions." onRetry={() => refetch()} />;
  } else if (data.content.length === 0) {
    body = <EmptyState message="No brew sessions found." />;
  } else {
    body = (
      <>
        <BrewSessionsTable sessions={data.content} />
        <TablePagination
          component="div"
          count={data.totalElements}
          page={page}
          rowsPerPage={size}
          rowsPerPageOptions={[10, 20, 50]}
          onPageChange={(_event, newPage) => setPage(newPage)}
          onRowsPerPageChange={(event) => {
            setSize(parseInt(event.target.value, 10));
            setPage(0);
          }}
        />
      </>
    );
  }

  return (
    <>
      <Typography variant="h5" component="h1" gutterBottom>
        Brew Sessions
      </Typography>
      <BrewSessionFilters value={filters} onChange={handleFiltersChange} />
      {body}
    </>
  );
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `npm test -- src/components/brew-sessions/BrewSessionsView.test.tsx`
Expected: PASS (5 tests).

- [ ] **Step 5: Create `src/app/brew-sessions/page.tsx`**

```tsx
import { BrewSessionsView } from '@/components/brew-sessions/BrewSessionsView';

export default function BrewSessionsPage() {
  return <BrewSessionsView />;
}
```

- [ ] **Step 6: Enable the Brew Sessions nav item in `src/components/layout/AppShell.tsx`**

Find the `NAV` array entry for Brew Sessions, currently:
`{ label: 'Brew Sessions', href: '/brew-sessions', enabled: false },`
Change it to:
`{ label: 'Brew Sessions', href: '/brew-sessions', enabled: true },`
Leave the other entries unchanged.

- [ ] **Step 7: Run the full suite and build**

Run (from `brewdeck-web/`): `npm test && npm run build`
Expected: all tests pass; build succeeds; `/brew-sessions` route present.

- [ ] **Step 8: Commit (from repo root)**

```bash
cd /Users/jvilla/Documents/brewdeck
git add brewdeck-web/src/components/brew-sessions/BrewSessionsView.tsx brewdeck-web/src/components/brew-sessions/BrewSessionsView.test.tsx brewdeck-web/src/app/brew-sessions/page.tsx brewdeck-web/src/components/layout/AppShell.tsx
git commit -m "feat(web): add brew sessions list view, route and nav entry"
```

---

### Task 6: Manual verification and pull request

**Files:** none (verification + PR).

- [ ] **Step 1: Start the backend (from repo root)**

```bash
cd /Users/jvilla/Documents/brewdeck
docker compose up -d
cd brewdeck-api && sh ./mvnw spring-boot:run
```

Wait for "Started BrewdeckApiApplication".

- [ ] **Step 2: Start the frontend (new shell, from `brewdeck-web/`)**

```bash
cd /Users/jvilla/Documents/brewdeck/brewdeck-web
npm run dev
```

- [ ] **Step 3: Verify in the browser**

Open `http://localhost:3000/brew-sessions`. Expected: the Brew Sessions nav item is active; a table renders (create a coffee, method, recipe, and brew session via the API/Postman if the DB is empty); pagination controls show; typing a number in Rating filters after a brief debounce; no CORS error in the console.

- [ ] **Step 4: Verify the empty and error states**

With Rating = `1` (assuming no rating-1 sessions), expect "No brew sessions found." Stop the backend and reload; expect the error alert with Retry.

- [ ] **Step 5: Push the branch and open the PR (from repo root)**

```bash
cd /Users/jvilla/Documents/brewdeck
git push -u origin feature/brew-sessions-list
```

Then open a PR from `feature/brew-sessions-list` into `feature/recipes-list` (retarget up the chain as parents merge), titled `feat(web): brew sessions list — paginated, rating-filterable table`, referencing `docs/superpowers/specs/2026-07-04-brew-sessions-list-design.md`. If `gh` is available, use `gh pr create --base feature/recipes-list --head feature/brew-sessions-list --title "..." --body-file <path>`; otherwise open the compare URL.

---

## Notes for the implementer

- Run test/build commands from `brewdeck-web/`; run `git` from the repo root.
- `keys.ts` imports `ListBrewSessionsParams` (type-only) from `@/lib/api/brewSessions`; no runtime cycle (brewSessions.ts does not import keys.ts).
- In `BrewSessionsView`, `setPage(0)` is synchronous, so a filter change resets the page immediately even though the filter value is debounced before reaching the query — this is what the page-reset test asserts.
- `formatDateTime` is intentionally a plain string operation (`iso.replace('T', ' ').slice(0, 16)`), not `toLocaleString`, so the rendered value is deterministic across environments and the table test is stable.
- This branch is stacked on `feature/recipes-list`; it edits the shared `keys.ts`, `types.ts`, and `AppShell.tsx` on top of the recipes versions.
