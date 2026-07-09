# Recipes List Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a paginated, filterable Recipes list page at `/recipes` consuming `GET /api/recipes`.

**Architecture:** Mirror the coffees slice. `listRecipes` builds the query string; `useRecipes` (with `keepPreviousData`) feeds a client `RecipesView` that owns page/size/filter state and renders an MUI Table + TablePagination + a filter bar (name + "Favorites only"). Reuses the existing `useDebounce` and UI primitives. List only.

**Tech Stack:** Next.js App Router, TypeScript strict, MUI 9, TanStack Query, Vitest + React Testing Library.

## Global Constraints

- App under `brewdeck-web/`, source `brewdeck-web/src/`, alias `@/*` → `src/*`.
- TypeScript strict — no `any` in committed code.
- No `fetch` outside `src/lib/api/client.ts`; new API modules call `apiFetch`.
- Backend `GET /api/recipes` returns `PageResponse<RecipeResponse>`; pagination 0-based; blank filter values omitted from the query string; `favorite=true` sent only when filtering to favorites.
- Client components using hooks/state/MUI start with `'use client'`; route `page.tsx` is a plain server component.
- Null table cell values render as an em dash "—"; the Favorite cell renders "★" when true, "—" when false.
- Run npm commands from `brewdeck-web/`; run `git` from repo root `/Users/jvilla/Documents/brewdeck`.
- Conventional Commits; commit at the end of each task.

---

### Task 1: Recipe types and listRecipes API

**Files:**
- Modify: `brewdeck-web/src/lib/api/types.ts` (append)
- Create: `brewdeck-web/src/lib/api/recipes.ts`
- Test: `brewdeck-web/src/lib/api/recipes.test.ts`

**Interfaces:**
- Produces:
  - `type Recipe` (all `RecipeResponse` fields, nullable where the DTO is)
  - `type RecipeFilters = { name?: string; favorite?: boolean }`
  - `type ListRecipesParams = { page: number; size: number; sort?: string; filters?: RecipeFilters }`
  - `listRecipes(params: ListRecipesParams): Promise<PageResponse<Recipe>>`

- [ ] **Step 1: Append types to `src/lib/api/types.ts`**

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

- [ ] **Step 2: Write the failing test — `src/lib/api/recipes.test.ts`**

```ts
import { afterEach, describe, expect, it, vi } from 'vitest';
import { listRecipes } from './recipes';

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

describe('listRecipes', () => {
  it('includes page/size/default sort and omits a blank name and unset favorite', async () => {
    const fetchMock = stubFetch();

    await listRecipes({ page: 1, size: 20, filters: { name: '  ' } });

    const url = fetchMock.mock.calls[0][0] as string;
    expect(url).toContain('/api/recipes?');
    expect(url).toContain('page=1');
    expect(url).toContain('size=20');
    expect(url).toContain('sort=id%2Casc');
    expect(url).not.toContain('name=');
    expect(url).not.toContain('favorite=');
  });

  it('includes a non-blank name, favorite=true, and a custom sort', async () => {
    const fetchMock = stubFetch();

    await listRecipes({ page: 0, size: 10, sort: 'name,asc', filters: { name: 'AeroPress', favorite: true } });

    const url = fetchMock.mock.calls[0][0] as string;
    expect(url).toContain('sort=name%2Casc');
    expect(url).toContain('name=AeroPress');
    expect(url).toContain('favorite=true');
  });

  it('omits favorite when it is false', async () => {
    const fetchMock = stubFetch();

    await listRecipes({ page: 0, size: 10, filters: { favorite: false } });

    const url = fetchMock.mock.calls[0][0] as string;
    expect(url).not.toContain('favorite=');
  });
});
```

- [ ] **Step 3: Run test to verify it fails**

Run: `npm test -- src/lib/api/recipes.test.ts`
Expected: FAIL (cannot find `./recipes`).

- [ ] **Step 4: Create `src/lib/api/recipes.ts`**

```ts
import { apiFetch } from './client';
import type { PageResponse, Recipe, RecipeFilters } from './types';

export type ListRecipesParams = {
  page: number;
  size: number;
  sort?: string;
  filters?: RecipeFilters;
};

export function listRecipes(params: ListRecipesParams): Promise<PageResponse<Recipe>> {
  const query = new URLSearchParams();
  query.set('page', String(params.page));
  query.set('size', String(params.size));
  query.set('sort', params.sort ?? 'id,asc');

  const filters = params.filters ?? {};
  const name = filters.name?.trim();
  if (name) {
    query.set('name', name);
  }
  if (filters.favorite === true) {
    query.set('favorite', 'true');
  }

  return apiFetch<PageResponse<Recipe>>(`/api/recipes?${query.toString()}`);
}
```

- [ ] **Step 5: Run test to verify it passes**

Run: `npm test -- src/lib/api/recipes.test.ts`
Expected: PASS (3 tests).

- [ ] **Step 6: Commit (from repo root)**

```bash
cd /Users/jvilla/Documents/brewdeck
git add brewdeck-web/src/lib/api/types.ts brewdeck-web/src/lib/api/recipes.ts brewdeck-web/src/lib/api/recipes.test.ts
git commit -m "feat(web): add recipe types and listRecipes API"
```

---

### Task 2: Query key and useRecipes hook

**Files:**
- Modify: `brewdeck-web/src/lib/query/keys.ts`
- Create: `brewdeck-web/src/hooks/useRecipes.ts`
- Test: `brewdeck-web/src/hooks/useRecipes.test.tsx`

**Interfaces:**
- Consumes: `listRecipes`, `ListRecipesParams` (Task 1).
- Produces:
  - `keys.recipes.list(params: ListRecipesParams)` → stable query key
  - `useRecipes(params: ListRecipesParams)` → `UseQueryResult<PageResponse<Recipe>, Error>`

- [ ] **Step 1: Replace `src/lib/query/keys.ts`**

The current file (from the coffees slice) is:

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

Replace its whole contents with (adds the `recipes` group and its import):

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

- [ ] **Step 2: Write the failing test — `src/hooks/useRecipes.test.tsx`**

```tsx
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { renderHook, waitFor } from '@testing-library/react';
import type { ReactNode } from 'react';
import { describe, expect, it, vi } from 'vitest';
import { useRecipes } from './useRecipes';
import * as recipesApi from '@/lib/api/recipes';

function wrapper({ children }: { children: ReactNode }) {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return <QueryClientProvider client={client}>{children}</QueryClientProvider>;
}

describe('useRecipes', () => {
  it('returns the paginated recipes from the API', async () => {
    vi.spyOn(recipesApi, 'listRecipes').mockResolvedValue({
      content: [
        {
          id: 1, coffeeId: 1, coffeeName: 'Mezcla', methodId: 1, methodName: 'AeroPress',
          name: 'Mezcla AeroPress', coffeeGrams: 15, waterGrams: 230, ratio: '1:15',
          grindSetting: null, waterTemp: 90, brewTime: null, steps: null, expectedTaste: null,
          favorite: true, createdAt: '2026-01-01T00:00:00', updatedAt: null,
        },
      ],
      page: 0, size: 10, totalElements: 1, totalPages: 1, first: true, last: true,
    });

    const { result } = renderHook(() => useRecipes({ page: 0, size: 10 }), { wrapper });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data?.content[0].name).toBe('Mezcla AeroPress');
    expect(result.current.data?.totalElements).toBe(1);
  });
});
```

- [ ] **Step 3: Run test to verify it fails**

Run: `npm test -- src/hooks/useRecipes.test.tsx`
Expected: FAIL (cannot find `./useRecipes`).

- [ ] **Step 4: Create `src/hooks/useRecipes.ts`**

```ts
'use client';

import { keepPreviousData, useQuery } from '@tanstack/react-query';
import { listRecipes, type ListRecipesParams } from '@/lib/api/recipes';
import { keys } from '@/lib/query/keys';

export function useRecipes(params: ListRecipesParams) {
  return useQuery({
    queryKey: keys.recipes.list(params),
    queryFn: () => listRecipes(params),
    placeholderData: keepPreviousData,
  });
}
```

- [ ] **Step 5: Run test to verify it passes**

Run: `npm test -- src/hooks/useRecipes.test.tsx`
Expected: PASS.

- [ ] **Step 6: Commit (from repo root)**

```bash
cd /Users/jvilla/Documents/brewdeck
git add brewdeck-web/src/lib/query/keys.ts brewdeck-web/src/hooks/useRecipes.ts brewdeck-web/src/hooks/useRecipes.test.tsx
git commit -m "feat(web): add recipes query key and useRecipes hook"
```

---

### Task 3: RecipesTable component

**Files:**
- Create: `brewdeck-web/src/components/recipes/RecipesTable.tsx`
- Test: `brewdeck-web/src/components/recipes/RecipesTable.test.tsx`

**Interfaces:**
- Consumes: `Recipe` (Task 1).
- Produces: `RecipesTable({ recipes }: { recipes: Recipe[] })`

- [ ] **Step 1: Write the failing test — `src/components/recipes/RecipesTable.test.tsx`**

```tsx
import { screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { renderWithTheme } from '@/test/renderWithTheme';
import { RecipesTable } from './RecipesTable';
import type { Recipe } from '@/lib/api/types';

const base: Recipe = {
  id: 1, coffeeId: 1, coffeeName: 'Mezcla', methodId: 1, methodName: 'AeroPress',
  name: 'Mezcla AeroPress', coffeeGrams: 15, waterGrams: 230, ratio: '1:15',
  grindSetting: null, waterTemp: 90, brewTime: null, steps: null, expectedTaste: null,
  favorite: true, createdAt: '2026-01-01T00:00:00', updatedAt: null,
};

const other: Recipe = {
  ...base, id: 2, name: 'Plain V60', coffeeName: 'Other', methodName: 'V60',
  ratio: null, waterTemp: null, favorite: false,
};

describe('RecipesTable', () => {
  it('renders recipe rows with coffee/method names, a star for favorites and dashes for null/false', () => {
    renderWithTheme(<RecipesTable recipes={[base, other]} />);

    expect(screen.getByText('Mezcla AeroPress')).toBeInTheDocument();
    expect(screen.getByText('Mezcla')).toBeInTheDocument();
    expect(screen.getByText('AeroPress')).toBeInTheDocument();
    expect(screen.getByText('1:15')).toBeInTheDocument();
    expect(screen.getByText('90')).toBeInTheDocument();
    expect(screen.getByText('★')).toBeInTheDocument();

    // 'other' has null ratio, null waterTemp, and favorite=false → three dashes
    expect(screen.getAllByText('—').length).toBeGreaterThanOrEqual(3);
  });
});
```

- [ ] **Step 2: Run test to verify it fails**

Run: `npm test -- src/components/recipes/RecipesTable.test.tsx`
Expected: FAIL (cannot find `./RecipesTable`).

- [ ] **Step 3: Create `src/components/recipes/RecipesTable.tsx`**

```tsx
'use client';

import Paper from '@mui/material/Paper';
import Table from '@mui/material/Table';
import TableBody from '@mui/material/TableBody';
import TableCell from '@mui/material/TableCell';
import TableContainer from '@mui/material/TableContainer';
import TableHead from '@mui/material/TableHead';
import TableRow from '@mui/material/TableRow';
import type { Recipe } from '@/lib/api/types';

function orDash(value: string | number | null): string {
  if (value === null) {
    return '—';
  }
  const text = String(value);
  return text.trim() !== '' ? text : '—';
}

export function RecipesTable({ recipes }: { recipes: Recipe[] }) {
  return (
    <TableContainer component={Paper} variant="outlined">
      <Table>
        <TableHead>
          <TableRow>
            <TableCell>Name</TableCell>
            <TableCell>Coffee</TableCell>
            <TableCell>Method</TableCell>
            <TableCell>Ratio</TableCell>
            <TableCell>Water Temp</TableCell>
            <TableCell>Favorite</TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          {recipes.map((recipe) => (
            <TableRow key={recipe.id}>
              <TableCell>{recipe.name}</TableCell>
              <TableCell>{recipe.coffeeName}</TableCell>
              <TableCell>{recipe.methodName}</TableCell>
              <TableCell>{orDash(recipe.ratio)}</TableCell>
              <TableCell>{orDash(recipe.waterTemp)}</TableCell>
              <TableCell>{recipe.favorite ? '★' : '—'}</TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </TableContainer>
  );
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `npm test -- src/components/recipes/RecipesTable.test.tsx`
Expected: PASS.

- [ ] **Step 5: Commit (from repo root)**

```bash
cd /Users/jvilla/Documents/brewdeck
git add brewdeck-web/src/components/recipes/RecipesTable.tsx brewdeck-web/src/components/recipes/RecipesTable.test.tsx
git commit -m "feat(web): add RecipesTable component"
```

---

### Task 4: RecipeFilters component

**Files:**
- Create: `brewdeck-web/src/components/recipes/RecipeFilters.tsx`
- Test: `brewdeck-web/src/components/recipes/RecipeFilters.test.tsx`

**Interfaces:**
- Consumes: `RecipeFilters` type (Task 1).
- Produces: `RecipeFilters({ value, onChange }: { value: RecipeFilters; onChange: (next: RecipeFilters) => void })` (component named `RecipeFilters`; import the type under an alias to avoid a name clash).

- [ ] **Step 1: Write the failing test — `src/components/recipes/RecipeFilters.test.tsx`**

```tsx
import { fireEvent, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { renderWithTheme } from '@/test/renderWithTheme';
import { RecipeFilters } from './RecipeFilters';

describe('RecipeFilters', () => {
  it('calls onChange with the merged name when the name field changes', () => {
    const onChange = vi.fn();
    renderWithTheme(<RecipeFilters value={{ favorite: true }} onChange={onChange} />);

    fireEvent.change(screen.getByLabelText('Name'), { target: { value: 'AeroPress' } });

    expect(onChange).toHaveBeenCalledWith({ favorite: true, name: 'AeroPress' });
  });

  it('calls onChange with favorite true when the Favorites only box is checked', () => {
    const onChange = vi.fn();
    renderWithTheme(<RecipeFilters value={{}} onChange={onChange} />);

    fireEvent.click(screen.getByLabelText('Favorites only'));

    expect(onChange).toHaveBeenCalledWith({ favorite: true });
  });
});
```

- [ ] **Step 2: Run test to verify it fails**

Run: `npm test -- src/components/recipes/RecipeFilters.test.tsx`
Expected: FAIL (cannot find `./RecipeFilters`).

- [ ] **Step 3: Create `src/components/recipes/RecipeFilters.tsx`**

```tsx
'use client';

import Box from '@mui/material/Box';
import Checkbox from '@mui/material/Checkbox';
import FormControlLabel from '@mui/material/FormControlLabel';
import TextField from '@mui/material/TextField';
import type { ChangeEvent } from 'react';
import type { RecipeFilters as Filters } from '@/lib/api/types';

export function RecipeFilters({
  value,
  onChange,
}: {
  value: Filters;
  onChange: (next: Filters) => void;
}) {
  const handleName = (event: ChangeEvent<HTMLInputElement>) => {
    onChange({ ...value, name: event.target.value });
  };

  const handleFavorite = (event: ChangeEvent<HTMLInputElement>) => {
    onChange({ ...value, favorite: event.target.checked ? true : undefined });
  };

  return (
    <Box sx={{ display: 'flex', gap: 2, alignItems: 'center', flexWrap: 'wrap', mb: 2 }}>
      <TextField label="Name" size="small" value={value.name ?? ''} onChange={handleName} />
      <FormControlLabel
        control={<Checkbox checked={value.favorite ?? false} onChange={handleFavorite} />}
        label="Favorites only"
      />
    </Box>
  );
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `npm test -- src/components/recipes/RecipeFilters.test.tsx`
Expected: PASS (2 tests).

- [ ] **Step 5: Commit (from repo root)**

```bash
cd /Users/jvilla/Documents/brewdeck
git add brewdeck-web/src/components/recipes/RecipeFilters.tsx brewdeck-web/src/components/recipes/RecipeFilters.test.tsx
git commit -m "feat(web): add RecipeFilters component"
```

---

### Task 5: RecipesView, route, and nav

**Files:**
- Create: `brewdeck-web/src/components/recipes/RecipesView.tsx`
- Create: `brewdeck-web/src/app/recipes/page.tsx`
- Modify: `brewdeck-web/src/components/layout/AppShell.tsx` (enable Recipes nav)
- Test: `brewdeck-web/src/components/recipes/RecipesView.test.tsx`

**Interfaces:**
- Consumes: `useRecipes` (Task 2), `useDebounce` (existing), `RecipeFilters` component (Task 4), `RecipesTable` (Task 3), `Spinner`/`ErrorState`/`EmptyState` (existing), `RecipeFilters` type (Task 1).
- Produces: `RecipesView()` client component; `/recipes` route.

- [ ] **Step 1: Write the failing test — `src/components/recipes/RecipesView.test.tsx`**

```tsx
import { fireEvent, screen } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { renderWithTheme } from '@/test/renderWithTheme';
import { RecipesView } from './RecipesView';
import * as recipesHook from '@/hooks/useRecipes';
import type { PageResponse, Recipe } from '@/lib/api/types';

type HookReturn = ReturnType<typeof recipesHook.useRecipes>;

function mockHook(value: Partial<HookReturn>) {
  return vi.spyOn(recipesHook, 'useRecipes').mockReturnValue(value as HookReturn);
}

function page(content: Recipe[], totalElements: number): PageResponse<Recipe> {
  return { content, page: 0, size: 10, totalElements, totalPages: 1, first: true, last: true };
}

const recipe: Recipe = {
  id: 1, coffeeId: 1, coffeeName: 'Mezcla', methodId: 1, methodName: 'AeroPress',
  name: 'Mezcla AeroPress', coffeeGrams: 15, waterGrams: 230, ratio: '1:15',
  grindSetting: null, waterTemp: 90, brewTime: null, steps: null, expectedTaste: null,
  favorite: true, createdAt: '2026-01-01T00:00:00', updatedAt: null,
};

afterEach(() => vi.restoreAllMocks());

describe('RecipesView', () => {
  it('shows a spinner while loading', () => {
    mockHook({ isLoading: true, isError: false, data: undefined });
    renderWithTheme(<RecipesView />);
    expect(screen.getByRole('status', { name: /loading/i })).toBeInTheDocument();
  });

  it('shows an error state on failure', () => {
    mockHook({ isLoading: false, isError: true, data: undefined, refetch: vi.fn() });
    renderWithTheme(<RecipesView />);
    expect(screen.getByText(/could not load recipes/i)).toBeInTheDocument();
  });

  it('shows an empty state when there are no recipes', () => {
    mockHook({ isLoading: false, isError: false, data: page([], 0) });
    renderWithTheme(<RecipesView />);
    expect(screen.getByText(/no recipes found/i)).toBeInTheDocument();
  });

  it('renders the table on success', () => {
    mockHook({ isLoading: false, isError: false, data: page([recipe], 1) });
    renderWithTheme(<RecipesView />);
    expect(screen.getByText('Mezcla AeroPress')).toBeInTheDocument();
  });

  it('resets to page 0 when a filter changes', () => {
    const hookMock = mockHook({ isLoading: false, isError: false, data: page([recipe], 100) });
    renderWithTheme(<RecipesView />);

    fireEvent.click(screen.getByRole('button', { name: /next page/i }));
    expect(hookMock).toHaveBeenLastCalledWith(expect.objectContaining({ page: 1 }));

    fireEvent.change(screen.getByLabelText('Name'), { target: { value: 'V60' } });
    expect(hookMock).toHaveBeenLastCalledWith(expect.objectContaining({ page: 0 }));
  });
});
```

- [ ] **Step 2: Run test to verify it fails**

Run: `npm test -- src/components/recipes/RecipesView.test.tsx`
Expected: FAIL (cannot find `./RecipesView`).

- [ ] **Step 3: Create `src/components/recipes/RecipesView.tsx`**

```tsx
'use client';

import TablePagination from '@mui/material/TablePagination';
import Typography from '@mui/material/Typography';
import { useState, type ReactNode } from 'react';
import { useRecipes } from '@/hooks/useRecipes';
import { useDebounce } from '@/hooks/useDebounce';
import { Spinner } from '@/components/ui/Spinner';
import { ErrorState } from '@/components/ui/ErrorState';
import { EmptyState } from '@/components/ui/EmptyState';
import { RecipeFilters } from './RecipeFilters';
import { RecipesTable } from './RecipesTable';
import type { RecipeFilters as Filters } from '@/lib/api/types';

export function RecipesView() {
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(10);
  const [filters, setFilters] = useState<Filters>({});
  const debouncedFilters = useDebounce(filters, 300);

  const { data, isLoading, isError, refetch } = useRecipes({
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
    body = <ErrorState message="Could not load recipes." onRetry={() => refetch()} />;
  } else if (data.content.length === 0) {
    body = <EmptyState message="No recipes found." />;
  } else {
    body = (
      <>
        <RecipesTable recipes={data.content} />
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
        Recipes
      </Typography>
      <RecipeFilters value={filters} onChange={handleFiltersChange} />
      {body}
    </>
  );
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `npm test -- src/components/recipes/RecipesView.test.tsx`
Expected: PASS (5 tests).

- [ ] **Step 5: Create `src/app/recipes/page.tsx`**

```tsx
import { RecipesView } from '@/components/recipes/RecipesView';

export default function RecipesPage() {
  return <RecipesView />;
}
```

- [ ] **Step 6: Enable the Recipes nav item in `src/components/layout/AppShell.tsx`**

Find the `NAV` array entry for Recipes, currently:
`{ label: 'Recipes', href: '/recipes', enabled: false },`
Change it to:
`{ label: 'Recipes', href: '/recipes', enabled: true },`
Leave the other entries unchanged.

- [ ] **Step 7: Run the full suite and build**

Run (from `brewdeck-web/`): `npm test && npm run build`
Expected: all tests pass; build succeeds; `/recipes` route present.

- [ ] **Step 8: Commit (from repo root)**

```bash
cd /Users/jvilla/Documents/brewdeck
git add brewdeck-web/src/components/recipes/RecipesView.tsx brewdeck-web/src/components/recipes/RecipesView.test.tsx brewdeck-web/src/app/recipes/page.tsx brewdeck-web/src/components/layout/AppShell.tsx
git commit -m "feat(web): add recipes list view, route and nav entry"
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

Open `http://localhost:3000/recipes`. Expected: the Recipes nav item is active; a table renders (create a coffee, a brew method, and a recipe via the API/Postman if the DB is empty); pagination controls show; typing in Name filters after a brief debounce; checking "Favorites only" narrows to favorites; no CORS error in the console.

- [ ] **Step 4: Verify the empty and error states**

With Name = `zzzzz`, expect "No recipes found." Stop the backend and reload; expect the error alert with Retry.

- [ ] **Step 5: Push the branch and open the PR (from repo root)**

```bash
cd /Users/jvilla/Documents/brewdeck
git push -u origin feature/recipes-list
```

Then open a PR from `feature/recipes-list` into `feature/coffees-list` (retarget up the chain as parents merge), titled `feat(web): recipes list — paginated, filterable table`, referencing `docs/superpowers/specs/2026-07-04-recipes-list-design.md`. If `gh` is available, use `gh pr create --base feature/coffees-list --head feature/recipes-list --title "..." --body-file <path>`; otherwise open the compare URL.

---

## Notes for the implementer

- Run test/build commands from `brewdeck-web/`; run `git` from the repo root.
- `keys.ts` imports `ListRecipesParams` (type-only) from `@/lib/api/recipes`; no runtime cycle (recipes.ts does not import keys.ts).
- In `RecipesView`, `setPage(0)` is synchronous, so a filter change resets the page immediately even though the filter value is debounced before reaching the query — this is what the page-reset test asserts.
- This branch is stacked on `feature/coffees-list`; it edits the shared `keys.ts`, `types.ts`, and `AppShell.tsx` on top of the coffees versions.
