# Numeric fields audit

Backlog item DATA-01. This doc lists every numeric (or number-like) field with its unit, range,
scale, storage and display, then the gaps found and the fixes planned. Checked against the code
on 2026-10-16. Where a fact comes from a probe and not from reading the code, the doc says so.

## Field inventory

| Field | Unit | Column | API type | Range / scale enforced | Form input | Display |
|---|---|---|---|---|---|---|
| `recipes.coffee_grams` | grams | `DECIMAL(6,2)` | `BigDecimal` | `@Positive`, `@DecimalMax 9999.99`, `@Digits(fraction = 2)` | `type=number step=any`, Zod `positive().max(9999.99)` + two decimals | `'{value} g'` |
| `recipes.water_grams` | grams | `DECIMAL(6,2)` | `BigDecimal` | same as coffee grams | same | `'{value} g'` |
| `recipes.ratio` | coffee:water, free text (`1:15`) | `VARCHAR(20)` | `String` | `@Size(max = 20)` | text | as typed |
| `recipes.water_temp` | °C | `NUMERIC(4,1)` (V31) | `BigDecimal` | `@DecimalMin 0`, `@DecimalMax 100`, `@Digits(3, 1)` | `type=number step=any`, Zod `min(0).max(100)` + one decimal | `'{value}°C'` |
| `recipes.brew_time` | free text (`2:30`) | `VARCHAR(40)` | `String` | `@Size(max = 20)` | text | as typed |
| `brew_sessions.actual_temp` | °C | `NUMERIC(4,1)` (V31) | `BigDecimal` | `@DecimalMin 0`, `@DecimalMax 100`, `@Digits(3, 1)` | `type=number step=any`, Zod `min(0).max(100)` + one decimal | `'{value}°C'` |
| `brew_sessions.actual_time` | free text | `VARCHAR(40)` | `String` | `@Size(max = 20)` | text | as typed |
| `brew_sessions.rating` | score 1–10 | `INTEGER` | `Integer` | `@Min 1`, `@Max 10` | `type=number`, Zod `int().min(1).max(10)` | `n/10` with a star |
| `coffees.*_score` (acidity, body, sweetness, bitterness) | score 1–5 | `INTEGER` | `Integer` | `@Min 1`, `@Max 5` | slider, Zod `int().min(1).max(5)` | radar chart, `'{value}/5'` |
| Average rating (recipe stats, top-rated, dashboard) | score 1–10 | computed `avg(rating)` | `Double`, full precision | n/a | n/a | `toFixed(1)`, or `—` when null |
| Counts (sessions, recipes, usage) | count | computed `count(*)` | `long` | n/a | n/a | plain integer |
| AI suggestion grams | grams | not stored until saved | `BigDecimal` | guardrails round to 1 decimal, `HALF_UP` | prefills the form | as above |

No table has a `CHECK` constraint on these columns. Bean Validation on the request records is the
only range guard.

## What is already right

- Grams are `DECIMAL(6,2)` mapped to `BigDecimal`, so there is no float drift in storage. The
  precision limit (9999.99) is enforced on both sides and tested
  (`ValidationLimitsIntegrationTest.recipe_gramsAboveTheColumnPrecision_isAFieldError`).
- Ratings and coffee scores are integers on both sides, including the Zod `int()`.
- Charts handle missing data:
  - `RecipeRatingTrend` drops unrated sessions.
  - `CoffeeTastingRadar` renders only when all four scores are set.
  - `MethodUsage` divides by `max(..., 1)`, so there is no `NaN` width.
- Average ratings are `null` (not `0`) when there is nothing to average. The UI shows `—` in that
  case.

## Gaps

| # | Gap | Evidence | Severity |
|---|---|---|---|
| G1 | A fractional temperature is silently truncated: `waterTemp: 92.7` is stored as `92` with `201 Created`, because Jackson accepts a float for an `Integer`. Same for `actualTemp`. The form allows it too (no Zod `int()`). | Probe on 2026-10-16 against `POST /api/recipes` | Medium: data changes without telling the user |
| G2 | Grams with more than two decimals: `coffeeGrams: 15.126` answers `15.126` in the `201` response, but the column stores `15.13`. The response and the database disagree until the next read. | Same probe; `DECIMAL(6,2)` rounds on insert | Medium |
| G3 | Unit messages print raw numbers. `'{value} g'` renders `15.5 g` in Spanish instead of `15,5 g`; `{value, number}` renders `15,5 g`. Affects `grams` and `celsius` in `RecipeCards`, `BrewSessionsTable`, `BrewSessionCards` and `BrewSessionDetailView`. `outOfFive` in `CoffeeCards` only ever gets integers, so it is unaffected. | `createTranslator` check on 2026-10-16 | Low |
| G4 | Averages are formatted with `toFixed(1)`, which ignores the locale (`8.3` in Spanish, should be `8,3`). It also rounds the binary double: an average of exactly 8.35 (167 / 20) shows `8.3`. | `DashboardView`, `TopRatedRecipes`, `RecipeDetailView` | Low |
| G5 | `ratio` is free text and is never checked against the grams. A recipe can say `1:15` with 15 g / 300 g (really 1:20). | `RecipeRequest`, `RecipeFormDialog` | Product decision, see below |
| G6 | Typing a decimal comma in a number field. **Assumption:** browsers normalise `type=number` input to a dot in `input.value`, so the form sees `15.5`; how each browser treats a typed comma depends on its locale. | Not yet checked in a browser | Needs a browser check in Spanish |
| G7 | `brew_time` / `actual_time` columns are `VARCHAR(40)` but validation caps them at 20. That is harmless, just inconsistent. | V1 vs `@Size(max = 20)` | Info, no change |

## Fix plan

Small PRs, each with tests:

1. **Temperatures with one decimal (G1). Done in #267.** Decided by the owner on 2026-10-16: water
   temperature must keep decimals, so the fix is to store them rather than reject them.
   - New Flyway migration: `recipes.water_temp` and `brew_sessions.actual_temp` go from `INTEGER`
     to `NUMERIC(4,1)` (0.0 to 100.0). Existing integers convert losslessly.
   - Entities, requests and responses use `BigDecimal`. The requests get `@DecimalMin("0")`,
     `@DecimalMax("100")` and `@Digits(integer = 3, fraction = 1)`, so `92.75` is a `400` field
     error instead of being rounded.
   - Carry the type through the AI path: `SuggestedRecipe`, `BrewHistoryEntry`, the prompts, both
     adapters, and the guardrails' temperature clamp.
   - Mirror in Zod: at most one decimal.
2. **Grams scale (G2). Done in #268; the AI guardrails also round model grams to two decimals so an accepted suggestion still saves.**
   - Add `@Digits(integer = 4, fraction = 2)` on both gram fields, so `15.126` becomes a `400`
     field error and the response always matches the stored value.
   - Mirror in Zod: at most two decimals.
3. **Locale-aware numbers in the UI (G3, G4).** This also covers the new decimal temperatures
   (`92,5 °C` in Spanish).
   - Change unit messages to `{value, number}`.
   - Replace `toFixed(1)` with one helper built on `Intl.NumberFormat(locale, { minimumFractionDigits: 1, maximumFractionDigits: 1 })`.
   - Round averages to one decimal `HALF_UP` in the API responses (`BigDecimal.valueOf(avg).setScale(1, HALF_UP)`), so 8.35 shows `8,4` / `8.4`. Ranking order keeps using the full value in SQL.
4. **Browser check (G6).** Type `15,5` in Chrome, Safari and Firefox with the UI in Spanish. Fix
   only if a browser drops or rejects it.
5. **Ratio (G5).** Option A, decided by the owner on 2026-10-16.

### Ratio (G5): decided, option A (2026-10-16)

- **Option A (recommended): derive it.** When both grams are set, the API computes
  `1:<water/coffee>` (one decimal) and stores it. The form shows it read-only next to the grams.
  Free text stays only for recipes without grams. Existing rows keep their text until they are
  edited.
- **Option B: hint only.** Keep free text, and show the computed ratio as a hint plus a warning
  when the typed one differs by more than 0.5.
- **Option C: leave it.**
