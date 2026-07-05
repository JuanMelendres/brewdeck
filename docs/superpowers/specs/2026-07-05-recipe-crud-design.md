# BrewDeck Recipe CRUD — Design

- **Date:** 2026-07-05
- **Status:** Approved (pending written-spec review)
- **Branch:** `feature/recipe-crud` (off `develop`).
- **Scope:** Create, edit, and delete recipes from the recipes list via modal
  dialogs. Mirrors the coffee-CRUD pattern, adding foreign-key Select dropdowns
  (coffee, brew method) and numeric fields. Recipe only — session CRUD is a
  separate, later slice.

## 1. Goal

Extend the mutation pattern (RHF + Zod + TanStack Query mutations + MUI dialogs,
established in the coffee-CRUD slice) to recipes. The new element is
foreign-key selection: a recipe references a coffee and a brew method, so the
form needs Select dropdowns populated from those resources.

## 2. Decisions (locked)

- **Scope:** recipe create + edit + delete from the recipes list.
- **Forms:** MUI `Dialog` modals; one `RecipeFormDialog` for create (empty) and
  edit (prefilled); `DeleteRecipeDialog` confirm. Same pattern as coffee CRUD.
- **Dropdowns:** lightweight options hooks fetch the first 100 of each resource
  (`size:100`) and map to `{ id, name }`; plain MUI `Select`s. (100-cap is
  acceptable for now; typeahead is a later upgrade.)
- **Numeric fields:** `coffeeGrams`, `waterGrams`, `waterTemp` use
  `z.coerce.number()` because RHF number inputs emit strings.
- **Mutations:** invalidate `{ queryKey: ['recipes'] }` on success. No optimistic
  updates.
- **Validation messages:** avoid special symbols — water-temperature messages use
  "degrees Celsius", not the degree symbol (backend sanitizes responses).
- **Stack:** unchanged — Next.js App Router, TS strict, MUI 9, TanStack Query,
  React Hook Form, Zod, Vitest + RTL.

## 3. Backend contract (already implemented)

- `POST /api/recipes` (201), `PUT /api/recipes/{id}` (200), `DELETE
  /api/recipes/{id}` (204). 400 → `ErrorResponse.validationErrors`.
- `RecipeRequest` fields/limits: `coffeeId` **required**; `methodId` **required**;
  `name` **required** max 120; `coffeeGrams` positive (> 0); `waterGrams` positive;
  `ratio` max 20; `grindSetting` max 120; `waterTemp` between 70 and 100 (degrees
  Celsius); `brewTime` max 20; `steps` max 1000; `expectedTaste` max 500;
  `favorite` boolean. All except `coffeeId`/`methodId`/`name` are optional.
- `GET /api/coffees` and `GET /api/brew-methods` return `PageResponse<…>` (0-based,
  `size` capped at 100). `BrewMethodResponse` has `id`, `name`, `description`,
  `createdAt`, `updatedAt`.
- `Recipe` (frontend type, already present) carries `coffeeId`, `methodId`, plus
  the denormalized `coffeeName`/`methodName` — used to prefill the edit form.

## 4. Brew-methods module and options hooks

- `src/lib/api/brewMethods.ts`: `type BrewMethod` (id, name, description|null,
  createdAt, updatedAt|null) and `listBrewMethods(params: { page: number; size:
  number }): Promise<PageResponse<BrewMethod>>` via `apiFetch`.
- `src/hooks/useCoffeeOptions.ts`: `useQuery(['coffees','options'], () =>
  listCoffees({ page: 0, size: 100 }))`, returning the query result; a small
  selector maps `data.content` to `{ id, name }[]`.
- `src/hooks/useMethodOptions.ts`: same against `listBrewMethods`, key
  `['brew-methods','options']`.

## 5. Validation schema (`src/lib/validation/recipeSchema.ts`)

Zod schema mirroring the limits. Numeric fields coerce from strings; optional
numbers accept empty via `.optional()` on a coerced number that treats `''` as
undefined. Shape:

```ts
export const recipeSchema = z.object({
  coffeeId: z.coerce.number({ message: 'Coffee is required' }).int().positive('Coffee is required'),
  methodId: z.coerce.number({ message: 'Brew method is required' }).int().positive('Brew method is required'),
  name: z.string().min(1, 'Name is required').max(120, 'Name must not exceed 120 characters'),
  coffeeGrams: z.coerce.number().positive('Coffee grams must be greater than zero').optional(),
  waterGrams: z.coerce.number().positive('Water grams must be greater than zero').optional(),
  ratio: z.string().max(20, 'Ratio must not exceed 20 characters').optional(),
  grindSetting: z.string().max(120, 'Grind setting must not exceed 120 characters').optional(),
  waterTemp: z.coerce
    .number()
    .min(70, 'Water temperature must be at least 70 degrees Celsius')
    .max(100, 'Water temperature must not exceed 100 degrees Celsius')
    .optional(),
  brewTime: z.string().max(20, 'Brew time must not exceed 20 characters').optional(),
  steps: z.string().max(1000, 'Steps must not exceed 1000 characters').optional(),
  expectedTaste: z.string().max(500, 'Expected taste must not exceed 500 characters').optional(),
  favorite: z.boolean().optional(),
});

export type RecipeFormValues = z.infer<typeof recipeSchema>;
```

The plan pins the exact handling so blank optional numeric inputs become
`undefined` rather than `NaN` (e.g. a preprocess that maps `''` → `undefined`).

## 6. Recipe write API (extend `src/lib/api/recipes.ts`)

```ts
createRecipe(body: RecipeFormValues): Promise<Recipe>   // POST /api/recipes
updateRecipe(id: number, body: RecipeFormValues): Promise<Recipe> // PUT /api/recipes/{id}
deleteRecipe(id: number): Promise<void>                 // DELETE /api/recipes/{id}
```

## 7. Mutation hooks (`src/hooks/useRecipeMutations.ts`)

`useCreateRecipe`/`useUpdateRecipe`/`useDeleteRecipe` — `useMutation` with the
matching API; `onSuccess` → `invalidateQueries({ queryKey: ['recipes'] })`.
`useUpdateRecipe.mutate` takes `{ id, body }`; create takes `RecipeFormValues`;
delete takes `number`.

## 8. Components (`src/components/recipes/`)

- **`RecipeFormDialog.tsx`** — MUI `Dialog` + RHF form. Fields: **Coffee**
  (`Select` from `useCoffeeOptions`), **Brew Method** (`Select` from
  `useMethodOptions`), Name, Coffee Grams (number), Water Grams (number), Ratio,
  Grind Setting, Water Temp (number), Brew Time, Steps (multiline), Expected
  Taste (multiline), Favorite (checkbox). Selects show a disabled/loading state
  until options load. Props: `open`, `recipe?: Recipe`, `onClose`. `recipe`
  present → edit (prefilled, including `coffeeId`/`methodId`); absent → create.
  `zodResolver(recipeSchema)`. Submit runs `useCreateRecipe`/`useUpdateRecipe`;
  `isPending` disables submit; server `ApiError.validationErrors` map onto fields;
  other errors → `Alert`; success → `onClose()`.
- **`DeleteRecipeDialog.tsx`** — confirm ("Delete recipe ‘{name}’? …"); runs
  `useDeleteRecipe`; `isPending` disables; success → `onClose()`.

## 9. Wiring the list

- **`RecipesView`**: an "Add Recipe" button beside the "Recipes" heading; dialog
  state (`createOpen`, `editing: Recipe | null`, `deleting: Recipe | null`); pass
  `onEdit`/`onDelete` to `RecipesTable`; render the three dialogs only when open.
- **`RecipesTable`**: add optional `onEdit?`/`onDelete?` props and an **Actions**
  column with Edit/Delete icon buttons; existing columns unchanged.

## 10. States and errors

- Options loading: the coffee/method Selects are disabled with a loading hint
  until their options arrive; the rest of the form is usable.
- Field validation: Zod errors inline; submit blocked until valid.
- Server errors: 400 `validationErrors` mapped onto matching fields; other errors
  in a dialog-level `Alert`.
- Success: dialog closes; the recipes list refetches via invalidation.

## 11. Testing

- `recipeSchema`: rejects missing `coffeeId`/`methodId`/`name`; accepts a valid
  minimal object; rejects `waterTemp` below 70 / above 100 with the right message;
  coerces numeric string inputs; blank optional numbers become undefined (no NaN).
- `brewMethods` API: `listBrewMethods` calls `apiFetch` with the right URL/params.
- Options hooks: map `content` to `{ id, name }` (mock the list API).
- Recipe write API: `createRecipe`/`updateRecipe`/`deleteRecipe` call `apiFetch`
  with correct method/URL/body.
- Mutation hooks: `onSuccess` invalidates `['recipes']` (create + delete at least).
- `RecipeFormDialog`: renders the Coffee/Method Selects with mocked options;
  submitting without a coffee/method/name shows the required errors and does not
  call the mutation; a valid submit calls create with the values (coerced
  numbers); a server 400 maps a field error.
- `DeleteRecipeDialog`: clicking Delete calls the delete mutation with the id.
- `RecipesView`: Add opens the create dialog; a row Edit opens the edit dialog
  prefilled; a row Delete opens the confirm dialog.

Mock the API client / hooks — no real network in tests.

## 12. Out of scope (future specs)

Brew-session CRUD (next slice), optimistic updates, typeahead/autocomplete
dropdowns, a success toast, recipe detail page, and dedicated create/edit routes.

## 13. Acceptance criteria

- `npm run build` clean; `npm test` passes (new tests included).
- With the backend running (and at least one coffee + one brew method present):
  "Add Recipe" opens a dialog whose Coffee/Method dropdowns list existing
  records; creating a recipe adds it to the list; row Edit updates it; row Delete
  removes it — each via list refetch.
- Client validation blocks a missing coffee/method/name and an out-of-range water
  temperature; a backend 400 surfaces on the form.
- The Recipes list gains an "Add Recipe" button and per-row edit/delete actions.
