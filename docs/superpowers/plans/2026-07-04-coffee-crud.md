# Coffee CRUD Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add coffee create/edit/delete from the coffees list via MUI modal dialogs with React Hook Form + Zod validation and TanStack Query mutations.

**Architecture:** A Zod schema mirrors the backend `CoffeeRequest`. New API functions (`createCoffee`/`updateCoffee`/`deleteCoffee`) call `apiFetch`. Mutation hooks invalidate the coffees list on success. One `CoffeeFormDialog` (create/edit) and a `DeleteCoffeeDialog` are wired into the existing `CoffeesView`/`CoffeesTable`. Coffee only.

**Tech Stack:** Next.js App Router, TypeScript strict, MUI 9, TanStack Query, React Hook Form, Zod, Vitest + React Testing Library.

## Global Constraints

- App under `brewdeck-web/`, source `brewdeck-web/src/`, alias `@/*` → `src/*`.
- TypeScript strict — no `any` in committed code.
- No `fetch` outside `src/lib/api/client.ts`; API modules call `apiFetch`.
- Client components using hooks/state/MUI start with `'use client'`.
- Backend: `POST /api/coffees` (201), `PUT /api/coffees/{id}` (200), `DELETE /api/coffees/{id}` (204). Validation failures → 400 with `ErrorResponse.validationErrors` (Record<field,message>).
- `CoffeeRequest` limits: `name` required max 120; `brand`/`origin`/`region`/`farm`/`producer`/`variety` max 120; `process`/`roastLevel`/`acidity`/`body`/`sweetness`/`bitterness` max 80; `notesPrimary` max 255; `notesSecondary` max 500; `description` max 1000.
- Mutations invalidate `{ queryKey: ['coffees'] }` on success.
- Run npm commands from `brewdeck-web/`; run `git` from repo root `/Users/jvilla/Documents/brewdeck`. New installs use `--legacy-peer-deps`.
- Conventional Commits; commit at the end of each task.

---

### Task 1: Dependencies and Zod schema

**Files:**
- Modify: `brewdeck-web/package.json` (via npm install)
- Create: `brewdeck-web/src/lib/validation/coffeeSchema.ts`
- Test: `brewdeck-web/src/lib/validation/coffeeSchema.test.ts`

**Interfaces:**
- Produces: `coffeeSchema` (Zod object), `type CoffeeFormValues = z.infer<typeof coffeeSchema>`

- [ ] **Step 1: Install form dependencies (from `brewdeck-web/`)**

```bash
cd brewdeck-web
npm install --legacy-peer-deps react-hook-form zod @hookform/resolvers
```

- [ ] **Step 2: Write the failing test — `src/lib/validation/coffeeSchema.test.ts`**

```ts
import { describe, expect, it } from 'vitest';
import { coffeeSchema } from './coffeeSchema';

describe('coffeeSchema', () => {
  it('rejects an empty name', () => {
    const result = coffeeSchema.safeParse({ name: '' });
    expect(result.success).toBe(false);
    if (!result.success) {
      expect(result.error.issues[0].message).toBe('Name is required');
    }
  });

  it('accepts a minimal valid object (name only)', () => {
    const result = coffeeSchema.safeParse({ name: 'Mezcla Veracruz' });
    expect(result.success).toBe(true);
  });

  it('rejects a name longer than 120 characters', () => {
    const result = coffeeSchema.safeParse({ name: 'A'.repeat(121) });
    expect(result.success).toBe(false);
    if (!result.success) {
      expect(result.error.issues[0].message).toBe('Name must not exceed 120 characters');
    }
  });
});
```

- [ ] **Step 3: Run test to verify it fails**

Run: `npm test -- src/lib/validation/coffeeSchema.test.ts`
Expected: FAIL (cannot find `./coffeeSchema`).

- [ ] **Step 4: Create `src/lib/validation/coffeeSchema.ts`**

```ts
import { z } from 'zod';

export const coffeeSchema = z.object({
  name: z
    .string()
    .min(1, 'Name is required')
    .max(120, 'Name must not exceed 120 characters'),
  brand: z.string().max(120, 'Brand must not exceed 120 characters').optional(),
  origin: z.string().max(120, 'Origin must not exceed 120 characters').optional(),
  region: z.string().max(120, 'Region must not exceed 120 characters').optional(),
  farm: z.string().max(120, 'Farm must not exceed 120 characters').optional(),
  producer: z.string().max(120, 'Producer must not exceed 120 characters').optional(),
  variety: z.string().max(120, 'Variety must not exceed 120 characters').optional(),
  process: z.string().max(80, 'Process must not exceed 80 characters').optional(),
  roastLevel: z.string().max(80, 'Roast level must not exceed 80 characters').optional(),
  notesPrimary: z.string().max(255, 'Primary notes must not exceed 255 characters').optional(),
  notesSecondary: z.string().max(500, 'Secondary notes must not exceed 500 characters').optional(),
  acidity: z.string().max(80, 'Acidity must not exceed 80 characters').optional(),
  body: z.string().max(80, 'Body must not exceed 80 characters').optional(),
  sweetness: z.string().max(80, 'Sweetness must not exceed 80 characters').optional(),
  bitterness: z.string().max(80, 'Bitterness must not exceed 80 characters').optional(),
  description: z.string().max(1000, 'Description must not exceed 1000 characters').optional(),
});

export type CoffeeFormValues = z.infer<typeof coffeeSchema>;
```

- [ ] **Step 5: Run test to verify it passes**

Run: `npm test -- src/lib/validation/coffeeSchema.test.ts`
Expected: PASS (3 tests).

- [ ] **Step 6: Commit (from repo root)**

```bash
cd /Users/jvilla/Documents/brewdeck
git add brewdeck-web/package.json brewdeck-web/package-lock.json brewdeck-web/src/lib/validation/coffeeSchema.ts brewdeck-web/src/lib/validation/coffeeSchema.test.ts
git commit -m "feat(web): add react-hook-form, zod and the coffee form schema"
```

---

### Task 2: Coffee write API functions

**Files:**
- Modify: `brewdeck-web/src/lib/api/coffees.ts` (append)
- Test: `brewdeck-web/src/lib/api/coffeesMutations.test.ts`

**Interfaces:**
- Consumes: `CoffeeFormValues` (Task 1), `Coffee` (existing).
- Produces:
  - `createCoffee(body: CoffeeFormValues): Promise<Coffee>` — POST `/api/coffees`
  - `updateCoffee(id: number, body: CoffeeFormValues): Promise<Coffee>` — PUT `/api/coffees/{id}`
  - `deleteCoffee(id: number): Promise<void>` — DELETE `/api/coffees/{id}`

- [ ] **Step 1: Write the failing test — `src/lib/api/coffeesMutations.test.ts`**

```ts
import { afterEach, describe, expect, it, vi } from 'vitest';
import { createCoffee, updateCoffee, deleteCoffee } from './coffees';

function stubFetch() {
  const fetchMock = vi.fn().mockResolvedValue({
    ok: true,
    status: 200,
    json: () => Promise.resolve({ id: 1, name: 'Mezcla' }),
  });
  vi.stubGlobal('fetch', fetchMock);
  return fetchMock;
}

afterEach(() => vi.unstubAllGlobals());

describe('coffee write API', () => {
  it('createCoffee POSTs the body to /api/coffees', async () => {
    const fetchMock = stubFetch();
    await createCoffee({ name: 'Mezcla' });
    const [url, init] = fetchMock.mock.calls[0];
    expect(String(url)).toContain('/api/coffees');
    expect(init.method).toBe('POST');
    expect(JSON.parse(init.body)).toEqual({ name: 'Mezcla' });
  });

  it('updateCoffee PUTs the body to /api/coffees/{id}', async () => {
    const fetchMock = stubFetch();
    await updateCoffee(7, { name: 'Updated' });
    const [url, init] = fetchMock.mock.calls[0];
    expect(String(url)).toContain('/api/coffees/7');
    expect(init.method).toBe('PUT');
    expect(JSON.parse(init.body)).toEqual({ name: 'Updated' });
  });

  it('deleteCoffee DELETEs /api/coffees/{id}', async () => {
    const fetchMock = stubFetch();
    await deleteCoffee(7);
    const [url, init] = fetchMock.mock.calls[0];
    expect(String(url)).toContain('/api/coffees/7');
    expect(init.method).toBe('DELETE');
  });
});
```

- [ ] **Step 2: Run test to verify it fails**

Run: `npm test -- src/lib/api/coffeesMutations.test.ts`
Expected: FAIL (createCoffee/updateCoffee/deleteCoffee not exported).

- [ ] **Step 3: Append to `src/lib/api/coffees.ts`**

Add this import at the top (with the existing imports) and the three functions at the end of the file:

```ts
import type { CoffeeFormValues } from '@/lib/validation/coffeeSchema';
```

```ts
export function createCoffee(body: CoffeeFormValues): Promise<Coffee> {
  return apiFetch<Coffee>('/api/coffees', {
    method: 'POST',
    body: JSON.stringify(body),
  });
}

export function updateCoffee(id: number, body: CoffeeFormValues): Promise<Coffee> {
  return apiFetch<Coffee>(`/api/coffees/${id}`, {
    method: 'PUT',
    body: JSON.stringify(body),
  });
}

export function deleteCoffee(id: number): Promise<void> {
  return apiFetch<void>(`/api/coffees/${id}`, { method: 'DELETE' });
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `npm test -- src/lib/api/coffeesMutations.test.ts`
Expected: PASS (3 tests).

- [ ] **Step 5: Commit (from repo root)**

```bash
cd /Users/jvilla/Documents/brewdeck
git add brewdeck-web/src/lib/api/coffees.ts brewdeck-web/src/lib/api/coffeesMutations.test.ts
git commit -m "feat(web): add coffee create/update/delete API functions"
```

---

### Task 3: Coffee mutation hooks

**Files:**
- Create: `brewdeck-web/src/hooks/useCoffeeMutations.ts`
- Test: `brewdeck-web/src/hooks/useCoffeeMutations.test.tsx`

**Interfaces:**
- Consumes: `createCoffee`/`updateCoffee`/`deleteCoffee` (Task 2), `CoffeeFormValues` (Task 1).
- Produces: `useCreateCoffee()`, `useUpdateCoffee()`, `useDeleteCoffee()` — TanStack `useMutation` results. `useUpdateCoffee().mutate` takes `{ id: number; body: CoffeeFormValues }`; create takes `CoffeeFormValues`; delete takes `number`.

- [ ] **Step 1: Write the failing test — `src/hooks/useCoffeeMutations.test.tsx`**

```tsx
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { renderHook, waitFor } from '@testing-library/react';
import type { ReactNode } from 'react';
import { describe, expect, it, vi } from 'vitest';
import { useCreateCoffee, useDeleteCoffee } from './useCoffeeMutations';
import * as coffeesApi from '@/lib/api/coffees';

function setup() {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  const invalidateSpy = vi.spyOn(client, 'invalidateQueries');
  const wrapper = ({ children }: { children: ReactNode }) => (
    <QueryClientProvider client={client}>{children}</QueryClientProvider>
  );
  return { wrapper, invalidateSpy };
}

describe('coffee mutation hooks', () => {
  it('useCreateCoffee invalidates the coffees list on success', async () => {
    vi.spyOn(coffeesApi, 'createCoffee').mockResolvedValue({ id: 1, name: 'Mezcla' } as never);
    const { wrapper, invalidateSpy } = setup();

    const { result } = renderHook(() => useCreateCoffee(), { wrapper });
    result.current.mutate({ name: 'Mezcla' });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(invalidateSpy).toHaveBeenCalledWith({ queryKey: ['coffees'] });
  });

  it('useDeleteCoffee invalidates the coffees list on success', async () => {
    vi.spyOn(coffeesApi, 'deleteCoffee').mockResolvedValue(undefined);
    const { wrapper, invalidateSpy } = setup();

    const { result } = renderHook(() => useDeleteCoffee(), { wrapper });
    result.current.mutate(1);

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(invalidateSpy).toHaveBeenCalledWith({ queryKey: ['coffees'] });
  });
});
```

- [ ] **Step 2: Run test to verify it fails**

Run: `npm test -- src/hooks/useCoffeeMutations.test.tsx`
Expected: FAIL (cannot find `./useCoffeeMutations`).

- [ ] **Step 3: Create `src/hooks/useCoffeeMutations.ts`**

```ts
'use client';

import { useMutation, useQueryClient } from '@tanstack/react-query';
import { createCoffee, deleteCoffee, updateCoffee } from '@/lib/api/coffees';
import type { CoffeeFormValues } from '@/lib/validation/coffeeSchema';

export function useCreateCoffee() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (body: CoffeeFormValues) => createCoffee(body),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['coffees'] }),
  });
}

export function useUpdateCoffee() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, body }: { id: number; body: CoffeeFormValues }) => updateCoffee(id, body),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['coffees'] }),
  });
}

export function useDeleteCoffee() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => deleteCoffee(id),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['coffees'] }),
  });
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `npm test -- src/hooks/useCoffeeMutations.test.tsx`
Expected: PASS (2 tests).

- [ ] **Step 5: Commit (from repo root)**

```bash
cd /Users/jvilla/Documents/brewdeck
git add brewdeck-web/src/hooks/useCoffeeMutations.ts brewdeck-web/src/hooks/useCoffeeMutations.test.tsx
git commit -m "feat(web): add coffee mutation hooks that invalidate the list"
```

---

### Task 4: CoffeeFormDialog

**Files:**
- Create: `brewdeck-web/src/components/coffees/CoffeeFormDialog.tsx`
- Test: `brewdeck-web/src/components/coffees/CoffeeFormDialog.test.tsx`

**Interfaces:**
- Consumes: `coffeeSchema`/`CoffeeFormValues` (Task 1), `useCreateCoffee`/`useUpdateCoffee` (Task 3), `ApiError` (existing `@/lib/api/client`), `Coffee` (existing).
- Produces: `CoffeeFormDialog({ open, coffee, onClose }: { open: boolean; coffee?: Coffee; onClose: () => void })`.

- [ ] **Step 1: Write the failing test — `src/components/coffees/CoffeeFormDialog.test.tsx`**

```tsx
import { fireEvent, screen, waitFor } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { renderWithTheme } from '@/test/renderWithTheme';
import { CoffeeFormDialog } from './CoffeeFormDialog';
import * as mutations from '@/hooks/useCoffeeMutations';
import { ApiError } from '@/lib/api/client';

const createMutate = vi.fn();
const updateMutate = vi.fn();

function mockHooks() {
  vi.spyOn(mutations, 'useCreateCoffee').mockReturnValue({ mutate: createMutate, isPending: false } as never);
  vi.spyOn(mutations, 'useUpdateCoffee').mockReturnValue({ mutate: updateMutate, isPending: false } as never);
}

beforeEach(() => {
  createMutate.mockReset();
  updateMutate.mockReset();
});

afterEach(() => vi.restoreAllMocks());

describe('CoffeeFormDialog', () => {
  it('blocks submit and shows a required error when name is empty', async () => {
    mockHooks();
    renderWithTheme(<CoffeeFormDialog open onClose={vi.fn()} />);

    fireEvent.click(screen.getByRole('button', { name: /create/i }));

    expect(await screen.findByText('Name is required')).toBeInTheDocument();
    expect(createMutate).not.toHaveBeenCalled();
  });

  it('calls the create mutation with the entered values on valid submit', async () => {
    mockHooks();
    renderWithTheme(<CoffeeFormDialog open onClose={vi.fn()} />);

    fireEvent.change(screen.getByLabelText(/^Name/), { target: { value: 'Mezcla' } });
    fireEvent.click(screen.getByRole('button', { name: /create/i }));

    await waitFor(() => expect(createMutate).toHaveBeenCalledTimes(1));
    expect(createMutate.mock.calls[0][0]).toEqual(expect.objectContaining({ name: 'Mezcla' }));
  });

  it('maps a server 400 validation error onto the field', async () => {
    mockHooks();
    createMutate.mockImplementation((_body, opts) => {
      opts.onError(new ApiError(400, 'Validation failed', '/api/coffees', { name: 'Coffee name is required' }));
    });
    renderWithTheme(<CoffeeFormDialog open onClose={vi.fn()} />);

    fireEvent.change(screen.getByLabelText(/^Name/), { target: { value: 'X' } });
    fireEvent.click(screen.getByRole('button', { name: /create/i }));

    expect(await screen.findByText('Coffee name is required')).toBeInTheDocument();
  });
});
```

- [ ] **Step 2: Run test to verify it fails**

Run: `npm test -- src/components/coffees/CoffeeFormDialog.test.tsx`
Expected: FAIL (cannot find `./CoffeeFormDialog`).

- [ ] **Step 3: Create `src/components/coffees/CoffeeFormDialog.tsx`**

```tsx
'use client';

import Alert from '@mui/material/Alert';
import Button from '@mui/material/Button';
import CircularProgress from '@mui/material/CircularProgress';
import Dialog from '@mui/material/Dialog';
import DialogActions from '@mui/material/DialogActions';
import DialogContent from '@mui/material/DialogContent';
import DialogTitle from '@mui/material/DialogTitle';
import Stack from '@mui/material/Stack';
import TextField from '@mui/material/TextField';
import { zodResolver } from '@hookform/resolvers/zod';
import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { ApiError } from '@/lib/api/client';
import { coffeeSchema, type CoffeeFormValues } from '@/lib/validation/coffeeSchema';
import { useCreateCoffee, useUpdateCoffee } from '@/hooks/useCoffeeMutations';
import type { Coffee } from '@/lib/api/types';

const FIELDS: Array<{ name: keyof CoffeeFormValues; label: string }> = [
  { name: 'name', label: 'Name' },
  { name: 'brand', label: 'Brand' },
  { name: 'origin', label: 'Origin' },
  { name: 'region', label: 'Region' },
  { name: 'farm', label: 'Farm' },
  { name: 'producer', label: 'Producer' },
  { name: 'variety', label: 'Variety' },
  { name: 'process', label: 'Process' },
  { name: 'roastLevel', label: 'Roast Level' },
  { name: 'notesPrimary', label: 'Primary Notes' },
  { name: 'notesSecondary', label: 'Secondary Notes' },
  { name: 'acidity', label: 'Acidity' },
  { name: 'body', label: 'Body' },
  { name: 'sweetness', label: 'Sweetness' },
  { name: 'bitterness', label: 'Bitterness' },
  { name: 'description', label: 'Description' },
];

function toDefaults(coffee?: Coffee): CoffeeFormValues {
  return {
    name: coffee?.name ?? '',
    brand: coffee?.brand ?? '',
    origin: coffee?.origin ?? '',
    region: coffee?.region ?? '',
    farm: coffee?.farm ?? '',
    producer: coffee?.producer ?? '',
    variety: coffee?.variety ?? '',
    process: coffee?.process ?? '',
    roastLevel: coffee?.roastLevel ?? '',
    notesPrimary: coffee?.notesPrimary ?? '',
    notesSecondary: coffee?.notesSecondary ?? '',
    acidity: coffee?.acidity ?? '',
    body: coffee?.body ?? '',
    sweetness: coffee?.sweetness ?? '',
    bitterness: coffee?.bitterness ?? '',
    description: coffee?.description ?? '',
  };
}

export function CoffeeFormDialog({
  open,
  coffee,
  onClose,
}: {
  open: boolean;
  coffee?: Coffee;
  onClose: () => void;
}) {
  const isEdit = coffee !== undefined;
  const create = useCreateCoffee();
  const update = useUpdateCoffee();
  const [serverError, setServerError] = useState<string | null>(null);

  const {
    register,
    handleSubmit,
    setError,
    formState: { errors },
  } = useForm<CoffeeFormValues>({
    resolver: zodResolver(coffeeSchema),
    values: toDefaults(coffee),
  });

  const pending = create.isPending || update.isPending;

  const onSubmit = (data: CoffeeFormValues) => {
    setServerError(null);
    const options = {
      onSuccess: () => onClose(),
      onError: (error: unknown) => {
        if (error instanceof ApiError && error.validationErrors) {
          Object.entries(error.validationErrors).forEach(([field, message]) =>
            setError(field as keyof CoffeeFormValues, { message }),
          );
        } else {
          setServerError(error instanceof Error ? error.message : 'Something went wrong');
        }
      },
    };
    if (isEdit && coffee) {
      update.mutate({ id: coffee.id, body: data }, options);
    } else {
      create.mutate(data, options);
    }
  };

  return (
    <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
      <DialogTitle>{isEdit ? 'Edit coffee' : 'Add coffee'}</DialogTitle>
      <form onSubmit={handleSubmit(onSubmit)} noValidate>
        <DialogContent>
          {serverError ? (
            <Alert severity="error" sx={{ mb: 2 }}>
              {serverError}
            </Alert>
          ) : null}
          <Stack spacing={2}>
            {FIELDS.map((field) => (
              <TextField
                key={field.name}
                label={field.label}
                required={field.name === 'name'}
                size="small"
                fullWidth
                error={Boolean(errors[field.name])}
                helperText={errors[field.name]?.message}
                {...register(field.name)}
              />
            ))}
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

Run: `npm test -- src/components/coffees/CoffeeFormDialog.test.tsx`
Expected: PASS (3 tests).

- [ ] **Step 5: Commit (from repo root)**

```bash
cd /Users/jvilla/Documents/brewdeck
git add brewdeck-web/src/components/coffees/CoffeeFormDialog.tsx brewdeck-web/src/components/coffees/CoffeeFormDialog.test.tsx
git commit -m "feat(web): add CoffeeFormDialog for create and edit"
```

---

### Task 5: DeleteCoffeeDialog

**Files:**
- Create: `brewdeck-web/src/components/coffees/DeleteCoffeeDialog.tsx`
- Test: `brewdeck-web/src/components/coffees/DeleteCoffeeDialog.test.tsx`

**Interfaces:**
- Consumes: `useDeleteCoffee` (Task 3), `Coffee` (existing).
- Produces: `DeleteCoffeeDialog({ open, coffee, onClose }: { open: boolean; coffee: Coffee; onClose: () => void })`.

- [ ] **Step 1: Write the failing test — `src/components/coffees/DeleteCoffeeDialog.test.tsx`**

```tsx
import { fireEvent, screen } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { renderWithTheme } from '@/test/renderWithTheme';
import { DeleteCoffeeDialog } from './DeleteCoffeeDialog';
import * as mutations from '@/hooks/useCoffeeMutations';
import type { Coffee } from '@/lib/api/types';

const deleteMutate = vi.fn();

const coffee: Coffee = {
  id: 7, name: 'Mezcla', brand: null, origin: null, region: null, farm: null, producer: null,
  variety: null, process: null, roastLevel: null, notesPrimary: null, notesSecondary: null,
  acidity: null, body: null, sweetness: null, bitterness: null, description: null,
  createdAt: '2026-01-01T00:00:00', updatedAt: null,
};

afterEach(() => vi.restoreAllMocks());

describe('DeleteCoffeeDialog', () => {
  it('calls the delete mutation with the coffee id when confirmed', () => {
    vi.spyOn(mutations, 'useDeleteCoffee').mockReturnValue({ mutate: deleteMutate, isPending: false } as never);
    renderWithTheme(<DeleteCoffeeDialog open coffee={coffee} onClose={vi.fn()} />);

    fireEvent.click(screen.getByRole('button', { name: /^delete$/i }));

    expect(deleteMutate.mock.calls[0][0]).toBe(7);
  });
});
```

- [ ] **Step 2: Run test to verify it fails**

Run: `npm test -- src/components/coffees/DeleteCoffeeDialog.test.tsx`
Expected: FAIL (cannot find `./DeleteCoffeeDialog`).

- [ ] **Step 3: Create `src/components/coffees/DeleteCoffeeDialog.tsx`**

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
import { useDeleteCoffee } from '@/hooks/useCoffeeMutations';
import type { Coffee } from '@/lib/api/types';

export function DeleteCoffeeDialog({
  open,
  coffee,
  onClose,
}: {
  open: boolean;
  coffee: Coffee;
  onClose: () => void;
}) {
  const del = useDeleteCoffee();
  const [error, setError] = useState<string | null>(null);

  const onConfirm = () => {
    setError(null);
    del.mutate(coffee.id, {
      onSuccess: () => onClose(),
      onError: (e: unknown) => setError(e instanceof Error ? e.message : 'Something went wrong'),
    });
  };

  return (
    <Dialog open={open} onClose={onClose} maxWidth="xs" fullWidth>
      <DialogTitle>Delete coffee</DialogTitle>
      <DialogContent>
        {error ? (
          <Alert severity="error" sx={{ mb: 2 }}>
            {error}
          </Alert>
        ) : null}
        <DialogContentText>
          Delete coffee &ldquo;{coffee.name}&rdquo;? This cannot be undone.
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

Run: `npm test -- src/components/coffees/DeleteCoffeeDialog.test.tsx`
Expected: PASS.

- [ ] **Step 5: Commit (from repo root)**

```bash
cd /Users/jvilla/Documents/brewdeck
git add brewdeck-web/src/components/coffees/DeleteCoffeeDialog.tsx brewdeck-web/src/components/coffees/DeleteCoffeeDialog.test.tsx
git commit -m "feat(web): add DeleteCoffeeDialog confirm"
```

---

### Task 6: CoffeesTable actions column

**Files:**
- Modify: `brewdeck-web/src/components/coffees/CoffeesTable.tsx`
- Modify: `brewdeck-web/src/components/coffees/CoffeesTable.test.tsx`

**Interfaces:**
- Consumes: `Coffee` (existing).
- Produces: `CoffeesTable({ coffees, onEdit, onDelete }: { coffees: Coffee[]; onEdit?: (c: Coffee) => void; onDelete?: (c: Coffee) => void })` — the two callbacks are optional so existing callers keep compiling; the Actions column always renders and calls them optionally.

- [ ] **Step 1: Update the props and add the Actions column in `src/components/coffees/CoffeesTable.tsx`**

Add these imports with the existing MUI imports:

```tsx
import IconButton from '@mui/material/IconButton';
import DeleteIcon from '@mui/icons-material/Delete';
import EditIcon from '@mui/icons-material/Edit';
```

Change the component signature and add the header + body Actions cell. The full component body becomes:

```tsx
export function CoffeesTable({
  coffees,
  onEdit,
  onDelete,
}: {
  coffees: Coffee[];
  onEdit?: (coffee: Coffee) => void;
  onDelete?: (coffee: Coffee) => void;
}) {
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
            <TableCell>Actions</TableCell>
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
              <TableCell>
                <IconButton aria-label="edit" size="small" onClick={() => onEdit?.(coffee)}>
                  <EditIcon fontSize="small" />
                </IconButton>
                <IconButton aria-label="delete" size="small" onClick={() => onDelete?.(coffee)}>
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

Keep the existing `'use client'` directive, the MUI table imports, the `Coffee` import, and the `orDash` helper unchanged.

- [ ] **Step 2: Add an actions test to `src/components/coffees/CoffeesTable.test.tsx`**

Append this test inside the existing `describe` block (keep the existing rendering test as-is — it does not pass the optional callbacks, which is fine):

```tsx
it('calls onEdit and onDelete with the row coffee when the action buttons are clicked', () => {
  const onEdit = vi.fn();
  const onDelete = vi.fn();
  renderWithTheme(<CoffeesTable coffees={[coffee]} onEdit={onEdit} onDelete={onDelete} />);

  fireEvent.click(screen.getByRole('button', { name: 'edit' }));
  fireEvent.click(screen.getByRole('button', { name: 'delete' }));

  expect(onEdit).toHaveBeenCalledWith(coffee);
  expect(onDelete).toHaveBeenCalledWith(coffee);
});
```

Ensure the test file imports `fireEvent` and `vi` (add them to the existing imports if missing):

```tsx
import { fireEvent, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
```

The existing test's fixture `coffee` object is reused; if the existing test uses an inline object rather than a shared `coffee` const, hoist it to a shared `const coffee: Coffee = { ... }` above the `describe` so both tests reference it.

- [ ] **Step 3: Run the table test**

Run: `npm test -- src/components/coffees/CoffeesTable.test.tsx`
Expected: PASS (existing render test + the new actions test).

- [ ] **Step 4: Commit (from repo root)**

```bash
cd /Users/jvilla/Documents/brewdeck
git add brewdeck-web/src/components/coffees/CoffeesTable.tsx brewdeck-web/src/components/coffees/CoffeesTable.test.tsx
git commit -m "feat(web): add edit/delete actions column to CoffeesTable"
```

---

### Task 7: Wire dialogs into CoffeesView

**Files:**
- Modify: `brewdeck-web/src/components/coffees/CoffeesView.tsx`
- Modify: `brewdeck-web/src/components/coffees/CoffeesView.test.tsx`

**Interfaces:**
- Consumes: `CoffeeFormDialog` (Task 4), `DeleteCoffeeDialog` (Task 5), `CoffeesTable` with `onEdit`/`onDelete` (Task 6).
- Produces: an "Add Coffee" button and dialog state in `CoffeesView`; the table rows can edit/delete.

- [ ] **Step 1: Update `src/components/coffees/CoffeesView.tsx`**

Add these imports with the existing ones:

```tsx
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import { CoffeeFormDialog } from './CoffeeFormDialog';
import { DeleteCoffeeDialog } from './DeleteCoffeeDialog';
import type { Coffee } from '@/lib/api/types';
```

Inside the component, after the existing `filters`/`debouncedFilters`/`useCoffees` setup and `handleFiltersChange`, add dialog state:

```tsx
  const [createOpen, setCreateOpen] = useState(false);
  const [editing, setEditing] = useState<Coffee | null>(null);
  const [deleting, setDeleting] = useState<Coffee | null>(null);
```

Where the success branch renders `<CoffeesTable coffees={data.content} />`, pass the callbacks:

```tsx
        <CoffeesTable
          coffees={data.content}
          onEdit={(coffee) => setEditing(coffee)}
          onDelete={(coffee) => setDeleting(coffee)}
        />
```

Replace the top of the returned JSX (the `<Typography>` heading area) so an "Add Coffee" button sits beside the title, and render the dialogs at the end of the fragment:

```tsx
  return (
    <>
      <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', mb: 1 }}>
        <Typography variant="h5" component="h1">
          Coffees
        </Typography>
        <Button variant="contained" onClick={() => setCreateOpen(true)}>
          Add Coffee
        </Button>
      </Box>
      <CoffeeFilters value={filters} onChange={handleFiltersChange} />
      {body}

      {createOpen ? (
        <CoffeeFormDialog open onClose={() => setCreateOpen(false)} />
      ) : null}
      {editing ? (
        <CoffeeFormDialog open coffee={editing} onClose={() => setEditing(null)} />
      ) : null}
      {deleting ? (
        <DeleteCoffeeDialog open coffee={deleting} onClose={() => setDeleting(null)} />
      ) : null}
    </>
  );
```

Keep the existing `body` variable (the loading/error/empty/success branch) exactly as it is, except for adding the `onEdit`/`onDelete` props to `CoffeesTable` shown above. Note the dialogs are rendered only when open, so their mutation hooks (which need a QueryClient) do not run in the closed state.

- [ ] **Step 2: Update `src/components/coffees/CoffeesView.test.tsx`**

The existing tests mock `@/hooks/useCoffees`. Add a module mock for the mutation hooks so the dialogs can mount without a QueryClient, and add one test for opening the create dialog. At the top of the file (with the other imports and mocks), add:

```tsx
vi.mock('@/hooks/useCoffeeMutations', () => ({
  useCreateCoffee: () => ({ mutate: vi.fn(), isPending: false }),
  useUpdateCoffee: () => ({ mutate: vi.fn(), isPending: false }),
  useDeleteCoffee: () => ({ mutate: vi.fn(), isPending: false }),
}));
```

Append this test inside the existing `describe` block:

```tsx
it('opens the create dialog when Add Coffee is clicked', () => {
  mockHook({ isLoading: false, isError: false, data: page([], 0) });
  renderWithTheme(<CoffeesView />);

  fireEvent.click(screen.getByRole('button', { name: /add coffee/i }));

  expect(screen.getByRole('dialog')).toBeInTheDocument();
  expect(screen.getByText('Add coffee')).toBeInTheDocument();
});
```

Ensure `fireEvent` is imported (`import { fireEvent, screen } from '@testing-library/react';`). The existing `mockHook`/`page` helpers are reused.

- [ ] **Step 3: Run the CoffeesView test**

Run: `npm test -- src/components/coffees/CoffeesView.test.tsx`
Expected: PASS (existing 5 tests + the new dialog test).

- [ ] **Step 4: Run the full suite and build**

Run (from `brewdeck-web/`): `npm test && npm run build`
Expected: all tests pass; build succeeds.

- [ ] **Step 5: Commit (from repo root)**

```bash
cd /Users/jvilla/Documents/brewdeck
git add brewdeck-web/src/components/coffees/CoffeesView.tsx brewdeck-web/src/components/coffees/CoffeesView.test.tsx
git commit -m "feat(web): wire create/edit/delete dialogs into the coffees list"
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

Wait for "Started BrewdeckApiApplication".

- [ ] **Step 2: Start the frontend (new shell, from `brewdeck-web/`)**

```bash
cd /Users/jvilla/Documents/brewdeck/brewdeck-web
npm run dev
```

- [ ] **Step 3: Verify in the browser**

Open `http://localhost:3000/coffees`. Expected: an "Add Coffee" button; clicking it opens a dialog; submitting with a blank name shows "Name is required"; entering a name and Create adds the coffee and it appears in the list (list refetches). Each row has edit/delete icons: Edit opens a prefilled dialog and Save updates the row; Delete opens a confirm and removes the row. No CORS error in the console.

- [ ] **Step 4: Push the branch and open the PR (from repo root)**

```bash
cd /Users/jvilla/Documents/brewdeck
git push -u origin feature/coffee-crud
```

Then open a PR from `feature/coffee-crud` into `develop`, titled `feat(web): coffee create/edit/delete`, referencing `docs/superpowers/specs/2026-07-04-coffee-crud-design.md`. If `gh` is available, use `gh pr create --base develop --head feature/coffee-crud --title "..." --body-file <path>`; otherwise open the compare URL.

---

## Notes for the implementer

- Run test/build commands from `brewdeck-web/`; run `git` from the repo root.
- New deps (`react-hook-form`, `zod`, `@hookform/resolvers`) install with `--legacy-peer-deps`, matching the existing setup.
- The dialogs use the mutation hooks, which need a `QueryClient`. In `CoffeesView` they render only when open; in tests they are mocked. `CoffeeFormDialog`/`DeleteCoffeeDialog` own tests mock the mutation hooks directly.
- `useForm({ values: toDefaults(coffee) })` (not `defaultValues`) so the form re-syncs when a different row is edited or the create dialog opens empty.
- The form submits optional fields as empty strings; the backend accepts blank optionals. Do not convert to null in this slice.
- This branch is based on the latest `develop` (which contains all four read-only list screens).
