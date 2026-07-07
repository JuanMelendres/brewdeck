# Coffee Tasting-Notes Radar Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Show a coffee's flavor profile as a radar chart on the coffee detail page, driven by numeric 1–5 tasting scores that replace the old free-text tasting fields.

**Architecture:** Backend replaces four free-text tasting columns (`acidity`, `body`, `sweetness`, `bitterness`) with four nullable integer score columns (1–5), validated in the DTO. Frontend replaces the four tasting text inputs with MUI sliders, adds a recharts `RadarChart` component, and renders it on the coffee detail page. Scores ride the existing coffee endpoints — no new API.

**Tech Stack:** Java 21, Spring Boot 3, Flyway, JPA/Lombok, JUnit 5 + MockMvc + Testcontainers. Next.js 15 / React 19 / TypeScript, MUI, react-hook-form + zod, recharts, Vitest + React Testing Library.

## Global Constraints

- Tasting score scale is integer **1–5 inclusive**; columns/fields are **nullable** (a coffee may be un-scored).
- Bean Validation messages contain **no special symbols** (responses are sanitized). Plain ASCII only.
- Collection GETs return `PageResponse`; GET-by-id returns the DTO directly. (No API shape change here — scores ride existing coffee endpoints.)
- Never leak JPA entities from controllers — map through `CoffeeResponse`.
- Frontend: no `any`; named exports (except Next.js `page.tsx`/`layout.tsx`); import domain types from `src/lib/api`; server state via TanStack Query.
- Backend verify: `./mvnw spotless:apply` then `./mvnw clean verify`.
- Frontend verify (in `brewdeck-web/`): `npm run test`, `npm run type-check`, `npm run lint:fix -- <changed files>`, `npm run build`.
- Radar renders only when **all four** scores are present; any null → empty state ("Add tasting scores to see the flavor profile").
- `CoffeeFilter` / `CoffeeSpecification` reference only name/origin/roastLevel/process — **do not touch them**; they have no tasting fields.

---

### Task 1: Backend — scores replace text tasting fields (migration, entity, DTO, service, tests)

Java records force the entity, both DTOs, the service mapper, and every test that constructs `CoffeeRequest` / reads `CoffeeResponse` to change together or the module won't compile. This is one cohesive task ending in a green `./mvnw clean verify`.

**Files:**
- Create: `brewdeck-api/src/main/resources/db/migration/V3__coffee_tasting_scores.sql`
- Modify: `brewdeck-api/src/main/java/com/brewdeck/brewdeck_api/coffee/Coffee.java` (fields at lines 39–42)
- Modify: `brewdeck-api/src/main/java/com/brewdeck/brewdeck_api/coffee/CoffeeRequest.java`
- Modify: `brewdeck-api/src/main/java/com/brewdeck/brewdeck_api/coffee/CoffeeResponse.java`
- Modify: `brewdeck-api/src/main/java/com/brewdeck/brewdeck_api/coffee/CoffeeService.java` (`applyRequest`, lines ~112–128)
- Test: `brewdeck-api/src/test/java/com/brewdeck/brewdeck_api/coffee/CoffeeControllerTest.java`
- Test: `brewdeck-api/src/test/java/com/brewdeck/brewdeck_api/coffee/CoffeeServiceTest.java`
- Test: `brewdeck-api/src/test/java/com/brewdeck/brewdeck_api/coffee/CoffeeRepositoryTest.java`

**Interfaces:**
- Consumes: nothing new.
- Produces:
  - `CoffeeRequest` record fields (in order): `name, brand, origin, region, farm, producer, variety, process, roastLevel, notesPrimary, notesSecondary, acidityScore, bodyScore, sweetnessScore, bitternessScore, description` — the four score fields are `Integer`.
  - `CoffeeResponse` record fields: same order, score fields `Integer` (`acidityScore, bodyScore, sweetnessScore, bitternessScore`), plus trailing `description, createdAt, updatedAt`.
  - `Coffee` entity: `Integer acidityScore, bodyScore, sweetnessScore, bitternessScore` with `@Column(name = "acidity_score")` etc.

- [ ] **Step 1: Write the Flyway migration**

Create `brewdeck-api/src/main/resources/db/migration/V3__coffee_tasting_scores.sql`:

```sql
ALTER TABLE coffees DROP COLUMN acidity;
ALTER TABLE coffees DROP COLUMN body;
ALTER TABLE coffees DROP COLUMN sweetness;
ALTER TABLE coffees DROP COLUMN bitterness;

ALTER TABLE coffees ADD COLUMN acidity_score SMALLINT;
ALTER TABLE coffees ADD COLUMN body_score SMALLINT;
ALTER TABLE coffees ADD COLUMN sweetness_score SMALLINT;
ALTER TABLE coffees ADD COLUMN bitterness_score SMALLINT;
```

- [ ] **Step 2: Update the `Coffee` entity**

In `Coffee.java`, replace lines 39–42:

```java
  private String acidity;
  private String body;
  private String sweetness;
  private String bitterness;
```

with:

```java
  @Column(name = "acidity_score")
  private Integer acidityScore;

  @Column(name = "body_score")
  private Integer bodyScore;

  @Column(name = "sweetness_score")
  private Integer sweetnessScore;

  @Column(name = "bitterness_score")
  private Integer bitternessScore;
```

- [ ] **Step 3: Update `CoffeeRequest`**

In `CoffeeRequest.java`, replace the four `@Size ... String acidity/body/sweetness/bitterness` fields with:

```java
    @Min(value = 1, message = "Acidity score must be at least 1")
        @Max(value = 5, message = "Acidity score must not exceed 5")
        Integer acidityScore,
    @Min(value = 1, message = "Body score must be at least 1")
        @Max(value = 5, message = "Body score must not exceed 5")
        Integer bodyScore,
    @Min(value = 1, message = "Sweetness score must be at least 1")
        @Max(value = 5, message = "Sweetness score must not exceed 5")
        Integer sweetnessScore,
    @Min(value = 1, message = "Bitterness score must be at least 1")
        @Max(value = 5, message = "Bitterness score must not exceed 5")
        Integer bitternessScore,
```

Update imports: remove nothing (keep `@Size`, still used elsewhere); add:

```java
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
```

- [ ] **Step 4: Update `CoffeeResponse`**

In `CoffeeResponse.java`, replace the four `String acidity/body/sweetness/bitterness` record components with:

```java
    Integer acidityScore,
    Integer bodyScore,
    Integer sweetnessScore,
    Integer bitternessScore,
```

and in `fromEntity`, replace the four `coffee.getAcidity()` ... lines with:

```java
        coffee.getAcidityScore(),
        coffee.getBodyScore(),
        coffee.getSweetnessScore(),
        coffee.getBitternessScore(),
```

- [ ] **Step 5: Update `CoffeeService.applyRequest`**

In `CoffeeService.java`, replace the four `coffee.setAcidity(request.acidity()); ...` lines with:

```java
    coffee.setAcidityScore(request.acidityScore());
    coffee.setBodyScore(request.bodyScore());
    coffee.setSweetnessScore(request.sweetnessScore());
    coffee.setBitternessScore(request.bitternessScore());
```

- [ ] **Step 6: Update `CoffeeServiceTest`**

Every `new CoffeeRequest(...)` in this file passes four tasting args after `notesSecondary`. Replace those four string literals (e.g. `"Media", "Medio", "Media", "Baja"`) with four integers, e.g. `3, 3, 4, 2`.

In the builder that mirrors the request (around lines 213–216), replace:

```java
            .acidity(request.acidity())
            .body(request.body())
            .sweetness(request.sweetness())
            .bitterness(request.bitterness())
```

with:

```java
            .acidityScore(request.acidityScore())
            .bodyScore(request.bodyScore())
            .sweetnessScore(request.sweetnessScore())
            .bitternessScore(request.bitternessScore())
```

Replace the assertions (around lines 301–304):

```java
    assertThat(result.acidityScore()).isEqualTo(3);
    assertThat(result.bodyScore()).isEqualTo(3);
    assertThat(result.sweetnessScore()).isEqualTo(4);
    assertThat(result.bitternessScore()).isEqualTo(2);
```

(Match whatever integers you used when constructing the request above.)

- [ ] **Step 7: Update `CoffeeRepositoryTest`**

Wherever it sets tasting fields on a `Coffee` (via builder `.acidity("...")` or `setAcidity("...")`), switch to `.acidityScore(3)` / `setAcidityScore(3)` etc. If it asserts on those fields, assert integers.

- [ ] **Step 8: Update `CoffeeControllerTest` — response mapping**

Around lines 273–276 and 363–366 the test builds an expected `CoffeeResponse` from request fields. Replace:

```java
            request.acidity(),
            request.body(),
            request.sweetness(),
            request.bitterness(),
```

with:

```java
            request.acidityScore(),
            request.bodyScore(),
            request.sweetnessScore(),
            request.bitternessScore(),
```

Update every `new CoffeeRequest(...)` in this file the same way as Step 6 (four integers in the tasting positions).

- [ ] **Step 9: Update `CoffeeControllerTest` — replace the `@Size` tasting cases with score range cases**

The parameterized case block (around lines 476–479) currently tests `acidity,81,...` length limits. Replace those four rows. Find the `@CsvSource` they belong to; if it drives a "too long" test, remove the four tasting rows from it (scores aren't strings). Then add a new parameterized test for score bounds:

```java
  @ParameterizedTest
  @CsvSource({
    "acidityScore,0,Acidity score must be at least 1",
    "acidityScore,6,Acidity score must not exceed 5",
    "bodyScore,0,Body score must be at least 1",
    "bodyScore,6,Body score must not exceed 5",
    "sweetnessScore,0,Sweetness score must be at least 1",
    "sweetnessScore,6,Sweetness score must not exceed 5",
    "bitternessScore,0,Bitterness score must be at least 1",
    "bitternessScore,6,Bitterness score must not exceed 5"
  })
  void create_shouldReturnValidationError_whenScoreOutOfRange(
      String field, int value, String expectedMessage) throws Exception {
    String payload =
        String.format("{\"name\":\"Valid name\",\"%s\":%d}", field, value);

    mockMvc
        .perform(
            post("/api/coffees").contentType(MediaType.APPLICATION_JSON).content(payload))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.validationErrors." + field).value(expectedMessage));
  }
```

Ensure imports for `@ParameterizedTest` / `@CsvSource` exist (they already do — the file uses them). Add a `MediaType` import if not present.

- [ ] **Step 10: Add a null-scores-are-valid controller test**

Add to `CoffeeControllerTest`:

```java
  @Test
  void create_shouldSucceed_whenScoresAreOmitted() throws Exception {
    given(coffeeService.create(any(CoffeeRequest.class)))
        .willReturn(
            new CoffeeResponse(
                1L, "No scores", null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, LocalDateTime.now(), null));

    mockMvc
        .perform(
            post("/api/coffees")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"No scores\"}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.acidityScore").doesNotExist());
  }
```

Adjust the `CoffeeResponse` constructor arg count/order to match the record (16 value fields + `createdAt` + `updatedAt`). If the file already has a helper for building a response, reuse it instead of the raw constructor.

- [ ] **Step 11: Format**

Run: `./mvnw spotless:apply`
Expected: files reformatted, no errors.

- [ ] **Step 12: Run the full backend build**

Run: `./mvnw clean verify`
Expected: BUILD SUCCESS. Flyway applies V3 in Testcontainers; all coffee tests green; the new score-range and null-score tests pass.

- [ ] **Step 13: Commit**

```bash
git add brewdeck-api/src/main/resources/db/migration/V3__coffee_tasting_scores.sql \
        brewdeck-api/src/main/java/com/brewdeck/brewdeck_api/coffee/Coffee.java \
        brewdeck-api/src/main/java/com/brewdeck/brewdeck_api/coffee/CoffeeRequest.java \
        brewdeck-api/src/main/java/com/brewdeck/brewdeck_api/coffee/CoffeeResponse.java \
        brewdeck-api/src/main/java/com/brewdeck/brewdeck_api/coffee/CoffeeService.java \
        brewdeck-api/src/test/java/com/brewdeck/brewdeck_api/coffee/
git commit -m "feat(api): replace free-text tasting fields with 1-5 scores"
```

---

### Task 2: Frontend — Coffee type, zod schema, form sliders

**Files:**
- Modify: `brewdeck-web/src/lib/api/types.ts` (`Coffee` type, lines 29–49)
- Modify: `brewdeck-web/src/lib/validation/coffeeSchema.ts`
- Modify: `brewdeck-web/src/components/coffees/CoffeeFormDialog.tsx`
- Test: `brewdeck-web/src/components/coffees/CoffeeFormDialog.test.tsx`

**Interfaces:**
- Consumes: Task 1's response shape (`acidityScore` etc. as `number | null`).
- Produces:
  - `Coffee` type gains `acidityScore, bodyScore, sweetnessScore, bitternessScore: number | null` (replacing the four `string | null` tasting fields).
  - `CoffeeFormValues` (from `coffeeSchema`) gains `acidityScore?, bodyScore?, sweetnessScore?, bitternessScore?: number` (replacing the four string tasting fields).

- [ ] **Step 1: Update the `Coffee` domain type**

In `types.ts`, inside `export type Coffee`, replace:

```ts
  acidity: string | null;
  body: string | null;
  sweetness: string | null;
  bitterness: string | null;
```

with:

```ts
  acidityScore: number | null;
  bodyScore: number | null;
  sweetnessScore: number | null;
  bitternessScore: number | null;
```

- [ ] **Step 2: Update the zod schema**

In `coffeeSchema.ts`, replace the four `acidity/body/sweetness/bitterness: z.string()...` lines with:

```ts
  acidityScore: z.coerce.number().int().min(1).max(5).optional(),
  bodyScore: z.coerce.number().int().min(1).max(5).optional(),
  sweetnessScore: z.coerce.number().int().min(1).max(5).optional(),
  bitternessScore: z.coerce.number().int().min(1).max(5).optional(),
```

- [ ] **Step 3: Write a failing form test for the sliders**

In `CoffeeFormDialog.test.tsx`, add (adapt the existing render/wrapper helper the file already uses):

```tsx
it('renders tasting score sliders', () => {
  renderDialog(); // use the file's existing render helper; open state = true, no coffee
  expect(screen.getByRole('slider', { name: /acidity/i })).toBeInTheDocument();
  expect(screen.getByRole('slider', { name: /body/i })).toBeInTheDocument();
  expect(screen.getByRole('slider', { name: /sweetness/i })).toBeInTheDocument();
  expect(screen.getByRole('slider', { name: /bitterness/i })).toBeInTheDocument();
});
```

If the file has no shared render helper, render directly:

```tsx
renderWithTheme(<CoffeeFormDialog open onClose={vi.fn()} />);
```

(`renderWithTheme` is imported from `@/test/renderWithTheme`; ensure `screen` from `@testing-library/react` is imported.)

- [ ] **Step 4: Run the test to verify it fails**

Run: `npm run test -- src/components/coffees/CoffeeFormDialog.test.tsx`
Expected: FAIL — no slider roles found (form still renders text fields).

- [ ] **Step 5: Update the form — remove tasting text fields, add sliders**

In `CoffeeFormDialog.tsx`:

Remove the four tasting entries (`acidity`, `body`, `sweetness`, `bitterness`) from the `FIELDS` array.

In `toDefaults`, replace the four `acidity: coffee?.acidity ?? ''` lines with:

```ts
    acidityScore: coffee?.acidityScore ?? 3,
    bodyScore: coffee?.bodyScore ?? 3,
    sweetnessScore: coffee?.sweetnessScore ?? 3,
    bitternessScore: coffee?.bitternessScore ?? 3,
```

Add imports:

```tsx
import Slider from '@mui/material/Slider';
import Typography from '@mui/material/Typography';
import { Controller } from 'react-hook-form';
```

Destructure `control` from `useForm`:

```tsx
  const {
    register,
    handleSubmit,
    setError,
    control,
    formState: { errors },
  } = useForm<CoffeeFormValues>({
```

Add a `SCORE_FIELDS` constant near `FIELDS`:

```tsx
const SCORE_FIELDS: Array<{ name: keyof CoffeeFormValues; label: string }> = [
  { name: 'acidityScore', label: 'Acidity' },
  { name: 'bodyScore', label: 'Body' },
  { name: 'sweetnessScore', label: 'Sweetness' },
  { name: 'bitternessScore', label: 'Bitterness' },
];
```

Inside `<Stack spacing={2}>`, after the `FIELDS.map(...)` block, add:

```tsx
            <Typography variant="subtitle2" sx={{ mt: 1 }}>
              Tasting profile (1–5)
            </Typography>
            {SCORE_FIELDS.map((field) => (
              <Controller
                key={field.name}
                name={field.name}
                control={control}
                render={({ field: { value, onChange } }) => (
                  <div>
                    <Typography variant="body2" gutterBottom id={`${field.name}-label`}>
                      {field.label}
                    </Typography>
                    <Slider
                      value={typeof value === 'number' ? value : 3}
                      onChange={(_, next) => onChange(next as number)}
                      step={1}
                      marks
                      min={1}
                      max={5}
                      valueLabelDisplay="auto"
                      aria-labelledby={`${field.name}-label`}
                      aria-label={field.label}
                    />
                  </div>
                )}
              />
            ))}
```

- [ ] **Step 6: Run the form test to verify it passes**

Run: `npm run test -- src/components/coffees/CoffeeFormDialog.test.tsx`
Expected: PASS — four sliders found by accessible name.

- [ ] **Step 7: Type-check**

Run: `npm run type-check`
Expected: no errors. (If `CoffeeDetailView.tsx` now errors on `coffee.acidity`, that is fixed in Task 3 — you may see errors there; proceed, but do not commit until Step 9 passes. If you want a clean gate now, run type-check after Task 3.)

- [ ] **Step 8: Lint the changed files**

Run: `npm run lint:fix -- src/lib/api/types.ts src/lib/validation/coffeeSchema.ts src/components/coffees/CoffeeFormDialog.tsx src/components/coffees/CoffeeFormDialog.test.tsx`
Expected: no remaining lint errors.

- [ ] **Step 9: Commit**

```bash
git add brewdeck-web/src/lib/api/types.ts \
        brewdeck-web/src/lib/validation/coffeeSchema.ts \
        brewdeck-web/src/components/coffees/CoffeeFormDialog.tsx \
        brewdeck-web/src/components/coffees/CoffeeFormDialog.test.tsx
git commit -m "feat(web): score sliders replace tasting text inputs in coffee form"
```

---

### Task 3: Frontend — radar component + detail page

**Files:**
- Create: `brewdeck-web/src/components/coffees/CoffeeTastingRadar.tsx`
- Create: `brewdeck-web/src/components/coffees/CoffeeTastingRadar.test.tsx`
- Modify: `brewdeck-web/src/components/coffees/CoffeeDetailView.tsx`
- Modify: `brewdeck-web/src/components/coffees/CoffeeDetailView.test.tsx`

**Interfaces:**
- Consumes: `Coffee` type score fields from Task 2; `EmptyState` from `@/components/ui/EmptyState`.
- Produces: `CoffeeTastingRadar` — `export function CoffeeTastingRadar(props: { acidity: number | null; body: number | null; sweetness: number | null; bitterness: number | null }): JSX.Element`.

- [ ] **Step 1: Write the failing radar test**

Create `brewdeck-web/src/components/coffees/CoffeeTastingRadar.test.tsx`:

```tsx
import { screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { renderWithTheme } from '@/test/renderWithTheme';
import { CoffeeTastingRadar } from './CoffeeTastingRadar';

describe('CoffeeTastingRadar', () => {
  it('renders axis labels when all four scores are present', () => {
    renderWithTheme(
      <CoffeeTastingRadar acidity={4} body={3} sweetness={5} bitterness={2} />,
    );
    expect(screen.getByText('Acidity')).toBeInTheDocument();
    expect(screen.getByText('Body')).toBeInTheDocument();
    expect(screen.getByText('Sweetness')).toBeInTheDocument();
    expect(screen.getByText('Bitterness')).toBeInTheDocument();
  });

  it('shows an empty state when any score is missing', () => {
    renderWithTheme(
      <CoffeeTastingRadar acidity={4} body={null} sweetness={5} bitterness={2} />,
    );
    expect(screen.getByText(/add tasting scores/i)).toBeInTheDocument();
    expect(screen.queryByText('Acidity')).not.toBeInTheDocument();
  });
});
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `npm run test -- src/components/coffees/CoffeeTastingRadar.test.tsx`
Expected: FAIL — module `./CoffeeTastingRadar` not found.

- [ ] **Step 3: Implement the radar component**

Create `brewdeck-web/src/components/coffees/CoffeeTastingRadar.tsx`:

```tsx
'use client';

import Box from '@mui/material/Box';
import Card from '@mui/material/Card';
import CardContent from '@mui/material/CardContent';
import Typography from '@mui/material/Typography';
import {
  PolarAngleAxis,
  PolarGrid,
  PolarRadiusAxis,
  Radar,
  RadarChart,
  ResponsiveContainer,
} from 'recharts';
import { EmptyState } from '@/components/ui/EmptyState';

type CoffeeTastingRadarProps = {
  acidity: number | null;
  body: number | null;
  sweetness: number | null;
  bitterness: number | null;
};

export function CoffeeTastingRadar({
  acidity,
  body,
  sweetness,
  bitterness,
}: CoffeeTastingRadarProps) {
  const complete =
    acidity !== null && body !== null && sweetness !== null && bitterness !== null;

  const data = [
    { axis: 'Acidity', score: acidity },
    { axis: 'Body', score: body },
    { axis: 'Sweetness', score: sweetness },
    { axis: 'Bitterness', score: bitterness },
  ];

  return (
    <Card variant="outlined">
      <CardContent>
        <Typography variant="h6" component="h2" gutterBottom>
          Tasting profile
        </Typography>
        {complete ? (
          <Box sx={{ width: '100%', height: 280 }}>
            <ResponsiveContainer>
              <RadarChart data={data} margin={{ top: 8, right: 24, bottom: 8, left: 24 }}>
                <PolarGrid />
                <PolarAngleAxis dataKey="axis" fontSize={12} />
                <PolarRadiusAxis domain={[0, 5]} tickCount={6} fontSize={10} />
                <Radar
                  dataKey="score"
                  stroke="#1976d2"
                  fill="#1976d2"
                  fillOpacity={0.4}
                />
              </RadarChart>
            </ResponsiveContainer>
          </Box>
        ) : (
          <EmptyState message="Add tasting scores to see the flavor profile." />
        )}
      </CardContent>
    </Card>
  );
}
```

- [ ] **Step 4: Run the test to verify it passes**

Run: `npm run test -- src/components/coffees/CoffeeTastingRadar.test.tsx`
Expected: PASS — axis labels render for the complete case; empty-state text renders for the partial case.

- [ ] **Step 5: Wire the radar into `CoffeeDetailView`**

In `CoffeeDetailView.tsx`:

Add import:

```tsx
import { CoffeeTastingRadar } from '@/components/coffees/CoffeeTastingRadar';
```

Remove the four tasting rows from the `details` array (the `Acidity/Body/Sweetness/Bitterness` `orDash(...)` entries at lines 37–40).

After the `<Grid container>...</Grid>` details block (before the notes block), add:

```tsx
      <Box sx={{ mb: 3 }}>
        <CoffeeTastingRadar
          acidity={coffee.acidityScore}
          body={coffee.bodyScore}
          sweetness={coffee.sweetnessScore}
          bitterness={coffee.bitternessScore}
        />
      </Box>
```

(`Box` is already imported in this file.)

- [ ] **Step 6: Update `CoffeeDetailView.test.tsx`**

In the `coffee` fixture, replace the four string tasting fields:

```ts
  acidity: 'Media',
  body: 'Medio',
  sweetness: 'Media',
  bitterness: 'Baja',
```

with:

```ts
  acidityScore: 3,
  bodyScore: 3,
  sweetnessScore: 3,
  bitternessScore: 2,
```

Add a test that the radar heading renders:

```tsx
  it('renders the tasting profile radar', () => {
    mockHook({ isLoading: false, isError: false, data: coffee });
    renderWithTheme(<CoffeeDetailView coffeeId={1} />);
    expect(screen.getByRole('heading', { name: 'Tasting profile' })).toBeInTheDocument();
  });
```

- [ ] **Step 7: Run the coffee component tests**

Run: `npm run test -- src/components/coffees/`
Expected: PASS — detail, radar, and form tests all green.

- [ ] **Step 8: Type-check, lint, build**

Run: `npm run type-check`
Expected: no errors.
Run: `npm run lint:fix -- src/components/coffees/CoffeeTastingRadar.tsx src/components/coffees/CoffeeTastingRadar.test.tsx src/components/coffees/CoffeeDetailView.tsx src/components/coffees/CoffeeDetailView.test.tsx`
Expected: clean.
Run: `npm run build`
Expected: build succeeds (also type-checks).

- [ ] **Step 9: Commit**

```bash
git add brewdeck-web/src/components/coffees/CoffeeTastingRadar.tsx \
        brewdeck-web/src/components/coffees/CoffeeTastingRadar.test.tsx \
        brewdeck-web/src/components/coffees/CoffeeDetailView.tsx \
        brewdeck-web/src/components/coffees/CoffeeDetailView.test.tsx
git commit -m "feat(web): add tasting-profile radar to coffee detail page"
```

---

### Task 4: Postman + roadmap/state docs

**Files:**
- Modify: `docs/postman/brewdeck.postman_collection.json` (coffee create/update request bodies)
- Modify: `.claude/roadmap.md`
- Modify: `.claude/project-state.md`

- [ ] **Step 1: Update Postman coffee bodies**

In `docs/postman/brewdeck.postman_collection.json`, find the coffee **Create** and **Update** requests. In their JSON bodies, replace the string tasting fields:

```json
"acidity": "...",
"body": "...",
"sweetness": "...",
"bitterness": "..."
```

with numeric scores:

```json
"acidityScore": 4,
"bodyScore": 3,
"sweetnessScore": 5,
"bitternessScore": 2
```

(Keep valid JSON — mind trailing commas relative to surrounding fields.)

- [ ] **Step 2: Update the roadmap**

In `.claude/roadmap.md`, change the Phase 5 line:

```
- Coffee tasting notes visualization — Next (needs a coffee detail page)
```

to:

```
- Coffee tasting notes visualization (radar chart on coffee detail) — Done
```

- [ ] **Step 3: Update project state**

In `.claude/project-state.md`: bump "Last Updated" to `2026-07-06`; move the tasting-notes item from "Immediate Next Steps" into "Completed" / "Recently Worked On" with a one-line note (numeric 1–5 scores replaced free-text tasting fields; recharts radar on coffee detail). Update the "Current Phase" paragraph's "Next" pointer to the remaining Phase 5 items (AI suggestions, PDF export, share links).

- [ ] **Step 4: Commit**

```bash
git add docs/postman/brewdeck.postman_collection.json .claude/roadmap.md .claude/project-state.md
git commit -m "docs(project): mark tasting-notes radar done; update Postman coffee bodies"
```

---

## Self-Review

**Spec coverage:**
- Migration drop text + add score columns → Task 1 Step 1. ✓
- Entity Integer fields → Task 1 Step 2. ✓
- Request `@Min`/`@Max`, nullable → Task 1 Step 3 + tests Steps 9–10. ✓
- Response score fields + mapping → Task 1 Steps 4–5. ✓
- `CoffeeFilter` verify (no change) → Global Constraints note; confirmed no tasting refs. ✓
- FE types → Task 2 Step 1. ✓
- Zod coerce 1–5 optional → Task 2 Step 2. ✓
- Form sliders → Task 2 Steps 3–6. ✓
- Radar component + all-present-else-empty → Task 3 Steps 1–4. ✓
- Detail view swap → Task 3 Steps 5–6. ✓
- Backend + FE tests → Tasks 1–3. ✓
- Postman + roadmap/state → Task 4. ✓

**Placeholder scan:** No TBD/TODO; every code step shows code; test steps show test code. The two "adapt the file's existing helper" notes are unavoidable (existing test harness varies) but include a concrete fallback (`renderWithTheme(<... open ...>)`). ✓

**Type consistency:** Field names `acidityScore/bodyScore/sweetnessScore/bitternessScore` used identically across entity, `CoffeeRequest`, `CoffeeResponse`, `Coffee` TS type, zod schema, form `SCORE_FIELDS`, and radar props. Radar prop names are the short forms (`acidity/body/sweetness/bitterness`) mapped from `coffee.*Score` at the `CoffeeDetailView` call site (Task 3 Step 5) — intentional and consistent with the component's `Produces` interface. ✓

**Scope:** Single feature, one detail page + one resource's endpoints. Right-sized for one plan. Backend is one task by Java-compilation necessity; frontend split into type/form and radar/detail; docs separate. ✓
