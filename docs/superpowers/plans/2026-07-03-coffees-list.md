# Coffees List Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a paginated, filterable Coffees list page at `/coffees` consuming `GET /api/coffees`.

**Architecture:** Reuse the kickoff foundation (`apiFetch`, TanStack Query, MUI shell, UI primitives). A new `listCoffees` API builds the query string; `useCoffees` (with `keepPreviousData`) feeds a client `CoffeesView` that owns page/size/filter state and renders an MUI Table + TablePagination + a filter bar. List only.

**Tech Stack:** Next.js App Router, TypeScript strict, MUI 9, TanStack Query, Vitest + React Testing Library.

## Global Constraints

- App under `brewdeck-web/`, source `brewdeck-web/src/`, alias `@/*` → `src/*`.
- TypeScript strict — no `any` in committed code.
- No `fetch` outside `src/lib/api/client.ts`; new API modules call `apiFetch`.
- Backend `GET /api/coffees` returns `PageResponse<CoffeeResponse>`; pagination is 0-based; blank filter values must be omitted from the query string.
- Filters (exact names): `name` (contains), `origin`, `roastLevel`, `process` (exact match).
- Client components using hooks/state/MUI must start with `'use client'`; route `page.tsx` is a plain server component.
- Null table cell values render as an em dash "—".
- Run npm commands from `brewdeck-web/`; run `git` from repo root `/Users/jvilla/Documents/brewdeck`.
- Conventional Commits; commit at the end of each task.

---

### Task 1: Coffee types and listCoffees API

**Files:**
- Modify: `brewdeck-web/src/lib/api/types.ts` (append)
- Create: `brewdeck-web/src/lib/api/coffees.ts`
- Test: `brewdeck-web/src/lib/api/coffees.test.ts`

**Interfaces:**
- Produces:
  - `type Coffee` (all `CoffeeResponse` fields, nullable where the DTO is nullable)
  - `type CoffeeFilters = { name?: string; origin?: string; roastLevel?: string; process?: string }`
  - `type ListCoffeesParams = { page: number; size: number; sort?: string; filters?: CoffeeFilters }`
  - `listCoffees(params: ListCoffeesParams): Promise<PageResponse<Coffee>>`

- [ ] **Step 1: Append types to `src/lib/api/types.ts`**

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

- [ ] **Step 2: Write the failing test — `src/lib/api/coffees.test.ts`**

```ts
import { afterEach, describe, expect, it, vi } from 'vitest';
import { listCoffees } from './coffees';

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

describe('listCoffees', () => {
  it('includes page, size and default sort, and omits blank filters', async () => {
    const fetchMock = stubFetch();

    await listCoffees({ page: 2, size: 20, filters: { name: '', origin: 'Veracruz' } });

    const url = fetchMock.mock.calls[0][0] as string;
    expect(url).toContain('/api/coffees?');
    expect(url).toContain('page=2');
    expect(url).toContain('size=20');
    expect(url).toContain('sort=id%2Casc');
    expect(url).toContain('origin=Veracruz');
    expect(url).not.toContain('name=');
  });

  it('includes all non-blank filters and a custom sort', async () => {
    const fetchMock = stubFetch();

    await listCoffees({
      page: 0,
      size: 10,
      sort: 'name,asc',
      filters: { name: 'Blend', origin: 'Oaxaca', roastLevel: 'Medio', process: 'Lavado' },
    });

    const url = fetchMock.mock.calls[0][0] as string;
    expect(url).toContain('sort=name%2Casc');
    expect(url).toContain('name=Blend');
    expect(url).toContain('origin=Oaxaca');
    expect(url).toContain('roastLevel=Medio');
    expect(url).toContain('process=Lavado');
  });
});
```

- [ ] **Step 3: Run test to verify it fails**

Run: `npm test -- src/lib/api/coffees.test.ts`
Expected: FAIL (cannot find `./coffees`).

- [ ] **Step 4: Create `src/lib/api/coffees.ts`**

```ts
import { apiFetch } from './client';
import type { Coffee, CoffeeFilters, PageResponse } from './types';

export type ListCoffeesParams = {
  page: number;
  size: number;
  sort?: string;
  filters?: CoffeeFilters;
};

export function listCoffees(params: ListCoffeesParams): Promise<PageResponse<Coffee>> {
  const query = new URLSearchParams();
  query.set('page', String(params.page));
  query.set('size', String(params.size));
  query.set('sort', params.sort ?? 'id,asc');

  const filters = params.filters ?? {};
  (Object.keys(filters) as Array<keyof CoffeeFilters>).forEach((key) => {
    const value = filters[key]?.trim();
    if (value) {
      query.set(key, value);
    }
  });

  return apiFetch<PageResponse<Coffee>>(`/api/coffees?${query.toString()}`);
}
```

- [ ] **Step 5: Run test to verify it passes**

Run: `npm test -- src/lib/api/coffees.test.ts`
Expected: PASS (2 tests).

- [ ] **Step 6: Commit (from repo root)**

```bash
cd /Users/jvilla/Documents/brewdeck
git add brewdeck-web/src/lib/api/types.ts brewdeck-web/src/lib/api/coffees.ts brewdeck-web/src/lib/api/coffees.test.ts
git commit -m "feat(web): add coffee types and listCoffees API"
```

---

### Task 2: useDebounce hook

**Files:**
- Create: `brewdeck-web/src/hooks/useDebounce.ts`
- Test: `brewdeck-web/src/hooks/useDebounce.test.ts`

**Interfaces:**
- Produces: `useDebounce<T>(value: T, delayMs: number): T`

- [ ] **Step 1: Write the failing test — `src/hooks/useDebounce.test.ts`**

```ts
import { act, renderHook } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { useDebounce } from './useDebounce';

beforeEach(() => vi.useFakeTimers());
afterEach(() => vi.useRealTimers());

describe('useDebounce', () => {
  it('returns the initial value immediately', () => {
    const { result } = renderHook(() => useDebounce('a', 300));
    expect(result.current).toBe('a');
  });

  it('updates only after the delay elapses', () => {
    const { result, rerender } = renderHook(({ v }) => useDebounce(v, 300), {
      initialProps: { v: 'a' },
    });

    rerender({ v: 'b' });
    expect(result.current).toBe('a');

    act(() => {
      vi.advanceTimersByTime(300);
    });
    expect(result.current).toBe('b');
  });
});
```

- [ ] **Step 2: Run test to verify it fails**

Run: `npm test -- src/hooks/useDebounce.test.ts`
Expected: FAIL (cannot find `./useDebounce`).

- [ ] **Step 3: Create `src/hooks/useDebounce.ts`**

```ts
'use client';

import { useEffect, useState } from 'react';

export function useDebounce<T>(value: T, delayMs: number): T {
  const [debounced, setDebounced] = useState(value);

  useEffect(() => {
    const id = setTimeout(() => setDebounced(value), delayMs);
    return () => clearTimeout(id);
  }, [value, delayMs]);

  return debounced;
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `npm test -- src/hooks/useDebounce.test.ts`
Expected: PASS (2 tests).

- [ ] **Step 5: Commit (from repo root)**

```bash
cd /Users/jvilla/Documents/brewdeck
git add brewdeck-web/src/hooks/useDebounce.ts brewdeck-web/src/hooks/useDebounce.test.ts
git commit -m "feat(web): add useDebounce hook"
```

---

### Task 3: Query keys and useCoffees hook

**Files:**
- Modify: `brewdeck-web/src/lib/query/keys.ts`
- Create: `brewdeck-web/src/hooks/useCoffees.ts`
- Test: `brewdeck-web/src/hooks/useCoffees.test.tsx`

**Interfaces:**
- Consumes: `listCoffees`, `ListCoffeesParams` (Task 1).
- Produces:
  - `keys.coffees.list(params: ListCoffeesParams)` → stable query key
  - `useCoffees(params: ListCoffeesParams)` → `UseQueryResult<PageResponse<Coffee>, Error>`

- [ ] **Step 1: Replace `src/lib/query/keys.ts`**

The current file is `export const keys = { dashboard: { summary: ['dashboard', 'summary'] } } as const;`. Replace its whole contents with:

```ts
import type { ListCoffeesParams } from '@/lib/api/coffees';

export const keys = {
  dashboard: {
    summary: ['dashboard', 'summary'],
  },
  coffees: {
    list: (params: ListCoffeesParams) => ['coffees', 'list', params] as const,
  },
} as const;
```

- [ ] **Step 2: Write the failing test — `src/hooks/useCoffees.test.tsx`**

```tsx
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { renderHook, waitFor } from '@testing-library/react';
import type { ReactNode } from 'react';
import { describe, expect, it, vi } from 'vitest';
import { useCoffees } from './useCoffees';
import * as coffeesApi from '@/lib/api/coffees';

function wrapper({ children }: { children: ReactNode }) {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return <QueryClientProvider client={client}>{children}</QueryClientProvider>;
}

describe('useCoffees', () => {
  it('returns the paginated coffees from the API', async () => {
    vi.spyOn(coffeesApi, 'listCoffees').mockResolvedValue({
      content: [
        {
          id: 1, name: 'Mezcla', brand: null, origin: 'Veracruz', region: null, farm: null,
          producer: null, variety: null, process: null, roastLevel: null, notesPrimary: null,
          notesSecondary: null, acidity: null, body: null, sweetness: null, bitterness: null,
          description: null, createdAt: '2026-01-01T00:00:00', updatedAt: null,
        },
      ],
      page: 0, size: 10, totalElements: 1, totalPages: 1, first: true, last: true,
    });

    const { result } = renderHook(() => useCoffees({ page: 0, size: 10 }), { wrapper });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data?.content[0].name).toBe('Mezcla');
    expect(result.current.data?.totalElements).toBe(1);
  });
});
```

- [ ] **Step 3: Run test to verify it fails**

Run: `npm test -- src/hooks/useCoffees.test.tsx`
Expected: FAIL (cannot find `./useCoffees`).

- [ ] **Step 4: Create `src/hooks/useCoffees.ts`**

```ts
'use client';

import { keepPreviousData, useQuery } from '@tanstack/react-query';
import { listCoffees, type ListCoffeesParams } from '@/lib/api/coffees';
import { keys } from '@/lib/query/keys';

export function useCoffees(params: ListCoffeesParams) {
  return useQuery({
    queryKey: keys.coffees.list(params),
    queryFn: () => listCoffees(params),
    placeholderData: keepPreviousData,
  });
}
```

- [ ] **Step 5: Run test to verify it passes**

Run: `npm test -- src/hooks/useCoffees.test.tsx`
Expected: PASS.

- [ ] **Step 6: Commit (from repo root)**

```bash
cd /Users/jvilla/Documents/brewdeck
git add brewdeck-web/src/lib/query/keys.ts brewdeck-web/src/hooks/useCoffees.ts brewdeck-web/src/hooks/useCoffees.test.tsx
git commit -m "feat(web): add coffees query key and useCoffees hook"
```

---

### Task 4: CoffeesTable component

**Files:**
- Create: `brewdeck-web/src/components/coffees/CoffeesTable.tsx`
- Test: `brewdeck-web/src/components/coffees/CoffeesTable.test.tsx`

**Interfaces:**
- Consumes: `Coffee` (Task 1).
- Produces: `CoffeesTable({ coffees }: { coffees: Coffee[] })`

- [ ] **Step 1: Write the failing test — `src/components/coffees/CoffeesTable.test.tsx`**

```tsx
import { screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { renderWithTheme } from '@/test/renderWithTheme';
import { CoffeesTable } from './CoffeesTable';
import type { Coffee } from '@/lib/api/types';

const coffee: Coffee = {
  id: 1, name: 'Mezcla Veracruz', brand: 'Local', origin: 'Veracruz', region: null, farm: null,
  producer: null, variety: null, process: 'Lavado', roastLevel: null, notesPrimary: null,
  notesSecondary: null, acidity: null, body: null, sweetness: null, bitterness: null,
  description: null, createdAt: '2026-01-01T00:00:00', updatedAt: null,
};

describe('CoffeesTable', () => {
  it('renders a row with the coffee fields and an em dash for null roast', () => {
    renderWithTheme(<CoffeesTable coffees={[coffee]} />);
    expect(screen.getByText('Mezcla Veracruz')).toBeInTheDocument();
    expect(screen.getByText('Local')).toBeInTheDocument();
    expect(screen.getByText('Veracruz')).toBeInTheDocument();
    expect(screen.getByText('Lavado')).toBeInTheDocument();
    expect(screen.getByText('—')).toBeInTheDocument();
  });
});
```

- [ ] **Step 2: Run test to verify it fails**

Run: `npm test -- src/components/coffees/CoffeesTable.test.tsx`
Expected: FAIL (cannot find `./CoffeesTable`).

- [ ] **Step 3: Create `src/components/coffees/CoffeesTable.tsx`**

```tsx
'use client';

import Paper from '@mui/material/Paper';
import Table from '@mui/material/Table';
import TableBody from '@mui/material/TableBody';
import TableCell from '@mui/material/TableCell';
import TableContainer from '@mui/material/TableContainer';
import TableHead from '@mui/material/TableHead';
import TableRow from '@mui/material/TableRow';
import type { Coffee } from '@/lib/api/types';

function orDash(value: string | null): string {
  return value && value.trim() !== '' ? value : '—';
}

export function CoffeesTable({ coffees }: { coffees: Coffee[] }) {
  return (
    <TableContainer component={Paper} variant="outlined">
      <Table>
        <TableHead>
          <TableRow>
            <TableCell>Name</TableCell>
            <TableCell>Brand</TableCell>
            <TableCell>Origin</TableCell>
            <TableCell>Roast</TableCell>
            <TableCell>Process</TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          {coffees.map((coffee) => (
            <TableRow key={coffee.id}>
              <TableCell>{coffee.name}</TableCell>
              <TableCell>{orDash(coffee.brand)}</TableCell>
              <TableCell>{orDash(coffee.origin)}</TableCell>
              <TableCell>{orDash(coffee.roastLevel)}</TableCell>
              <TableCell>{orDash(coffee.process)}</TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </TableContainer>
  );
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `npm test -- src/components/coffees/CoffeesTable.test.tsx`
Expected: PASS.

- [ ] **Step 5: Commit (from repo root)**

```bash
cd /Users/jvilla/Documents/brewdeck
git add brewdeck-web/src/components/coffees/CoffeesTable.tsx brewdeck-web/src/components/coffees/CoffeesTable.test.tsx
git commit -m "feat(web): add CoffeesTable component"
```

---

### Task 5: CoffeeFilters component

**Files:**
- Create: `brewdeck-web/src/components/coffees/CoffeeFilters.tsx`
- Test: `brewdeck-web/src/components/coffees/CoffeeFilters.test.tsx`

**Interfaces:**
- Consumes: `CoffeeFilters` type (Task 1).
- Produces: `CoffeeFilters({ value, onChange }: { value: CoffeeFilters; onChange: (next: CoffeeFilters) => void })` (component named `CoffeeFilters`; import the type under an alias to avoid a name clash).

- [ ] **Step 1: Write the failing test — `src/components/coffees/CoffeeFilters.test.tsx`**

```tsx
import { fireEvent, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { renderWithTheme } from '@/test/renderWithTheme';
import { CoffeeFilters } from './CoffeeFilters';

describe('CoffeeFilters', () => {
  it('calls onChange with the merged filters when a field changes', () => {
    const onChange = vi.fn();
    renderWithTheme(<CoffeeFilters value={{ origin: 'Veracruz' }} onChange={onChange} />);

    fireEvent.change(screen.getByLabelText('Name'), { target: { value: 'Blend' } });

    expect(onChange).toHaveBeenCalledWith({ origin: 'Veracruz', name: 'Blend' });
  });
});
```

- [ ] **Step 2: Run test to verify it fails**

Run: `npm test -- src/components/coffees/CoffeeFilters.test.tsx`
Expected: FAIL (cannot find `./CoffeeFilters`).

- [ ] **Step 3: Create `src/components/coffees/CoffeeFilters.tsx`**

```tsx
'use client';

import Box from '@mui/material/Box';
import TextField from '@mui/material/TextField';
import type { ChangeEvent } from 'react';
import type { CoffeeFilters as Filters } from '@/lib/api/types';

export function CoffeeFilters({
  value,
  onChange,
}: {
  value: Filters;
  onChange: (next: Filters) => void;
}) {
  const handle = (key: keyof Filters) => (event: ChangeEvent<HTMLInputElement>) => {
    onChange({ ...value, [key]: event.target.value });
  };

  return (
    <Box sx={{ display: 'flex', gap: 2, flexWrap: 'wrap', mb: 2 }}>
      <TextField label="Name" size="small" value={value.name ?? ''} onChange={handle('name')} />
      <TextField label="Origin" size="small" value={value.origin ?? ''} onChange={handle('origin')} />
      <TextField
        label="Roast Level"
        size="small"
        value={value.roastLevel ?? ''}
        onChange={handle('roastLevel')}
      />
      <TextField label="Process" size="small" value={value.process ?? ''} onChange={handle('process')} />
    </Box>
  );
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `npm test -- src/components/coffees/CoffeeFilters.test.tsx`
Expected: PASS.

- [ ] **Step 5: Commit (from repo root)**

```bash
cd /Users/jvilla/Documents/brewdeck
git add brewdeck-web/src/components/coffees/CoffeeFilters.tsx brewdeck-web/src/components/coffees/CoffeeFilters.test.tsx
git commit -m "feat(web): add CoffeeFilters component"
```

---

### Task 6: CoffeesView, route, and nav

**Files:**
- Create: `brewdeck-web/src/components/coffees/CoffeesView.tsx`
- Create: `brewdeck-web/src/app/coffees/page.tsx`
- Modify: `brewdeck-web/src/components/layout/AppShell.tsx` (enable Coffees nav)
- Test: `brewdeck-web/src/components/coffees/CoffeesView.test.tsx`

**Interfaces:**
- Consumes: `useCoffees` (Task 3), `useDebounce` (Task 2), `CoffeeFilters` component (Task 5), `CoffeesTable` (Task 4), `Spinner`/`ErrorState`/`EmptyState` (kickoff), `CoffeeFilters` type (Task 1).
- Produces: `CoffeesView()` client component; `/coffees` route.

- [ ] **Step 1: Write the failing test — `src/components/coffees/CoffeesView.test.tsx`**

```tsx
import { fireEvent, screen } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { renderWithTheme } from '@/test/renderWithTheme';
import { CoffeesView } from './CoffeesView';
import * as coffeesHook from '@/hooks/useCoffees';
import type { Coffee, PageResponse } from '@/lib/api/types';

type HookReturn = ReturnType<typeof coffeesHook.useCoffees>;

function mockHook(value: Partial<HookReturn>) {
  return vi.spyOn(coffeesHook, 'useCoffees').mockReturnValue(value as HookReturn);
}

function page(content: Coffee[], totalElements: number): PageResponse<Coffee> {
  return { content, page: 0, size: 10, totalElements, totalPages: 1, first: true, last: true };
}

const coffee: Coffee = {
  id: 1, name: 'Mezcla Veracruz', brand: null, origin: null, region: null, farm: null,
  producer: null, variety: null, process: null, roastLevel: null, notesPrimary: null,
  notesSecondary: null, acidity: null, body: null, sweetness: null, bitterness: null,
  description: null, createdAt: '2026-01-01T00:00:00', updatedAt: null,
};

afterEach(() => vi.restoreAllMocks());

describe('CoffeesView', () => {
  it('shows a spinner while loading', () => {
    mockHook({ isLoading: true, isError: false, data: undefined });
    renderWithTheme(<CoffeesView />);
    expect(screen.getByRole('status', { name: /loading/i })).toBeInTheDocument();
  });

  it('shows an error state on failure', () => {
    mockHook({ isLoading: false, isError: true, data: undefined, refetch: vi.fn() });
    renderWithTheme(<CoffeesView />);
    expect(screen.getByText(/could not load coffees/i)).toBeInTheDocument();
  });

  it('shows an empty state when there are no coffees', () => {
    mockHook({ isLoading: false, isError: false, data: page([], 0) });
    renderWithTheme(<CoffeesView />);
    expect(screen.getByText(/no coffees found/i)).toBeInTheDocument();
  });

  it('renders the table on success', () => {
    mockHook({ isLoading: false, isError: false, data: page([coffee], 1) });
    renderWithTheme(<CoffeesView />);
    expect(screen.getByText('Mezcla Veracruz')).toBeInTheDocument();
  });

  it('resets to page 0 when a filter changes', () => {
    const hookMock = mockHook({ isLoading: false, isError: false, data: page([coffee], 100) });
    renderWithTheme(<CoffeesView />);

    fireEvent.click(screen.getByRole('button', { name: /next page/i }));
    expect(hookMock).toHaveBeenLastCalledWith(expect.objectContaining({ page: 1 }));

    fireEvent.change(screen.getByLabelText('Name'), { target: { value: 'Blend' } });
    expect(hookMock).toHaveBeenLastCalledWith(expect.objectContaining({ page: 0 }));
  });
});
```

- [ ] **Step 2: Run test to verify it fails**

Run: `npm test -- src/components/coffees/CoffeesView.test.tsx`
Expected: FAIL (cannot find `./CoffeesView`).

- [ ] **Step 3: Create `src/components/coffees/CoffeesView.tsx`**

```tsx
'use client';

import TablePagination from '@mui/material/TablePagination';
import Typography from '@mui/material/Typography';
import { useState } from 'react';
import { useCoffees } from '@/hooks/useCoffees';
import { useDebounce } from '@/hooks/useDebounce';
import { Spinner } from '@/components/ui/Spinner';
import { ErrorState } from '@/components/ui/ErrorState';
import { EmptyState } from '@/components/ui/EmptyState';
import { CoffeeFilters } from './CoffeeFilters';
import { CoffeesTable } from './CoffeesTable';
import type { CoffeeFilters as Filters } from '@/lib/api/types';

export function CoffeesView() {
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(10);
  const [filters, setFilters] = useState<Filters>({});
  const debouncedFilters = useDebounce(filters, 300);

  const { data, isLoading, isError, refetch } = useCoffees({
    page,
    size,
    filters: debouncedFilters,
  });

  const handleFiltersChange = (next: Filters) => {
    setPage(0);
    setFilters(next);
  };

  let body;
  if (isLoading && !data) {
    body = <Spinner />;
  } else if (isError || !data) {
    body = <ErrorState message="Could not load coffees." onRetry={() => refetch()} />;
  } else if (data.content.length === 0) {
    body = <EmptyState message="No coffees found." />;
  } else {
    body = (
      <>
        <CoffeesTable coffees={data.content} />
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
        Coffees
      </Typography>
      <CoffeeFilters value={filters} onChange={handleFiltersChange} />
      {body}
    </>
  );
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `npm test -- src/components/coffees/CoffeesView.test.tsx`
Expected: PASS (5 tests).

- [ ] **Step 5: Create `src/app/coffees/page.tsx`**

```tsx
import { CoffeesView } from '@/components/coffees/CoffeesView';

export default function CoffeesPage() {
  return <CoffeesView />;
}
```

- [ ] **Step 6: Enable the Coffees nav item in `src/components/layout/AppShell.tsx`**

Find the `NAV` array entry for Coffees, currently:
`{ label: 'Coffees', href: '/coffees', enabled: false },`
Change it to:
`{ label: 'Coffees', href: '/coffees', enabled: true },`
Leave the other entries unchanged.

- [ ] **Step 7: Run the full suite and build**

Run (from `brewdeck-web/`): `npm test && npm run build`
Expected: all tests pass; build succeeds; `/coffees` route present.

- [ ] **Step 8: Commit (from repo root)**

```bash
cd /Users/jvilla/Documents/brewdeck
git add brewdeck-web/src/components/coffees/CoffeesView.tsx brewdeck-web/src/components/coffees/CoffeesView.test.tsx brewdeck-web/src/app/coffees/page.tsx brewdeck-web/src/components/layout/AppShell.tsx
git commit -m "feat(web): add coffees list view, route and nav entry"
```

---

### Task 7: Manual verification and pull request

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

Open `http://localhost:3000/coffees`. Expected: the Coffees nav item is active; a table renders (seed data may make it empty — create a coffee via the API or Postman if needed to see rows); pagination controls show at the bottom; typing in a filter updates the results after a brief debounce; no CORS error in the console.

- [ ] **Step 4: Verify the empty and error states**

With a filter that matches nothing (e.g. Name = `zzzzz`), expect "No coffees found." Stop the backend and reload; expect the error alert with Retry.

- [ ] **Step 5: Push the branch and open the PR (from repo root)**

```bash
cd /Users/jvilla/Documents/brewdeck
git push -u origin feature/coffees-list
```

Then open a PR from `feature/coffees-list` into `feature/frontend-kickoff` (or into `develop` if the kickoff has merged by then), titled `feat(web): coffees list — paginated, filterable table`, referencing `docs/superpowers/specs/2026-07-03-coffees-list-design.md`. If `gh` is available: `gh pr create --base feature/frontend-kickoff --head feature/coffees-list --title "feat(web): coffees list — paginated, filterable table" --body-file <path>`; otherwise open the compare URL in the browser.

---

## Notes for the implementer

- Run test/build commands from `brewdeck-web/`; run `git` from the repo root.
- The `keys.ts` change imports a type from `@/lib/api/coffees`; this is a type-only import and does not create a runtime cycle (coffees.ts does not import keys.ts).
- In `CoffeesView`, `setPage(0)` is synchronous, so a filter change resets the page immediately even though the filter value itself is debounced before reaching the query — this is what the page-reset test asserts.
- The Coffees list depends on the kickoff foundation; this branch is based on `feature/frontend-kickoff`.
