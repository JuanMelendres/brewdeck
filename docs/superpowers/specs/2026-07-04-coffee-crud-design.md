# BrewDeck Coffee CRUD (first mutation slice) — Design

- **Date:** 2026-07-04
- **Status:** Approved (pending written-spec review)
- **Branch:** `feature/coffee-crud` (off `develop`, which now contains the full backend and all read-only frontend list screens).
- **Scope:** Create, edit, and delete coffees from the coffees list via modal dialogs. First frontend slice with writes/forms/validation. Coffee only — recipe/session mutations are deferred.

## 1. Goal

Introduce the mutation capability the frontend does not yet have: forms, client
validation, and write operations. Coffee is the simplest entity, so it is the
first. On success, mutations invalidate the coffees list so it refetches. This
establishes the reusable form + mutation pattern for later resources.

## 2. Decisions (locked)

- **Scope:** Coffee create + edit + delete, driven from the existing coffees list.
- **Forms:** MUI `Dialog` modals. One `CoffeeFormDialog` handles both create (empty)
  and edit (prefilled). Delete uses a confirm dialog.
- **Forms library:** React Hook Form + Zod (`@hookform/resolvers/zod`).
- **Edit prefill:** from the row's existing `Coffee` object — no GET-by-id call.
- **Mutations:** TanStack Query `useMutation`; `onSuccess` invalidates the coffees
  list query. No optimistic updates (invalidate-and-refetch only).
- **Feedback:** pending disables submit + shows a spinner; error shows an `Alert`;
  success closes the dialog and the list refreshes. No success snackbar (deferred).
- **Stack:** unchanged otherwise — Next.js App Router, TypeScript strict, MUI 9,
  TanStack Query, Vitest + RTL.

## 3. Backend contract (already implemented)

- `POST /api/coffees` with a `CoffeeRequest` body → `201` + `CoffeeResponse`.
- `PUT /api/coffees/{id}` with a `CoffeeRequest` body → `200` + `CoffeeResponse`.
- `DELETE /api/coffees/{id}` → `204 No Content`.
- Validation failures → `400` with `ErrorResponse` (`validationErrors:
  Record<field,message>`). `CoffeeRequest` fields and limits: `name` **required**,
  max 120; `brand`, `origin`, `region`, `farm`, `producer`, `variety` max 120;
  `process`, `roastLevel`, `acidity`, `body`, `sweetness`, `bitterness` max 80;
  `notesPrimary` max 255; `notesSecondary` max 500; `description` max 1000. All
  fields except `name` are optional.

## 4. Dependencies

Add to `brewdeck-web`: `react-hook-form`, `zod`, `@hookform/resolvers`. (Install
with `--legacy-peer-deps`, consistent with the existing setup.)

## 5. Validation schema (`src/lib/validation/coffeeSchema.ts`)

A Zod schema mirroring the backend limits, with optional text fields represented
as optional strings (empty string allowed and sent as-is; the backend treats
blank optionals as null-ish). Example shape:

```ts
export const coffeeSchema = z.object({
  name: z.string().min(1, 'Name is required').max(120, 'Name must not exceed 120 characters'),
  brand: z.string().max(120, 'Brand must not exceed 120 characters').optional(),
  origin: z.string().max(120).optional(),
  region: z.string().max(120).optional(),
  farm: z.string().max(120).optional(),
  producer: z.string().max(120).optional(),
  variety: z.string().max(120).optional(),
  process: z.string().max(80).optional(),
  roastLevel: z.string().max(80).optional(),
  notesPrimary: z.string().max(255).optional(),
  notesSecondary: z.string().max(500).optional(),
  acidity: z.string().max(80).optional(),
  body: z.string().max(80).optional(),
  sweetness: z.string().max(80).optional(),
  bitterness: z.string().max(80).optional(),
  description: z.string().max(1000).optional(),
});

export type CoffeeFormValues = z.infer<typeof coffeeSchema>;
```

Each `max` carries a message; the exact per-field messages are pinned in the plan.

## 6. API module (extend `src/lib/api/coffees.ts`)

```ts
createCoffee(body: CoffeeFormValues): Promise<Coffee>   // POST /api/coffees
updateCoffee(id: number, body: CoffeeFormValues): Promise<Coffee> // PUT /api/coffees/{id}
deleteCoffee(id: number): Promise<void>                 // DELETE /api/coffees/{id}
```

All call `apiFetch` with the correct method. `deleteCoffee` expects a 204 (no body).

## 7. Mutation hooks (`src/hooks/`)

- `useCreateCoffee()`, `useUpdateCoffee()`, `useDeleteCoffee()` — each wraps
  `useMutation` with the matching API function and, in `onSuccess`, calls
  `queryClient.invalidateQueries({ queryKey: ['coffees'] })` so any coffees-list
  query refetches. Return the raw mutation result so callers read
  `isPending`/`error`/`mutate`.

## 8. Components (`src/components/coffees/`)

- **`CoffeeFormDialog.tsx`** — MUI `Dialog` containing a React Hook Form form with
  all writable fields (name is required; the rest optional). Props:
  `open: boolean`, `coffee?: Coffee` (present → edit mode, prefilled; absent →
  create mode), `onClose: () => void`. Uses `zodResolver(coffeeSchema)`. Submit
  runs `useCreateCoffee` or `useUpdateCoffee` by mode; `isPending` disables the
  submit button and shows a spinner. On a server `ApiError` with
  `validationErrors`, map each field message onto the form via `setError`; other
  errors render in an `Alert`. On success, call `onClose()` (the mutation already
  invalidates the list).
- **`DeleteCoffeeDialog.tsx`** — MUI `Dialog` confirm ("Delete coffee ‘{name}’?")
  with Cancel/Delete. Props: `open`, `coffee: Coffee`, `onClose`. Delete runs
  `useDeleteCoffee`; pending disables the button; on success calls `onClose()`.

## 9. Wiring the list

- **`CoffeesView`**: add an "Add Coffee" button above the table that opens the
  create dialog; hold dialog state (`create` open, plus the `Coffee` selected for
  edit/delete). Render `CoffeeFormDialog` and `DeleteCoffeeDialog`.
- **`CoffeesTable`**: add an **Actions** column with Edit and Delete icon buttons
  per row, invoking callbacks passed from `CoffeesView`
  (`onEdit(coffee)` / `onDelete(coffee)`). Existing columns unchanged.

## 10. States and errors

- **Pending:** submit/delete buttons disabled with a spinner.
- **Field validation:** Zod errors shown inline under each field; submit blocked
  until valid.
- **Server errors:** 400 `validationErrors` mapped onto the matching fields; any
  other error shown as a dialog-level `Alert`.
- **Success:** dialog closes; the coffees list refetches via query invalidation.

## 11. Testing

- `coffeeSchema`: rejects empty `name`; accepts a valid minimal object; rejects an
  over-length field (e.g. name > 120) with the right message.
- API: `createCoffee`/`updateCoffee`/`deleteCoffee` call `apiFetch` with the
  correct method, URL, and (for create/update) JSON body (mock `fetch`).
- Mutation hooks: `onSuccess` invalidates `['coffees']` (spy on
  `queryClient.invalidateQueries`).
- `CoffeeFormDialog`: renders fields; submitting empty shows the "Name is required"
  error and does not call the mutation; submitting valid calls the create/update
  mutation with the entered values; a mocked server 400 maps a field error.
- `DeleteCoffeeDialog`: clicking Delete calls the delete mutation with the id.
- `CoffeesView`: the Add button opens the create dialog; a row Edit opens the edit
  dialog prefilled; a row Delete opens the confirm dialog.

Mock the API client / mutation hooks — no real network in tests.

## 12. Out of scope (future specs)

Recipe and brew-session mutations, optimistic updates, a success snackbar/toast,
image upload, bulk actions, and a dedicated create/edit route (this slice uses
dialogs).

## 13. Acceptance criteria

- `npm run build` clean; `npm test` passes (new tests included).
- With the backend running: "Add Coffee" creates a coffee and it appears in the
  list; row Edit updates it; row Delete removes it; each reflects immediately via
  list refetch.
- Client validation blocks an empty name; a backend 400 surfaces on the form.
- Pending, error, and success states are each handled.
