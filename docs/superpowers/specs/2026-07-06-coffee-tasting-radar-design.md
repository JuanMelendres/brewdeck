# Coffee Tasting-Notes Radar — Design

Date: 2026-07-06
Status: Approved
Phase: 5 (Product Improvements)

## Goal

Visualize a coffee's flavor profile as a radar/spider chart on the coffee detail
page (`/coffees/[id]`). Axes: Acidity, Body, Sweetness, Bitterness.

## Problem

The four tasting dimensions (`acidity`, `body`, `sweetness`, `bitterness`) are
free-text `VARCHAR(50)` columns today, rendered as plain text cards on the
detail page. A radar chart needs numeric values on a shared scale. Free text
cannot drive a chart reliably.

## Decision

Replace the four free-text tasting columns with numeric score columns (integer
scale 1–5). Full-stack change: DB migration, entity, DTOs, validation, form,
detail view, tests, Postman. No coffee seed data exists, so dropping the text
columns loses no shipped data. `notesPrimary` / `notesSecondary` prose fields
are unaffected — they keep the descriptive tasting language.

Rejected alternatives:
- **Frontend text→number mapping**: fragile; free text won't map cleanly.
- **Keep text + add scores**: redundant, doubles the tasting inputs in the form.

## Architecture

### 1. Data model & migration

Flyway `V3__coffee_tasting_scores.sql`:
- Drop columns `acidity`, `body`, `sweetness`, `bitterness` (VARCHAR).
- Add columns `acidity_score`, `body_score`, `sweetness_score`,
  `bitterness_score` — `SMALLINT NULL`, semantic range 1–5.

Range enforced in the application layer (Bean Validation), not a DB CHECK, so
violation messages flow through `GlobalExceptionHandler` consistently. Scores
are nullable — a coffee may be un-scored.

Entity `Coffee`: replace the four `String` tasting fields with four nullable
`Integer` fields (`acidityScore`, `bodyScore`, `sweetnessScore`,
`bitternessScore`) plus getters/setters.

### 2. Backend DTO, validation, API

`CoffeeRequest` (record): replace the four `String` tasting fields with four
nullable `Integer` score fields, each annotated:

```java
@Min(value = 1, message = "Acidity score must be at least 1")
@Max(value = 5, message = "Acidity score must not exceed 5")
Integer acidityScore,
```

(`@Min`/`@Max` only fire when the value is present, so null stays valid.)

`CoffeeResponse` (record): four `Integer` score fields, mapped from the entity
in `from(...)`.

No new endpoint. Scores travel on the existing
`POST` / `PUT` `/api/coffees` and `GET /api/coffees/{id}`. The radar reuses the
coffee-by-id payload the detail page already fetches — zero new network calls.

`CoffeeFilter`: if it references the dropped text columns, remove those clauses
(verify during implementation).

### 3. Frontend types, form, radar

Types (`src/lib/api/coffees.ts` / `types.ts`): replace
`acidity/body/sweetness/bitterness: string` with
`acidityScore/bodyScore/sweetnessScore/bitternessScore: number | null`.

Zod (`src/lib/validation/coffeeSchema.ts`): replace the four text fields with
`z.coerce.number().int().min(1).max(5).optional()` (×4). `z.coerce` accepts the
numeric slider value; empty stays `undefined`.

Form (`CoffeeFormDialog.tsx`): replace the four text inputs with four MUI
`Slider` controls (discrete, marks, range 1–5). Unset/empty allowed.

New `components/coffees/CoffeeTastingRadar.tsx`:
- Props: the four scores (`number | null`).
- If **any** score is null → render `EmptyState`
  ("Add tasting scores to see the flavor profile"). The radar only plots
  complete profiles, avoiding misleading zero-spikes.
- Else render recharts `RadarChart` + `PolarGrid` + `PolarAngleAxis`
  (four axes: Acidity / Body / Sweetness / Bitterness) + `Radar` series, domain
  `[0, 5]`, wrapped in `ResponsiveContainer`. Follow the rating-trend chart's
  conventions.

`CoffeeDetailView.tsx`: remove the four tasting `StatCard`s; render
`<CoffeeTastingRadar>` in that slot. The `notesPrimary` / `notesSecondary` and
description blocks are unchanged.

## Empty / partial-data behavior

A radar is drawn only when all four scores are present. Any missing score →
empty state. This keeps the shape honest (a partial profile with implicit zeros
would look like a real low score).

## Testing

### Backend
- Controller (MockMvc): POST/PUT with valid scores → 201/200; score `0` or `6`
  → 400 with `validationErrors.acidityScore`; null scores accepted.
- Mapping: `CoffeeResponse.from` carries scores through.
- Integration (Testcontainers): create coffee with scores, GET-by-id returns
  them. Update existing coffee tests that referenced the dropped text fields.
- `CoffeeFilter` specification tests: drop/adjust cases on removed columns.

### Frontend (Vitest + RTL)
- `CoffeeTastingRadar.test.tsx`: all four scores → four axis labels present
  (`getByText`); any null → empty-state text, no chart.
- `CoffeeFormDialog.test.tsx`: sliders present; submit sends numeric scores.
- `CoffeeDetailView.test.tsx`: mock coffee with scores → radar; without → empty
  state. Remove old text-card assertions.
- Recharts needs `ResponsiveContainer` sizing under jsdom — reuse the
  rating-trend chart test's existing mock/technique.

## Verification
- Backend: `./mvnw spotless:apply` then `./mvnw clean verify`.
- Frontend (`brewdeck-web/`): `npm run test`, `npm run type-check`,
  `npm run lint:fix -- <changed files>`, `npm run build`.

## Postman & docs
- Update Postman coffee create/update bodies: text tasting fields → score
  fields.
- Update `.claude/roadmap.md` and `.claude/project-state.md`: mark
  tasting-notes visualization done.

## Commit plan (~4)
1. `feat(api)`: migration + entity + DTO + validation.
2. `test(api)`: controller / mapping / integration / specification.
3. `feat(web)`: radar component + form sliders + detail view.
4. `test(web)` + `docs`: frontend tests, Postman, roadmap/state.

Group during implementation as changes settle.
