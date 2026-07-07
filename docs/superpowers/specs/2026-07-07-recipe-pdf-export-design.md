# Recipe PDF Export — Design Spec

**Date:** 2026-07-07
**Status:** Approved (design)
**Slice:** Phase 5 product improvement. Frontend-only. Follows the AI improve-from-history slice (PR #60 merged).

## Goal

Let a user export a single recipe to a PDF from the recipe detail page. A button
generates a branded, one-page "recipe card" from the already-loaded recipe data
and triggers a browser download. Fully client-side — no backend, no new endpoint.

## Non-Goals

- No backend PDF endpoint, no `GET /api/recipes/{id}/pdf` (rejected in
  brainstorming — client-side keeps the slice small and leaves the backend and
  CI quality gates untouched).
- No brew stats, no brew-session history in the PDF (rejected — recipe card
  only; avoids coupling export to extra async sources).
- No bulk / multi-recipe export, no email, no server-side storage of the PDF.
- No changes to the recipe data model or any existing endpoint.

## Decisions (from brainstorming)

1. **Generation:** client-side, using `jspdf`.
2. **Content:** recipe card only — name, favorite flag, coffee, method, the eight
   brew params, steps, expected taste. Single data source: the loaded `Recipe`.
3. **Layout:** branded card — BrewDeck header, title + favorite badge, a details
   section, conditional Steps and Expected-taste sections, a generated-date
   footer.

## Architecture

Three units with clear boundaries:

- **`src/lib/pdf/recipePdf.ts`** (new)
  - `buildRecipePdf(recipe: Recipe): jsPDF` — **pure**: consumes a `Recipe`,
    returns a populated `jsPDF` document. No DOM access, no download, no other
    side effects. This is the testable core.
  - `downloadRecipePdf(recipe: Recipe): void` — thin wrapper:
    `buildRecipePdf(recipe).save(recipePdfFilename(recipe))`.
  - `recipePdfFilename(recipe: Recipe): string` — slugifies `recipe.name`
    (lowercase, non-alphanumeric runs → `-`, trimmed), falls back to `recipe`
    when the slug is empty, and appends `.pdf`.
  - `orDash(value: string | number | null): string` — the null/blank → `—`
    helper, moved here so the PDF module and (optionally) the detail view share
    one implementation instead of duplicating it.
- **`src/components/recipes/RecipeDetailView.tsx`** (modified)
  - An "Export PDF" outlined button next to the existing "Improve with AI"
    button. `onClick` calls `downloadRecipePdf(recipe)` inside a `try/catch`; on
    failure it sets a local error string surfaced through an MUI `Alert`
    (mirrors the existing `improveError` Alert pattern).

New dependency: `jspdf` (added to `brewdeck-web/package.json`). Frontend-only.

### Data flow

```
Recipe detail page (recipe already loaded)
  └─ "Export PDF" button
       └─ downloadRecipePdf(recipe)
            └─ buildRecipePdf(recipe) → jsPDF
                 └─ doc.save("<slug>.pdf") → browser download
       └─ on throw: setPdfError(...) → error Alert
```

## PDF layout (branded card)

Single page, single data source (the loaded `recipe`; no async at export time):

- **Header band:** "BrewDeck" wordmark, "Recipe" subtitle.
- **Title:** `recipe.name`. If `recipe.favorite`, render a "Favorite" badge (plain text — jspdf's default Helvetica is WinAnsi and cannot encode the `★` glyph).
- **Details section** — labeled lines, in this order:
  Coffee (`coffeeName`), Method (`methodName`), Coffee (g) (`coffeeGrams`),
  Water (g) (`waterGrams`), Ratio (`ratio`), Grind (`grindSetting`),
  Water Temp (`waterTemp`), Brew Time (`brewTime`). Null/blank values render as
  `—` via `orDash`. Values render as-is (no unit suffixes), matching the detail
  page's `StatCard` display.
- **Steps section** — rendered only when `recipe.steps` is non-null/non-blank;
  wrapped to the page width via `doc.splitTextToSize`.
- **Expected taste section** — rendered only when `recipe.expectedTaste` is
  non-null/non-blank.
- **Footer:** "Generated <local date>" (`new Date().toLocaleDateString()`).

## Error handling

Building a PDF is synchronous CPU work over already-loaded data; the only
realistic failure is an unexpected `jspdf` error. The button's `onClick` wraps
`downloadRecipePdf` in `try/catch`; on a thrown error it sets a local error
message ("Could not generate the PDF.") shown in an MUI `Alert`, and clears any
prior message on the next click.

The button renders only after the recipe has loaded — `RecipeDetailView` returns
early (spinner / error state) when `recipeQuery.data` is absent — so no
disabled or loading state is required on the button.

## Testing

Frontend only. Vitest + React Testing Library.

- **`src/lib/pdf/recipePdf.test.ts`** (new):
  - Mock `jspdf`. `buildRecipePdf` emits the recipe name, coffee, method, and
    each of the eight params; null fields render `—`; the favorite badge appears
    only when `favorite` is true; the Steps and Expected-taste sections are
    omitted when those fields are null/blank and included when present. Assert
    `buildRecipePdf` returns the doc instance.
  - `recipePdfFilename` slugifies the name, appends `.pdf`, and falls back to
    `recipe.pdf` for an empty/symbol-only name.
  - `downloadRecipePdf` calls `doc.save` once with the slugified filename.
- **`src/components/recipes/RecipeDetailView.test.tsx`** (extend):
  - The "Export PDF" button renders when the recipe is loaded.
  - Clicking it calls `downloadRecipePdf` with the recipe (mock the
    `@/lib/pdf/recipePdf` module).
  - On a thrown error the error Alert appears.
- Run the **full** `vitest run` — the button is added to the shared
  `RecipeDetailView`, so sibling tests that mount it must be checked (see the
  sibling-test regression noted in project memory).

## Global constraints

- Frontend-only slice; no backend, no new endpoint, no migration.
- Strict TypeScript — no `any`; import `Recipe` from `@/lib/api/types`.
- Named exports for the pdf module and its functions (no default export;
  `export default` is reserved for Next.js `page.tsx`/`layout.tsx`).
- Absolute `@/` imports, grouped React → third-party → internal.
- Reuse `orDash` from one place; do not duplicate the null-handling logic.
- Handle the error state visibly (Alert), not via `console.error`.
- Scope `lint:fix` to changed files only.
- Conventional Commits; scope `web` (and `docs` for the roadmap/state update).
