# Recipe PDF Export Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a client-side "Export PDF" button on the recipe detail page that downloads a branded one-page recipe card built from the already-loaded recipe data.

**Architecture:** A pure `buildRecipePdf(recipe)` returns a `jsPDF` document; a thin `downloadRecipePdf(recipe)` wrapper saves it; `RecipeDetailView` gets a button that calls the wrapper inside a try/catch and surfaces failures through an MUI Alert. Frontend-only — no backend, no new endpoint, no migration.

**Tech Stack:** Next.js 15 / React 19 / TypeScript, MUI, `jspdf`, Vitest + React Testing Library (jsdom).

## Global Constraints

- Frontend-only slice; no backend, no new endpoint, no migration.
- All commands run from `brewdeck-web/`.
- Strict TypeScript — no `any`; import `Recipe` from `@/lib/api/types`.
- Named exports only for the pdf module and its functions (no default export; `export default` is reserved for Next.js `page.tsx`/`layout.tsx`).
- Absolute `@/` imports, grouped React → third-party → internal.
- Reuse one `orDash` implementation — do not duplicate the null-handling logic.
- Handle the error state visibly (MUI Alert), not via `console.error`.
- jspdf's default Helvetica uses WinAnsi encoding: the favorite badge text is plain `Favorite` (the `★` glyph U+2605 is not encodable); the em dash `—` (U+2014) IS WinAnsi-safe and is kept for empty values.
- Values render as-is (no unit suffixes), matching the detail page's `StatCard`.
- After changing the shared `RecipeDetailView`, run the FULL `vitest run` (sibling tests mount it).
- Conventional Commits; scope `web` (and `docs` for the roadmap/state update).

---

### Task 1: Pure PDF builder module

Adds the `jspdf` dependency and a pure, UI-free module that turns a `Recipe` into a `jsPDF` document, plus a filename helper, a shared `orDash`, and a download wrapper. Deliverable: `src/lib/pdf/recipePdf.ts` fully unit-tested; `npm run test` + `npm run type-check` green. No UI touched.

**Files:**
- Modify: `brewdeck-web/package.json` (add `jspdf` dependency)
- Create: `brewdeck-web/src/lib/pdf/recipePdf.ts`
- Test: `brewdeck-web/src/lib/pdf/recipePdf.test.ts`

**Interfaces:**
- Consumes: `Recipe` from `@/lib/api/types` — `{ id: number; coffeeId: number; coffeeName: string; methodId: number; methodName: string; name: string; coffeeGrams: number | null; waterGrams: number | null; ratio: string | null; grindSetting: string | null; waterTemp: number | null; brewTime: string | null; steps: string | null; expectedTaste: string | null; favorite: boolean; createdAt: string; updatedAt: string | null }`.
- Produces (Task 2 relies on):
  - `buildRecipePdf(recipe: Recipe): jsPDF`
  - `downloadRecipePdf(recipe: Recipe): void`
  - `recipePdfFilename(recipe: Recipe): string`
  - `orDash(value: string | number | null): string`

- [ ] **Step 1: Add the jspdf dependency**

Run (from `brewdeck-web/`):

```bash
npm install jspdf
```

Expected: `jspdf` appears under `dependencies` in `package.json`; `npm install` exits 0.

- [ ] **Step 2: Write the failing test**

Create `brewdeck-web/src/lib/pdf/recipePdf.test.ts`:

```ts
import { beforeEach, describe, expect, it, vi } from 'vitest';
import type { Recipe } from '@/lib/api/types';

const textCalls: unknown[][] = [];
const saveMock = vi.fn();

vi.mock('jspdf', () => ({
  jsPDF: class {
    internal = { pageSize: { getWidth: () => 210 } };
    setFontSize = vi.fn();
    splitTextToSize = (text: string) => String(text).split('\n');
    save = saveMock;
    text = (...args: unknown[]) => {
      textCalls.push(args);
    };
  },
}));

import { buildRecipePdf, downloadRecipePdf, orDash, recipePdfFilename } from './recipePdf';

function emittedText(): string[] {
  return textCalls
    .map((args) => args[0])
    .flatMap((value) => (Array.isArray(value) ? value : [value]))
    .map((value) => String(value));
}

const fullRecipe: Recipe = {
  id: 1,
  coffeeId: 1,
  coffeeName: 'Mezcla Veracruz',
  methodId: 1,
  methodName: 'AeroPress',
  name: 'Mezcla AeroPress',
  coffeeGrams: 15,
  waterGrams: 230,
  ratio: '1:15',
  grindSetting: 'Timemore S3 - 5.5',
  waterTemp: 90,
  brewTime: '2:30',
  steps: 'Bloom 30s, stir gently, press slowly.',
  expectedTaste: 'Clean, aromatic, balanced.',
  favorite: true,
  createdAt: '2026-01-01T00:00:00',
  updatedAt: null,
};

const emptyRecipe: Recipe = {
  ...fullRecipe,
  name: 'Bare Recipe',
  coffeeGrams: null,
  waterGrams: null,
  ratio: null,
  grindSetting: null,
  waterTemp: null,
  brewTime: null,
  steps: null,
  expectedTaste: null,
  favorite: false,
};

beforeEach(() => {
  textCalls.length = 0;
});

describe('orDash', () => {
  it('returns an em dash for null and blank, and the value otherwise', () => {
    expect(orDash(null)).toBe('—');
    expect(orDash('   ')).toBe('—');
    expect(orDash(90)).toBe('90');
    expect(orDash('1:15')).toBe('1:15');
  });
});

describe('recipePdfFilename', () => {
  it('slugifies the recipe name and appends .pdf', () => {
    expect(recipePdfFilename(fullRecipe)).toBe('mezcla-aeropress.pdf');
  });

  it('falls back to recipe.pdf when the name has no alphanumerics', () => {
    expect(recipePdfFilename({ ...fullRecipe, name: '!!!' })).toBe('recipe.pdf');
  });
});

describe('buildRecipePdf', () => {
  it('emits the header, title, coffee, method, and every param', () => {
    buildRecipePdf(fullRecipe);
    const text = emittedText();

    expect(text).toContain('BrewDeck');
    expect(text).toContain('Mezcla AeroPress');
    expect(text).toContain('Coffee: Mezcla Veracruz');
    expect(text).toContain('Method: AeroPress');
    expect(text).toContain('Coffee (g): 15');
    expect(text).toContain('Water (g): 230');
    expect(text).toContain('Ratio: 1:15');
    expect(text).toContain('Grind: Timemore S3 - 5.5');
    expect(text).toContain('Water Temp: 90');
    expect(text).toContain('Brew Time: 2:30');
  });

  it('renders an em dash for null params', () => {
    buildRecipePdf(emptyRecipe);
    const text = emittedText();

    expect(text).toContain('Coffee (g): —');
    expect(text).toContain('Ratio: —');
    expect(text).toContain('Water Temp: —');
  });

  it('shows the Favorite badge only when the recipe is a favorite', () => {
    buildRecipePdf(fullRecipe);
    expect(emittedText()).toContain('Favorite');

    textCalls.length = 0;
    buildRecipePdf({ ...fullRecipe, favorite: false });
    expect(emittedText()).not.toContain('Favorite');
  });

  it('includes the Steps and Expected taste sections when present', () => {
    buildRecipePdf(fullRecipe);
    const text = emittedText();

    expect(text).toContain('Steps');
    expect(text).toContain('Bloom 30s, stir gently, press slowly.');
    expect(text).toContain('Expected taste');
    expect(text).toContain('Clean, aromatic, balanced.');
  });

  it('omits the Steps and Expected taste sections when those fields are null', () => {
    buildRecipePdf(emptyRecipe);
    const text = emittedText();

    expect(text).not.toContain('Steps');
    expect(text).not.toContain('Expected taste');
  });

  it('returns a jsPDF document', () => {
    const doc = buildRecipePdf(fullRecipe);
    expect(typeof doc.save).toBe('function');
  });
});

describe('downloadRecipePdf', () => {
  it('saves the document under the slugified filename', () => {
    downloadRecipePdf(fullRecipe);
    expect(saveMock).toHaveBeenCalledWith('mezcla-aeropress.pdf');
  });
});
```

- [ ] **Step 3: Run the test to verify it fails**

Run: `npm run test -- src/lib/pdf/recipePdf.test.ts`
Expected: FAIL — cannot resolve `./recipePdf` (module does not exist yet).

- [ ] **Step 4: Write the module**

Create `brewdeck-web/src/lib/pdf/recipePdf.ts`:

```ts
import { jsPDF } from 'jspdf';
import type { Recipe } from '@/lib/api/types';

export function orDash(value: string | number | null): string {
  if (value === null) {
    return '—';
  }
  const text = String(value);
  return text.trim() !== '' ? text : '—';
}

export function recipePdfFilename(recipe: Recipe): string {
  const slug = recipe.name
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/^-+|-+$/g, '');
  return `${slug || 'recipe'}.pdf`;
}

export function buildRecipePdf(recipe: Recipe): jsPDF {
  const doc = new jsPDF();
  const marginX = 14;
  const maxWidth = doc.internal.pageSize.getWidth() - marginX * 2;
  let y = 20;

  doc.setFontSize(20);
  doc.text('BrewDeck', marginX, y);
  doc.setFontSize(11);
  doc.text('Recipe', marginX, y + 6);
  y += 18;

  doc.setFontSize(16);
  doc.text(recipe.name, marginX, y);
  y += 8;
  if (recipe.favorite) {
    doc.setFontSize(11);
    doc.text('Favorite', marginX, y);
    y += 8;
  }
  y += 6;

  const details: Array<[string, string]> = [
    ['Coffee', recipe.coffeeName],
    ['Method', recipe.methodName],
    ['Coffee (g)', orDash(recipe.coffeeGrams)],
    ['Water (g)', orDash(recipe.waterGrams)],
    ['Ratio', orDash(recipe.ratio)],
    ['Grind', orDash(recipe.grindSetting)],
    ['Water Temp', orDash(recipe.waterTemp)],
    ['Brew Time', orDash(recipe.brewTime)],
  ];
  doc.setFontSize(11);
  for (const [label, value] of details) {
    doc.text(`${label}: ${value}`, marginX, y);
    y += 7;
  }

  if (recipe.steps && recipe.steps.trim() !== '') {
    y += 6;
    doc.setFontSize(13);
    doc.text('Steps', marginX, y);
    y += 7;
    doc.setFontSize(11);
    const lines = doc.splitTextToSize(recipe.steps, maxWidth);
    doc.text(lines, marginX, y);
    y += lines.length * 6;
  }

  if (recipe.expectedTaste && recipe.expectedTaste.trim() !== '') {
    y += 6;
    doc.setFontSize(13);
    doc.text('Expected taste', marginX, y);
    y += 7;
    doc.setFontSize(11);
    const lines = doc.splitTextToSize(recipe.expectedTaste, maxWidth);
    doc.text(lines, marginX, y);
    y += lines.length * 6;
  }

  doc.setFontSize(9);
  doc.text(`Generated ${new Date().toLocaleDateString()}`, marginX, 285);

  return doc;
}

export function downloadRecipePdf(recipe: Recipe): void {
  buildRecipePdf(recipe).save(recipePdfFilename(recipe));
}
```

- [ ] **Step 5: Run the test to verify it passes**

Run: `npm run test -- src/lib/pdf/recipePdf.test.ts`
Expected: PASS — all cases green.

- [ ] **Step 6: Type-check and lint the changed files**

Run: `npm run type-check`
Expected: no errors.
Run: `npm run lint:fix -- src/lib/pdf/recipePdf.ts src/lib/pdf/recipePdf.test.ts`
Expected: no remaining lint errors.

- [ ] **Step 7: Commit**

```bash
git add package.json package-lock.json src/lib/pdf/recipePdf.ts src/lib/pdf/recipePdf.test.ts
git commit -m "feat(web): add recipe PDF builder module"
```

---

### Task 2: Wire the Export PDF button into the recipe detail page

Adds an "Export PDF" button beside "Improve with AI" that calls `downloadRecipePdf(recipe)` inside a try/catch and surfaces failures through an MUI Alert. Deliverable: button visible + wired, its tests plus the full existing suite green.

**Files:**
- Modify: `brewdeck-web/src/components/recipes/RecipeDetailView.tsx`
- Test: `brewdeck-web/src/components/recipes/RecipeDetailView.test.tsx`

**Interfaces:**
- Consumes (from Task 1): `downloadRecipePdf(recipe: Recipe): void` from `@/lib/pdf/recipePdf`.
- Produces: an "Export PDF" button in `RecipeDetailView`.

- [ ] **Step 1: Write the failing test**

In `brewdeck-web/src/components/recipes/RecipeDetailView.test.tsx`, add a mock for the pdf module alongside the existing `vi.mock` block (place this near the other mocks at the top of the file, after the existing `vi.hoisted`/`vi.mock` lines):

```ts
const { downloadRecipePdfMock } = vi.hoisted(() => ({ downloadRecipePdfMock: vi.fn() }));

vi.mock('@/lib/pdf/recipePdf', () => ({
  downloadRecipePdf: downloadRecipePdfMock,
}));
```

In the existing `beforeEach` (which already calls `improveMutate.mockReset()`), add:

```ts
  downloadRecipePdfMock.mockReset();
```

Then add these cases inside the `describe('RecipeDetailView', ...)` block:

```ts
  it('renders the Export PDF button when the recipe is loaded', () => {
    mockRecipe({ isLoading: false, isError: false, data: recipe });
    mockStats({ isLoading: false, isError: false, data: undefined });
    mockHistory({ isLoading: false, isError: false, data: sessionsPage([]) });

    renderWithTheme(<RecipeDetailView recipeId={1} />);

    expect(screen.getByRole('button', { name: /export pdf/i })).toBeInTheDocument();
  });

  it('downloads the PDF for the current recipe on click', () => {
    mockRecipe({ isLoading: false, isError: false, data: recipe });
    mockStats({ isLoading: false, isError: false, data: undefined });
    mockHistory({ isLoading: false, isError: false, data: sessionsPage([]) });

    renderWithTheme(<RecipeDetailView recipeId={1} />);
    fireEvent.click(screen.getByRole('button', { name: /export pdf/i }));

    expect(downloadRecipePdfMock).toHaveBeenCalledWith(recipe);
  });

  it('shows an error alert when PDF generation throws', () => {
    downloadRecipePdfMock.mockImplementation(() => {
      throw new Error('boom');
    });
    mockRecipe({ isLoading: false, isError: false, data: recipe });
    mockStats({ isLoading: false, isError: false, data: undefined });
    mockHistory({ isLoading: false, isError: false, data: sessionsPage([]) });

    renderWithTheme(<RecipeDetailView recipeId={1} />);
    fireEvent.click(screen.getByRole('button', { name: /export pdf/i }));

    expect(screen.getByText(/could not generate the pdf/i)).toBeInTheDocument();
  });
```

Note: `fireEvent`, `screen`, `mockRecipe`, `mockStats`, `mockHistory`, `sessionsPage`, and `recipe` already exist in this test file.

- [ ] **Step 2: Run the test to verify it fails**

Run: `npm run test -- src/components/recipes/RecipeDetailView.test.tsx`
Expected: FAIL — no button with name `/export pdf/i` (button not added yet).

- [ ] **Step 3: Add the import**

In `brewdeck-web/src/components/recipes/RecipeDetailView.tsx`, add the import next to the existing `RecipeFormDialog` import (line ~26):

```tsx
import { downloadRecipePdf } from '@/lib/pdf/recipePdf';
```

- [ ] **Step 4: Add the error state and handler**

In `RecipeDetailView`, after the existing `const [improveError, setImproveError] = useState<string | null>(null);` line, add:

```tsx
  const [pdfError, setPdfError] = useState<string | null>(null);
```

After the existing `onImprove` handler (just before `const details: Array<...>`), add:

```tsx
  const onExport = () => {
    setPdfError(null);
    try {
      downloadRecipePdf(recipe);
    } catch {
      setPdfError('Could not generate the PDF.');
    }
  };
```

- [ ] **Step 5: Add the button and its Alert**

In the existing action-button `Box` (the one wrapping the "Improve with AI" Tooltip/Button), add the Export button as a sibling after the Tooltip's closing tag, still inside the same `Box`:

```tsx
        <Button variant="outlined" size="small" onClick={onExport}>
          Export PDF
        </Button>
```

Immediately after the existing `improveError` Alert block, add:

```tsx
      {pdfError ? (
        <Alert severity="error" sx={{ mb: 2 }}>
          {pdfError}
        </Alert>
      ) : null}
```

(`Alert`, `Button`, `Box`, and `useState` are already imported in this file.)

- [ ] **Step 6: Run the focused test to verify it passes**

Run: `npm run test -- src/components/recipes/RecipeDetailView.test.tsx`
Expected: PASS — all three new cases plus the existing ones green.

- [ ] **Step 7: Run the FULL suite (shared component changed)**

Run: `npm run test`
Expected: all test files pass (the button was added to the shared `RecipeDetailView`; sibling tests that mount it must stay green).

- [ ] **Step 8: Type-check, lint, build**

Run: `npm run type-check`
Expected: no errors.
Run: `npm run lint:fix -- src/components/recipes/RecipeDetailView.tsx src/components/recipes/RecipeDetailView.test.tsx`
Expected: no remaining lint errors.
Run: `npm run build`
Expected: production build succeeds (also type-checks).

- [ ] **Step 9: Commit**

```bash
git add src/components/recipes/RecipeDetailView.tsx src/components/recipes/RecipeDetailView.test.tsx
git commit -m "feat(web): add Export PDF button on recipe detail"
```

---

### Task 3: Update roadmap and project-state docs

Records the shipped slice. Docs-only; no code touched.

**Files:**
- Modify: `.claude/roadmap.md`
- Modify: `.claude/project-state.md`

- [ ] **Step 1: Mark the roadmap item Done**

In `.claude/roadmap.md`, under `## Phase 5 — Product Improvements`, replace the line:

```
- Export recipes to PDF — Not Started
```

with:

```
- Export recipes to PDF — Done: client-side "Export PDF" on recipe detail (jspdf recipe card)
```

- [ ] **Step 2: Update project-state**

In `.claude/project-state.md`, update the `## Current Phase` paragraph by appending this sentence before the final "Next:" sentence:

```
Recipe PDF export shipped (frontend-only): a client-side "Export PDF" button on the recipe detail page downloads a branded one-page recipe card built with jspdf from the loaded recipe.
```

And add this entry at the top of the `## Recently Worked On` list:

```
- Recipe PDF export (frontend-only) — client-side "Export PDF" button on the recipe detail page; pure buildRecipePdf(recipe) → jsPDF recipe card (name, favorite, coffee, method, params, steps, expected taste), downloadRecipePdf saves a slugified filename; failures surface as an MUI Alert
```

Update the `## Immediate Next Steps` list, replacing the "export recipes to PDF" mention so only the remaining work stays:

```
1. Remaining Phase 5 feature: public share links.
2. Review JaCoCo and SonarCloud.
```

- [ ] **Step 3: Commit**

```bash
git add .claude/roadmap.md .claude/project-state.md
git commit -m "docs: record recipe PDF export slice"
```

---

## Notes for the executor

- All commands run from `brewdeck-web/`.
- There is no backend change in this slice — do not run the Maven build.
- The `jspdf` mock in `recipePdf.test.ts` is deliberately minimal: it captures `text()` calls and stubs `splitTextToSize`/`save`. Do not import the real jspdf in tests — the assertions are on what text the builder emits, not on rendered bytes.
