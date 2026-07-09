# Recipe CRUD Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add recipe create/edit/delete from the recipes list via MUI modal dialogs, with foreign-key Select dropdowns (coffee, brew method), React Hook Form + Zod validation, and TanStack Query mutations.

**Architecture:** Mirror the coffee-CRUD slice. A new brew-methods API module and two options hooks feed the FK Selects. A Zod schema (with `z.coerce.number` for numeric fields) validates the form. Write API + mutation hooks invalidate the recipes list. One `RecipeFormDialog` (create/edit) and a `DeleteRecipeDialog` are wired into `RecipesView`/`RecipesTable`.

**Tech Stack:** Next.js App Router, TypeScript strict, MUI 9, TanStack Query, React Hook Form, Zod, Vitest + React Testing Library.

## Global Constraints

- App under `brewdeck-web/`, source `brewdeck-web/src/`, alias `@/*` → `src/*`.
- TypeScript strict — no `any` in components (tests may use `as never` for mocked hooks).
- No `fetch` outside `src/lib/api/client.ts`; API modules call `apiFetch`.
- Backend: `POST /api/recipes` (201), `PUT /api/recipes/{id}` (200), `DELETE /api/recipes/{id}` (204); 400 → `ErrorResponse.validationErrors`.
- `RecipeRequest` limits: `coffeeId` required; `methodId` required; `name` required max 120; `coffeeGrams`/`waterGrams` positive (>0); `ratio` max 20; `grindSetting` max 120; `waterTemp` 70–100 (degrees Celsius); `brewTime` max 20; `steps` max 1000; `expectedTaste` max 500; `favorite` boolean.
- Validation messages avoid special symbols (use "degrees Celsius", not the degree symbol).
- Mutations invalidate `{ queryKey: ['recipes'] }` on success.
- FK Selects use native selects (`SelectProps={{ native: true }}`) for testability and accessibility.
- Run npm commands from `brewdeck-web/`; run `git` from repo root `/Users/jvilla/Documents/brewdeck`. Report the FULL test-suite totals after each task; never dismiss a failure as "unrelated".
- Conventional Commits; commit at the end of each task.

---

### Task 1: Brew-methods API module

**Files:**
- Create: `brewdeck-web/src/lib/api/brewMethods.ts`
- Test: `brewdeck-web/src/lib/api/brewMethods.test.ts`

**Interfaces:**
- Produces: `type BrewMethod`; `listBrewMethods(params: { page: number; size: number }): Promise<PageResponse<BrewMethod>>`

- [ ] **Step 1: Write the failing test — `src/lib/api/brewMethods.test.ts`**

```ts
import { afterEach, describe, expect, it, vi } from 'vitest';
import { listBrewMethods } from './brewMethods';

function stubFetch() {
  const fetchMock = vi.fn().mockResolvedValue({
    ok: true,
    status: 200,
    json: () => Promise.resolve({ content: [], page: 0, size: 100, totalElements: 0, totalPages: 0, first: true, last: true }),
  });
  vi.stubGlobal('fetch', fetchMock);
  return fetchMock;
}

afterEach(() => vi.unstubAllGlobals());

describe('listBrewMethods', () => {
  it('requests /api/brew-methods with page and size', async () => {
    const fetchMock = stubFetch();
    await listBrewMethods({ page: 0, size: 100 });
    const url = String(fetchMock.mock.calls[0][0]);
    expect(url).toContain('/api/brew-methods?');
    expect(url).toContain('page=0');
    expect(url).toContain('size=100');
  });
});
```

- [ ] **Step 2: Run test to verify it fails**

Run: `npm test -- src/lib/api/brewMethods.test.ts`
Expected: FAIL (cannot find `./brewMethods`).

- [ ] **Step 3: Create `src/lib/api/brewMethods.ts`**

```ts
import { apiFetch } from './client';
import type { PageResponse } from './types';

export type BrewMethod = {
  id: number;
  name: string;
  description: string | null;
  createdAt: string;
  updatedAt: string | null;
};

export function listBrewMethods(params: {
  page: number;
  size: number;
}): Promise<PageResponse<BrewMethod>> {
  const query = new URLSearchParams();
  query.set('page', String(params.page));
  query.set('size', String(params.size));
  return apiFetch<PageResponse<BrewMethod>>(`/api/brew-methods?${query.toString()}`);
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `npm test -- src/lib/api/brewMethods.test.ts`
Expected: PASS.

- [ ] **Step 5: Commit (from repo root)**

```bash
cd /Users/jvilla/Documents/brewdeck
git add brewdeck-web/src/lib/api/brewMethods.ts brewdeck-web/src/lib/api/brewMethods.test.ts
git commit -m "feat(web): add brew-methods API module"
```

---

### Task 2: Recipe form schema

**Files:**
- Create: `brewdeck-web/src/lib/validation/recipeSchema.ts`
- Test: `brewdeck-web/src/lib/validation/recipeSchema.test.ts`

**Interfaces:**
- Produces: `recipeSchema`; `type RecipeFormValues = z.infer<typeof recipeSchema>` (coerced output — numeric fields are numbers).

- [ ] **Step 1: Write the failing test — `src/lib/validation/recipeSchema.test.ts`**

```ts
import { describe, expect, it } from 'vitest';
import { recipeSchema } from './recipeSchema';

const valid = { coffeeId: '1', methodId: '2', name: 'AeroPress' };

describe('recipeSchema', () => {
  it('requires coffeeId, methodId and name', () => {
    const r = recipeSchema.safeParse({ coffeeId: '', methodId: '', name: '' });
    expect(r.success).toBe(false);
    if (!r.success) {
      const messages = r.error.issues.map((i) => i.message);
      expect(messages).toContain('Coffee is required');
      expect(messages).toContain('Brew method is required');
      expect(messages).toContain('Name is required');
    }
  });

  it('coerces numeric string inputs to numbers', () => {
    const r = recipeSchema.safeParse({ ...valid, coffeeGrams: '15', waterTemp: '90' });
    expect(r.success).toBe(true);
    if (r.success) {
      expect(r.data.coffeeId).toBe(1);
      expect(r.data.coffeeGrams).toBe(15);
      expect(r.data.waterTemp).toBe(90);
    }
  });

  it('treats blank optional numbers as undefined (no NaN)', () => {
    const r = recipeSchema.safeParse({ ...valid, coffeeGrams: '', waterTemp: '' });
    expect(r.success).toBe(true);
    if (r.success) {
      expect(r.data.coffeeGrams).toBeUndefined();
      expect(r.data.waterTemp).toBeUndefined();
    }
  });

  it('rejects a water temperature below 70', () => {
    const r = recipeSchema.safeParse({ ...valid, waterTemp: '60' });
    expect(r.success).toBe(false);
    if (!r.success) {
      expect(r.error.issues[0].message).toBe('Water temperature must be at least 70 degrees Celsius');
    }
  });
});
```

- [ ] **Step 2: Run test to verify it fails**

Run: `npm test -- src/lib/validation/recipeSchema.test.ts`
Expected: FAIL (cannot find `./recipeSchema`).

- [ ] **Step 3: Create `src/lib/validation/recipeSchema.ts`**

```ts
import { z } from 'zod';

function optionalNumber(inner: z.ZodNumber) {
  return z.preprocess(
    (value) => (value === '' || value === null || value === undefined ? undefined : value),
    inner.optional(),
  );
}

export const recipeSchema = z.object({
  coffeeId: z.coerce.number().int().positive('Coffee is required'),
  methodId: z.coerce.number().int().positive('Brew method is required'),
  name: z.string().min(1, 'Name is required').max(120, 'Name must not exceed 120 characters'),
  coffeeGrams: optionalNumber(z.coerce.number().positive('Coffee grams must be greater than zero')),
  waterGrams: optionalNumber(z.coerce.number().positive('Water grams must be greater than zero')),
  ratio: z.string().max(20, 'Ratio must not exceed 20 characters').optional(),
  grindSetting: z.string().max(120, 'Grind setting must not exceed 120 characters').optional(),
  waterTemp: optionalNumber(
    z.coerce
      .number()
      .min(70, 'Water temperature must be at least 70 degrees Celsius')
      .max(100, 'Water temperature must not exceed 100 degrees Celsius'),
  ),
  brewTime: z.string().max(20, 'Brew time must not exceed 20 characters').optional(),
  steps: z.string().max(1000, 'Steps must not exceed 1000 characters').optional(),
  expectedTaste: z.string().max(500, 'Expected taste must not exceed 500 characters').optional(),
  favorite: z.boolean().optional(),
});

export type RecipeFormValues = z.infer<typeof recipeSchema>;
```

- [ ] **Step 4: Run test to verify it passes**

Run: `npm test -- src/lib/validation/recipeSchema.test.ts`
Expected: PASS (4 tests).

- [ ] **Step 5: Commit (from repo root)**

```bash
cd /Users/jvilla/Documents/brewdeck
git add brewdeck-web/src/lib/validation/recipeSchema.ts brewdeck-web/src/lib/validation/recipeSchema.test.ts
git commit -m "feat(web): add recipe form schema"
```

---

### Task 3: Recipe write API functions

**Files:**
- Modify: `brewdeck-web/src/lib/api/recipes.ts` (append)
- Test: `brewdeck-web/src/lib/api/recipesMutations.test.ts`

**Interfaces:**
- Consumes: `RecipeFormValues` (Task 2), `Recipe` (existing).
- Produces: `createRecipe(body)`, `updateRecipe(id, body)`, `deleteRecipe(id)`.

- [ ] **Step 1: Write the failing test — `src/lib/api/recipesMutations.test.ts`**

```ts
import { afterEach, describe, expect, it, vi } from 'vitest';
import { createRecipe, updateRecipe, deleteRecipe } from './recipes';

function stubFetch() {
  const fetchMock = vi.fn().mockResolvedValue({
    ok: true,
    status: 200,
    json: () => Promise.resolve({ id: 1, name: 'AeroPress' }),
  });
  vi.stubGlobal('fetch', fetchMock);
  return fetchMock;
}

afterEach(() => vi.unstubAllGlobals());

describe('recipe write API', () => {
  it('createRecipe POSTs to /api/recipes', async () => {
    const fetchMock = stubFetch();
    await createRecipe({ coffeeId: 1, methodId: 2, name: 'AeroPress' });
    const [url, init] = fetchMock.mock.calls[0];
    expect(String(url)).toContain('/api/recipes');
    expect(init.method).toBe('POST');
    expect(JSON.parse(init.body)).toEqual({ coffeeId: 1, methodId: 2, name: 'AeroPress' });
  });

  it('updateRecipe PUTs to /api/recipes/{id}', async () => {
    const fetchMock = stubFetch();
    await updateRecipe(7, { coffeeId: 1, methodId: 2, name: 'Updated' });
    const [url, init] = fetchMock.mock.calls[0];
    expect(String(url)).toContain('/api/recipes/7');
    expect(init.method).toBe('PUT');
  });

  it('deleteRecipe DELETEs /api/recipes/{id}', async () => {
    const fetchMock = stubFetch();
    await deleteRecipe(7);
    const [url, init] = fetchMock.mock.calls[0];
    expect(String(url)).toContain('/api/recipes/7');
    expect(init.method).toBe('DELETE');
  });
});
```

- [ ] **Step 2: Run test to verify it fails**

Run: `npm test -- src/lib/api/recipesMutations.test.ts`
Expected: FAIL (functions not exported).

- [ ] **Step 3: Append to `src/lib/api/recipes.ts`**

Add the import with the existing imports and the three functions at the end (keep the existing `listRecipes` and `Recipe` import unchanged):

```ts
import type { RecipeFormValues } from '@/lib/validation/recipeSchema';
```

```ts
export function createRecipe(body: RecipeFormValues): Promise<Recipe> {
  return apiFetch<Recipe>('/api/recipes', { method: 'POST', body: JSON.stringify(body) });
}

export function updateRecipe(id: number, body: RecipeFormValues): Promise<Recipe> {
  return apiFetch<Recipe>(`/api/recipes/${id}`, { method: 'PUT', body: JSON.stringify(body) });
}

export function deleteRecipe(id: number): Promise<void> {
  return apiFetch<void>(`/api/recipes/${id}`, { method: 'DELETE' });
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `npm test -- src/lib/api/recipesMutations.test.ts`
Expected: PASS (3 tests).

- [ ] **Step 5: Commit (from repo root)**

```bash
cd /Users/jvilla/Documents/brewdeck
git add brewdeck-web/src/lib/api/recipes.ts brewdeck-web/src/lib/api/recipesMutations.test.ts
git commit -m "feat(web): add recipe create/update/delete API functions"
```

---

### Task 4: Recipe mutation hooks

**Files:**
- Create: `brewdeck-web/src/hooks/useRecipeMutations.ts`
- Test: `brewdeck-web/src/hooks/useRecipeMutations.test.tsx`

**Interfaces:**
- Consumes: `createRecipe`/`updateRecipe`/`deleteRecipe` (Task 3), `RecipeFormValues` (Task 2).
- Produces: `useCreateRecipe()`, `useUpdateRecipe()`, `useDeleteRecipe()`. `useUpdateRecipe.mutate` takes `{ id, body }`; create takes `RecipeFormValues`; delete takes `number`.

- [ ] **Step 1: Write the failing test — `src/hooks/useRecipeMutations.test.tsx`**

```tsx
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { renderHook, waitFor } from '@testing-library/react';
import type { ReactNode } from 'react';
import { describe, expect, it, vi } from 'vitest';
import { useCreateRecipe, useUpdateRecipe, useDeleteRecipe } from './useRecipeMutations';
import * as recipesApi from '@/lib/api/recipes';

function setup() {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  const invalidateSpy = vi.spyOn(client, 'invalidateQueries');
  const wrapper = ({ children }: { children: ReactNode }) => (
    <QueryClientProvider client={client}>{children}</QueryClientProvider>
  );
  return { wrapper, invalidateSpy };
}

describe('recipe mutation hooks', () => {
  it('useCreateRecipe invalidates recipes on success', async () => {
    vi.spyOn(recipesApi, 'createRecipe').mockResolvedValue({ id: 1, name: 'AeroPress' } as never);
    const { wrapper, invalidateSpy } = setup();
    const { result } = renderHook(() => useCreateRecipe(), { wrapper });
    result.current.mutate({ coffeeId: 1, methodId: 2, name: 'AeroPress' });
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(invalidateSpy).toHaveBeenCalledWith({ queryKey: ['recipes'] });
  });

  it('useUpdateRecipe invalidates recipes on success', async () => {
    vi.spyOn(recipesApi, 'updateRecipe').mockResolvedValue({ id: 1, name: 'Updated' } as never);
    const { wrapper, invalidateSpy } = setup();
    const { result } = renderHook(() => useUpdateRecipe(), { wrapper });
    result.current.mutate({ id: 1, body: { coffeeId: 1, methodId: 2, name: 'Updated' } });
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(invalidateSpy).toHaveBeenCalledWith({ queryKey: ['recipes'] });
  });

  it('useDeleteRecipe invalidates recipes on success', async () => {
    vi.spyOn(recipesApi, 'deleteRecipe').mockResolvedValue(undefined);
    const { wrapper, invalidateSpy } = setup();
    const { result } = renderHook(() => useDeleteRecipe(), { wrapper });
    result.current.mutate(1);
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(invalidateSpy).toHaveBeenCalledWith({ queryKey: ['recipes'] });
  });
});
```

- [ ] **Step 2: Run test to verify it fails**

Run: `npm test -- src/hooks/useRecipeMutations.test.tsx`
Expected: FAIL (cannot find `./useRecipeMutations`).

- [ ] **Step 3: Create `src/hooks/useRecipeMutations.ts`**

```ts
'use client';

import { useMutation, useQueryClient } from '@tanstack/react-query';
import { createRecipe, deleteRecipe, updateRecipe } from '@/lib/api/recipes';
import type { RecipeFormValues } from '@/lib/validation/recipeSchema';

export function useCreateRecipe() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (body: RecipeFormValues) => createRecipe(body),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['recipes'] }),
  });
}

export function useUpdateRecipe() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, body }: { id: number; body: RecipeFormValues }) => updateRecipe(id, body),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['recipes'] }),
  });
}

export function useDeleteRecipe() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => deleteRecipe(id),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['recipes'] }),
  });
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `npm test -- src/hooks/useRecipeMutations.test.tsx`
Expected: PASS (3 tests).

- [ ] **Step 5: Commit (from repo root)**

```bash
cd /Users/jvilla/Documents/brewdeck
git add brewdeck-web/src/hooks/useRecipeMutations.ts brewdeck-web/src/hooks/useRecipeMutations.test.tsx
git commit -m "feat(web): add recipe mutation hooks that invalidate the list"
```

---

### Task 5: Coffee and method options hooks

**Files:**
- Create: `brewdeck-web/src/hooks/useResourceOptions.ts`
- Test: `brewdeck-web/src/hooks/useResourceOptions.test.tsx`

**Interfaces:**
- Consumes: `listCoffees` (existing), `listBrewMethods` (Task 1).
- Produces: `useCoffeeOptions()` and `useMethodOptions()` — TanStack `useQuery` results whose `data` is `Array<{ id: number; name: string }>`.

- [ ] **Step 1: Write the failing test — `src/hooks/useResourceOptions.test.tsx`**

```tsx
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { renderHook, waitFor } from '@testing-library/react';
import type { ReactNode } from 'react';
import { describe, expect, it, vi } from 'vitest';
import { useCoffeeOptions } from './useResourceOptions';
import * as coffeesApi from '@/lib/api/coffees';

function wrapper({ children }: { children: ReactNode }) {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return <QueryClientProvider client={client}>{children}</QueryClientProvider>;
}

describe('useCoffeeOptions', () => {
  it('maps the coffee page content to { id, name } options', async () => {
    vi.spyOn(coffeesApi, 'listCoffees').mockResolvedValue({
      content: [{ id: 3, name: 'Mezcla' }] as never,
      page: 0, size: 100, totalElements: 1, totalPages: 1, first: true, last: true,
    });
    const { result } = renderHook(() => useCoffeeOptions(), { wrapper });
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data).toEqual([{ id: 3, name: 'Mezcla' }]);
  });
});
```

- [ ] **Step 2: Run test to verify it fails**

Run: `npm test -- src/hooks/useResourceOptions.test.tsx`
Expected: FAIL (cannot find `./useResourceOptions`).

- [ ] **Step 3: Create `src/hooks/useResourceOptions.ts`**

```ts
'use client';

import { useQuery } from '@tanstack/react-query';
import { listCoffees } from '@/lib/api/coffees';
import { listBrewMethods } from '@/lib/api/brewMethods';

export type ResourceOption = { id: number; name: string };

export function useCoffeeOptions() {
  return useQuery({
    queryKey: ['coffees', 'options'],
    queryFn: () => listCoffees({ page: 0, size: 100 }),
    select: (page): ResourceOption[] => page.content.map((c) => ({ id: c.id, name: c.name })),
  });
}

export function useMethodOptions() {
  return useQuery({
    queryKey: ['brew-methods', 'options'],
    queryFn: () => listBrewMethods({ page: 0, size: 100 }),
    select: (page): ResourceOption[] => page.content.map((m) => ({ id: m.id, name: m.name })),
  });
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `npm test -- src/hooks/useResourceOptions.test.tsx`
Expected: PASS.

- [ ] **Step 5: Commit (from repo root)**

```bash
cd /Users/jvilla/Documents/brewdeck
git add brewdeck-web/src/hooks/useResourceOptions.ts brewdeck-web/src/hooks/useResourceOptions.test.tsx
git commit -m "feat(web): add coffee and method options hooks"
```

---

### Task 6: RecipeFormDialog

**Files:**
- Create: `brewdeck-web/src/components/recipes/RecipeFormDialog.tsx`
- Test: `brewdeck-web/src/components/recipes/RecipeFormDialog.test.tsx`

**Interfaces:**
- Consumes: `recipeSchema`/`RecipeFormValues` (Task 2), `useCreateRecipe`/`useUpdateRecipe` (Task 4), `useCoffeeOptions`/`useMethodOptions` (Task 5), `ApiError` (existing), `Recipe` (existing).
- Produces: `RecipeFormDialog({ open, recipe, onClose }: { open: boolean; recipe?: Recipe; onClose: () => void })`.

- [ ] **Step 1: Write the failing test — `src/components/recipes/RecipeFormDialog.test.tsx`**

```tsx
import { fireEvent, screen, waitFor } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { renderWithTheme } from '@/test/renderWithTheme';
import { RecipeFormDialog } from './RecipeFormDialog';
import * as mutations from '@/hooks/useRecipeMutations';
import * as options from '@/hooks/useResourceOptions';
import { ApiError } from '@/lib/api/client';

const createMutate = vi.fn();
const updateMutate = vi.fn();

function mockAll() {
  vi.spyOn(mutations, 'useCreateRecipe').mockReturnValue({ mutate: createMutate, isPending: false } as never);
  vi.spyOn(mutations, 'useUpdateRecipe').mockReturnValue({ mutate: updateMutate, isPending: false } as never);
  vi.spyOn(options, 'useCoffeeOptions').mockReturnValue({ data: [{ id: 1, name: 'Mezcla' }], isLoading: false } as never);
  vi.spyOn(options, 'useMethodOptions').mockReturnValue({ data: [{ id: 2, name: 'AeroPress' }], isLoading: false } as never);
}

beforeEach(() => {
  createMutate.mockReset();
  updateMutate.mockReset();
});
afterEach(() => vi.restoreAllMocks());

describe('RecipeFormDialog', () => {
  it('renders the coffee and method options', () => {
    mockAll();
    renderWithTheme(<RecipeFormDialog open onClose={vi.fn()} />);
    expect(screen.getByRole('option', { name: 'Mezcla' })).toBeInTheDocument();
    expect(screen.getByRole('option', { name: 'AeroPress' })).toBeInTheDocument();
  });

  it('blocks submit and shows required errors when empty', async () => {
    mockAll();
    renderWithTheme(<RecipeFormDialog open onClose={vi.fn()} />);
    fireEvent.click(screen.getByRole('button', { name: /create/i }));
    expect(await screen.findByText('Name is required')).toBeInTheDocument();
    expect(createMutate).not.toHaveBeenCalled();
  });

  it('calls create with coerced values on valid submit', async () => {
    mockAll();
    renderWithTheme(<RecipeFormDialog open onClose={vi.fn()} />);
    fireEvent.change(screen.getByRole('combobox', { name: /coffee/i }), { target: { value: '1' } });
    fireEvent.change(screen.getByRole('combobox', { name: /brew method/i }), { target: { value: '2' } });
    fireEvent.change(screen.getByLabelText(/^Name/), { target: { value: 'My Recipe' } });
    fireEvent.click(screen.getByRole('button', { name: /create/i }));
    await waitFor(() => expect(createMutate).toHaveBeenCalledTimes(1));
    expect(createMutate.mock.calls[0][0]).toEqual(
      expect.objectContaining({ coffeeId: 1, methodId: 2, name: 'My Recipe' }),
    );
  });

  it('maps a server 400 validation error onto the field', async () => {
    mockAll();
    createMutate.mockImplementation((_body, opts) =>
      opts.onError(new ApiError(400, 'Validation failed', '/api/recipes', { name: 'Recipe name is required' })),
    );
    renderWithTheme(<RecipeFormDialog open onClose={vi.fn()} />);
    fireEvent.change(screen.getByRole('combobox', { name: /coffee/i }), { target: { value: '1' } });
    fireEvent.change(screen.getByRole('combobox', { name: /brew method/i }), { target: { value: '2' } });
    fireEvent.change(screen.getByLabelText(/^Name/), { target: { value: 'X' } });
    fireEvent.click(screen.getByRole('button', { name: /create/i }));
    expect(await screen.findByText('Recipe name is required')).toBeInTheDocument();
  });
});
```

- [ ] **Step 2: Run test to verify it fails**

Run: `npm test -- src/components/recipes/RecipeFormDialog.test.tsx`
Expected: FAIL (cannot find `./RecipeFormDialog`).

- [ ] **Step 3: Create `src/components/recipes/RecipeFormDialog.tsx`**

```tsx
'use client';

import Alert from '@mui/material/Alert';
import Button from '@mui/material/Button';
import Checkbox from '@mui/material/Checkbox';
import CircularProgress from '@mui/material/CircularProgress';
import Dialog from '@mui/material/Dialog';
import DialogActions from '@mui/material/DialogActions';
import DialogContent from '@mui/material/DialogContent';
import DialogTitle from '@mui/material/DialogTitle';
import FormControlLabel from '@mui/material/FormControlLabel';
import Stack from '@mui/material/Stack';
import TextField from '@mui/material/TextField';
import { zodResolver } from '@hookform/resolvers/zod';
import { useState } from 'react';
import { Controller, useForm } from 'react-hook-form';
import type { z } from 'zod';
import { ApiError } from '@/lib/api/client';
import { recipeSchema, type RecipeFormValues } from '@/lib/validation/recipeSchema';
import { useCreateRecipe, useUpdateRecipe } from '@/hooks/useRecipeMutations';
import { useCoffeeOptions, useMethodOptions } from '@/hooks/useResourceOptions';
import type { Recipe } from '@/lib/api/types';

type RecipeFormInput = z.input<typeof recipeSchema>;

const TEXT_FIELDS: Array<{ name: keyof RecipeFormValues; label: string; multiline?: boolean; number?: boolean }> = [
  { name: 'name', label: 'Name' },
  { name: 'coffeeGrams', label: 'Coffee Grams', number: true },
  { name: 'waterGrams', label: 'Water Grams', number: true },
  { name: 'ratio', label: 'Ratio' },
  { name: 'grindSetting', label: 'Grind Setting' },
  { name: 'waterTemp', label: 'Water Temp', number: true },
  { name: 'brewTime', label: 'Brew Time' },
  { name: 'steps', label: 'Steps', multiline: true },
  { name: 'expectedTaste', label: 'Expected Taste', multiline: true },
];

function toDefaults(recipe?: Recipe): RecipeFormInput {
  return {
    coffeeId: recipe?.coffeeId ?? '',
    methodId: recipe?.methodId ?? '',
    name: recipe?.name ?? '',
    coffeeGrams: recipe?.coffeeGrams ?? '',
    waterGrams: recipe?.waterGrams ?? '',
    ratio: recipe?.ratio ?? '',
    grindSetting: recipe?.grindSetting ?? '',
    waterTemp: recipe?.waterTemp ?? '',
    brewTime: recipe?.brewTime ?? '',
    steps: recipe?.steps ?? '',
    expectedTaste: recipe?.expectedTaste ?? '',
    favorite: recipe?.favorite ?? false,
  } as RecipeFormInput;
}

export function RecipeFormDialog({
  open,
  recipe,
  onClose,
}: {
  open: boolean;
  recipe?: Recipe;
  onClose: () => void;
}) {
  const isEdit = recipe !== undefined;
  const create = useCreateRecipe();
  const update = useUpdateRecipe();
  const coffeeOptions = useCoffeeOptions();
  const methodOptions = useMethodOptions();
  const [serverError, setServerError] = useState<string | null>(null);

  const {
    control,
    register,
    handleSubmit,
    setError,
    formState: { errors },
  } = useForm<RecipeFormInput, unknown, RecipeFormValues>({
    resolver: zodResolver(recipeSchema),
    values: toDefaults(recipe),
  });

  const pending = create.isPending || update.isPending;

  const onSubmit = (data: RecipeFormValues) => {
    setServerError(null);
    const options = {
      onSuccess: () => onClose(),
      onError: (error: unknown) => {
        if (error instanceof ApiError && error.validationErrors) {
          Object.entries(error.validationErrors).forEach(([field, message]) =>
            setError(field as keyof RecipeFormValues, { message }),
          );
        } else {
          setServerError(error instanceof Error ? error.message : 'Something went wrong');
        }
      },
    };
    if (isEdit && recipe) {
      update.mutate({ id: recipe.id, body: data }, options);
    } else {
      create.mutate(data, options);
    }
  };

  return (
    <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
      <DialogTitle>{isEdit ? 'Edit recipe' : 'Add recipe'}</DialogTitle>
      <form onSubmit={handleSubmit(onSubmit)} noValidate>
        <DialogContent>
          {serverError ? (
            <Alert severity="error" sx={{ mb: 2 }}>
              {serverError}
            </Alert>
          ) : null}
          <Stack spacing={2}>
            <Controller
              name="coffeeId"
              control={control}
              render={({ field }) => (
                <TextField
                  select
                  SelectProps={{ native: true }}
                  label="Coffee"
                  required
                  size="small"
                  fullWidth
                  disabled={coffeeOptions.isLoading}
                  value={field.value ?? ''}
                  onChange={field.onChange}
                  error={Boolean(errors.coffeeId)}
                  helperText={errors.coffeeId?.message}
                >
                  <option value="">{coffeeOptions.isLoading ? 'Loading…' : 'Select a coffee'}</option>
                  {(coffeeOptions.data ?? []).map((option) => (
                    <option key={option.id} value={option.id}>
                      {option.name}
                    </option>
                  ))}
                </TextField>
              )}
            />
            <Controller
              name="methodId"
              control={control}
              render={({ field }) => (
                <TextField
                  select
                  SelectProps={{ native: true }}
                  label="Brew Method"
                  required
                  size="small"
                  fullWidth
                  disabled={methodOptions.isLoading}
                  value={field.value ?? ''}
                  onChange={field.onChange}
                  error={Boolean(errors.methodId)}
                  helperText={errors.methodId?.message}
                >
                  <option value="">{methodOptions.isLoading ? 'Loading…' : 'Select a brew method'}</option>
                  {(methodOptions.data ?? []).map((option) => (
                    <option key={option.id} value={option.id}>
                      {option.name}
                    </option>
                  ))}
                </TextField>
              )}
            />
            {TEXT_FIELDS.map((f) => (
              <TextField
                key={f.name}
                label={f.label}
                required={f.name === 'name'}
                type={f.number ? 'number' : 'text'}
                multiline={f.multiline}
                minRows={f.multiline ? 2 : undefined}
                size="small"
                fullWidth
                error={Boolean(errors[f.name])}
                helperText={errors[f.name]?.message}
                {...register(f.name)}
              />
            ))}
            <Controller
              name="favorite"
              control={control}
              render={({ field }) => (
                <FormControlLabel
                  control={<Checkbox checked={Boolean(field.value)} onChange={(e) => field.onChange(e.target.checked)} />}
                  label="Favorite"
                />
              )}
            />
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={onClose} disabled={pending}>
            Cancel
          </Button>
          <Button
            type="submit"
            variant="contained"
            disabled={pending}
            startIcon={pending ? <CircularProgress size={16} /> : undefined}
          >
            {isEdit ? 'Save' : 'Create'}
          </Button>
        </DialogActions>
      </form>
    </Dialog>
  );
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `npm test -- src/components/recipes/RecipeFormDialog.test.tsx`
Expected: PASS (4 tests).

- [ ] **Step 5: Commit (from repo root)**

```bash
cd /Users/jvilla/Documents/brewdeck
git add brewdeck-web/src/components/recipes/RecipeFormDialog.tsx brewdeck-web/src/components/recipes/RecipeFormDialog.test.tsx
git commit -m "feat(web): add RecipeFormDialog for create and edit"
```

---

### Task 7: DeleteRecipeDialog

**Files:**
- Create: `brewdeck-web/src/components/recipes/DeleteRecipeDialog.tsx`
- Test: `brewdeck-web/src/components/recipes/DeleteRecipeDialog.test.tsx`

**Interfaces:**
- Consumes: `useDeleteRecipe` (Task 4), `Recipe` (existing).
- Produces: `DeleteRecipeDialog({ open, recipe, onClose }: { open: boolean; recipe: Recipe; onClose: () => void })`.

- [ ] **Step 1: Write the failing test — `src/components/recipes/DeleteRecipeDialog.test.tsx`**

```tsx
import { fireEvent, screen } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { renderWithTheme } from '@/test/renderWithTheme';
import { DeleteRecipeDialog } from './DeleteRecipeDialog';
import * as mutations from '@/hooks/useRecipeMutations';
import type { Recipe } from '@/lib/api/types';

const deleteMutate = vi.fn();

const recipe: Recipe = {
  id: 7, coffeeId: 1, coffeeName: 'Mezcla', methodId: 2, methodName: 'AeroPress',
  name: 'My Recipe', coffeeGrams: null, waterGrams: null, ratio: null, grindSetting: null,
  waterTemp: null, brewTime: null, steps: null, expectedTaste: null, favorite: false,
  createdAt: '2026-01-01T00:00:00', updatedAt: null,
};

afterEach(() => vi.restoreAllMocks());

describe('DeleteRecipeDialog', () => {
  it('calls the delete mutation with the recipe id when confirmed', () => {
    vi.spyOn(mutations, 'useDeleteRecipe').mockReturnValue({ mutate: deleteMutate, isPending: false } as never);
    renderWithTheme(<DeleteRecipeDialog open recipe={recipe} onClose={vi.fn()} />);
    fireEvent.click(screen.getByRole('button', { name: /^delete$/i }));
    expect(deleteMutate.mock.calls[0][0]).toBe(7);
  });
});
```

- [ ] **Step 2: Run test to verify it fails**

Run: `npm test -- src/components/recipes/DeleteRecipeDialog.test.tsx`
Expected: FAIL (cannot find `./DeleteRecipeDialog`).

- [ ] **Step 3: Create `src/components/recipes/DeleteRecipeDialog.tsx`**

```tsx
'use client';

import Alert from '@mui/material/Alert';
import Button from '@mui/material/Button';
import CircularProgress from '@mui/material/CircularProgress';
import Dialog from '@mui/material/Dialog';
import DialogActions from '@mui/material/DialogActions';
import DialogContent from '@mui/material/DialogContent';
import DialogContentText from '@mui/material/DialogContentText';
import DialogTitle from '@mui/material/DialogTitle';
import { useState } from 'react';
import { useDeleteRecipe } from '@/hooks/useRecipeMutations';
import type { Recipe } from '@/lib/api/types';

export function DeleteRecipeDialog({
  open,
  recipe,
  onClose,
}: {
  open: boolean;
  recipe: Recipe;
  onClose: () => void;
}) {
  const del = useDeleteRecipe();
  const [error, setError] = useState<string | null>(null);

  const onConfirm = () => {
    setError(null);
    del.mutate(recipe.id, {
      onSuccess: () => onClose(),
      onError: (e: unknown) => setError(e instanceof Error ? e.message : 'Something went wrong'),
    });
  };

  return (
    <Dialog open={open} onClose={onClose} maxWidth="xs" fullWidth>
      <DialogTitle>Delete recipe</DialogTitle>
      <DialogContent>
        {error ? (
          <Alert severity="error" sx={{ mb: 2 }}>
            {error}
          </Alert>
        ) : null}
        <DialogContentText>
          Delete recipe &ldquo;{recipe.name}&rdquo;? This cannot be undone.
        </DialogContentText>
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose} disabled={del.isPending}>
          Cancel
        </Button>
        <Button
          color="error"
          variant="contained"
          onClick={onConfirm}
          disabled={del.isPending}
          startIcon={del.isPending ? <CircularProgress size={16} /> : undefined}
        >
          Delete
        </Button>
      </DialogActions>
    </Dialog>
  );
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `npm test -- src/components/recipes/DeleteRecipeDialog.test.tsx`
Expected: PASS.

- [ ] **Step 5: Commit (from repo root)**

```bash
cd /Users/jvilla/Documents/brewdeck
git add brewdeck-web/src/components/recipes/DeleteRecipeDialog.tsx brewdeck-web/src/components/recipes/DeleteRecipeDialog.test.tsx
git commit -m "feat(web): add DeleteRecipeDialog confirm"
```

---

### Task 8: RecipesTable actions column

**Files:**
- Modify: `brewdeck-web/src/components/recipes/RecipesTable.tsx`
- Modify: `brewdeck-web/src/components/recipes/RecipesTable.test.tsx`

**Interfaces:**
- Produces: `RecipesTable({ recipes, onEdit, onDelete }: { recipes: Recipe[]; onEdit?: (r: Recipe) => void; onDelete?: (r: Recipe) => void })` — callbacks optional; the Actions column always renders.

- [ ] **Step 1: Update `src/components/recipes/RecipesTable.tsx`**

Add these imports with the existing MUI imports:

```tsx
import IconButton from '@mui/material/IconButton';
import DeleteIcon from '@mui/icons-material/Delete';
import EditIcon from '@mui/icons-material/Edit';
```

Change the component signature and add the header + body Actions cell (keep `'use client'`, the existing MUI table imports, the `Recipe` import, the `orDash` helper, and the existing columns Name/Coffee/Method/Ratio/Water Temp/Favorite unchanged). The component becomes:

```tsx
export function RecipesTable({
  recipes,
  onEdit,
  onDelete,
}: {
  recipes: Recipe[];
  onEdit?: (recipe: Recipe) => void;
  onDelete?: (recipe: Recipe) => void;
}) {
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
            <TableCell>Actions</TableCell>
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
              <TableCell>
                <IconButton aria-label="edit" size="small" onClick={() => onEdit?.(recipe)}>
                  <EditIcon fontSize="small" />
                </IconButton>
                <IconButton aria-label="delete" size="small" onClick={() => onDelete?.(recipe)}>
                  <DeleteIcon fontSize="small" />
                </IconButton>
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </TableContainer>
  );
}
```

- [ ] **Step 2: Add an actions test to `src/components/recipes/RecipesTable.test.tsx`**

Ensure the test imports `fireEvent` and `vi` (add to the existing imports if missing):

```tsx
import { fireEvent, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
```

If the existing render test uses inline recipe objects, hoist a shared `const base: Recipe = { ... }` (the existing fixture) above the `describe` so the new test can reuse it. Append this test inside the `describe` block:

```tsx
it('calls onEdit and onDelete with the row recipe when the action buttons are clicked', () => {
  const onEdit = vi.fn();
  const onDelete = vi.fn();
  renderWithTheme(<RecipesTable recipes={[base]} onEdit={onEdit} onDelete={onDelete} />);

  fireEvent.click(screen.getByRole('button', { name: 'edit' }));
  fireEvent.click(screen.getByRole('button', { name: 'delete' }));

  expect(onEdit).toHaveBeenCalledWith(base);
  expect(onDelete).toHaveBeenCalledWith(base);
});
```

(If the existing fixture is named differently, use that name in place of `base` — the point is to reuse the existing row object so `toHaveBeenCalledWith` matches by reference.)

- [ ] **Step 3: Run the table test**

Run: `npm test -- src/components/recipes/RecipesTable.test.tsx`
Expected: PASS (existing render test + new actions test).

- [ ] **Step 4: Commit (from repo root)**

```bash
cd /Users/jvilla/Documents/brewdeck
git add brewdeck-web/src/components/recipes/RecipesTable.tsx brewdeck-web/src/components/recipes/RecipesTable.test.tsx
git commit -m "feat(web): add edit/delete actions column to RecipesTable"
```

---

### Task 9: Wire dialogs into RecipesView

**Files:**
- Modify: `brewdeck-web/src/components/recipes/RecipesView.tsx`
- Modify: `brewdeck-web/src/components/recipes/RecipesView.test.tsx`

**Interfaces:**
- Consumes: `RecipeFormDialog` (Task 6), `DeleteRecipeDialog` (Task 7), `RecipesTable` with `onEdit`/`onDelete` (Task 8).

- [ ] **Step 1: Update `src/components/recipes/RecipesView.tsx`**

Add these imports with the existing ones:

```tsx
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import { RecipeFormDialog } from './RecipeFormDialog';
import { DeleteRecipeDialog } from './DeleteRecipeDialog';
import type { Recipe } from '@/lib/api/types';
```

Inside the component, after the existing state/hook setup and `handleFiltersChange`, add dialog state:

```tsx
  const [createOpen, setCreateOpen] = useState(false);
  const [editing, setEditing] = useState<Recipe | null>(null);
  const [deleting, setDeleting] = useState<Recipe | null>(null);
```

Where the success branch renders `<RecipesTable recipes={data.content} />`, pass the callbacks:

```tsx
        <RecipesTable
          recipes={data.content}
          onEdit={(recipe) => setEditing(recipe)}
          onDelete={(recipe) => setDeleting(recipe)}
        />
```

Replace the heading area so an "Add Recipe" button sits beside the title, keep `<RecipeFilters>` and `{body}`, and render the dialogs only when open at the end of the fragment:

```tsx
  return (
    <>
      <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', mb: 1 }}>
        <Typography variant="h5" component="h1">
          Recipes
        </Typography>
        <Button variant="contained" onClick={() => setCreateOpen(true)}>
          Add Recipe
        </Button>
      </Box>
      <RecipeFilters value={filters} onChange={handleFiltersChange} />
      {body}

      {createOpen ? <RecipeFormDialog open onClose={() => setCreateOpen(false)} /> : null}
      {editing ? <RecipeFormDialog open recipe={editing} onClose={() => setEditing(null)} /> : null}
      {deleting ? <DeleteRecipeDialog open recipe={deleting} onClose={() => setDeleting(null)} /> : null}
    </>
  );
```

Keep the existing `body` variable (loading/error/empty/success branch) unchanged except for the `onEdit`/`onDelete` props added to `RecipesTable`.

- [ ] **Step 2: Update `src/components/recipes/RecipesView.test.tsx`**

The existing tests mock `@/hooks/useRecipes`. Add module mocks so the dialogs mount without a QueryClient, and add a test for opening the create dialog. Add near the top with the other imports/mocks:

```tsx
vi.mock('@/hooks/useRecipeMutations', () => ({
  useCreateRecipe: () => ({ mutate: vi.fn(), isPending: false }),
  useUpdateRecipe: () => ({ mutate: vi.fn(), isPending: false }),
  useDeleteRecipe: () => ({ mutate: vi.fn(), isPending: false }),
}));
vi.mock('@/hooks/useResourceOptions', () => ({
  useCoffeeOptions: () => ({ data: [], isLoading: false }),
  useMethodOptions: () => ({ data: [], isLoading: false }),
}));
```

Ensure `fireEvent` is imported (`import { fireEvent, screen } from '@testing-library/react';`). Append this test inside the `describe` block (reusing the existing `mockHook`/`page` helpers):

```tsx
it('opens the create dialog when Add Recipe is clicked', () => {
  mockHook({ isLoading: false, isError: false, data: page([], 0) });
  renderWithTheme(<RecipesView />);

  fireEvent.click(screen.getByRole('button', { name: /add recipe/i }));

  expect(screen.getByRole('dialog')).toBeInTheDocument();
  expect(screen.getByText('Add recipe')).toBeInTheDocument();
});
```

- [ ] **Step 3: Run the RecipesView test**

Run: `npm test -- src/components/recipes/RecipesView.test.tsx`
Expected: PASS (existing tests + the new dialog test).

- [ ] **Step 4: Run the full suite and build**

Run (from `brewdeck-web/`): `npm test && npm run build`
Expected: all tests pass; build succeeds.

- [ ] **Step 5: Commit (from repo root)**

```bash
cd /Users/jvilla/Documents/brewdeck
git add brewdeck-web/src/components/recipes/RecipesView.tsx brewdeck-web/src/components/recipes/RecipesView.test.tsx
git commit -m "feat(web): wire create/edit/delete dialogs into the recipes list"
```

---

### Task 10: Manual verification and pull request

**Files:** none (verification + PR).

- [ ] **Step 1: Start the backend (from repo root)**

```bash
cd /Users/jvilla/Documents/brewdeck
docker compose up -d
cd brewdeck-api && sh ./mvnw spring-boot:run
```

Wait for "Started BrewdeckApiApplication". Ensure at least one coffee and one brew method exist (create via the API/Postman if needed).

- [ ] **Step 2: Start the frontend (new shell, from `brewdeck-web/`)**

```bash
cd /Users/jvilla/Documents/brewdeck/brewdeck-web
npm run dev
```

- [ ] **Step 3: Verify in the browser**

Open `http://localhost:3000/recipes`. Expected: "Add Recipe" opens a dialog whose Coffee and Brew Method dropdowns list existing records; submitting without a coffee/method/name shows the required errors; a water temperature of 60 shows the range error; a valid submit creates the recipe and it appears in the list. Each row's Edit opens a prefilled dialog (correct coffee/method preselected) and Save updates it; Delete confirms and removes it. No CORS error in the console.

- [ ] **Step 4: Push the branch and open the PR (from repo root)**

```bash
cd /Users/jvilla/Documents/brewdeck
git push -u origin feature/recipe-crud
```

Then open a PR from `feature/recipe-crud` into `develop`, titled `feat(web): recipe create/edit/delete`, referencing `docs/superpowers/specs/2026-07-05-recipe-crud-design.md`. If `gh` is available, use `gh pr create --base develop --head feature/recipe-crud --title "..." --body-file <path>`; otherwise open the compare URL.

---

## Notes for the implementer

- Run test/build commands from `brewdeck-web/`; run `git` from the repo root. Report FULL-suite totals after each task.
- No new npm dependencies — react-hook-form, zod, @hookform/resolvers, and @testing-library/dom are already installed from the coffee-CRUD slice (on `develop`).
- The recipe schema uses `z.coerce.number` with an `optionalNumber` preprocess so blank optional numeric inputs become `undefined` (not `NaN`/`0`). The form is typed `useForm<z.input<typeof recipeSchema>, unknown, RecipeFormValues>` so `handleSubmit` yields coerced numbers; `toDefaults` returns the input (string-friendly) shape.
- FK dropdowns are native selects (`SelectProps={{ native: true }}`) so `fireEvent.change` sets them in tests and screen readers get real `<option>`s. The two Selects use RHF `Controller`; the rest use `register`.
- Dialogs render only when open in `RecipesView` (mutation/option hooks don't run closed), and are mocked in the `RecipesView` test.
- This branch is based on `develop` (which has the read-only recipes list and the coffee-CRUD-era dependencies).
```
