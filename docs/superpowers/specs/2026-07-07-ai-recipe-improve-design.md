# AI Recipe Improve-from-History — Design Spec

**Date:** 2026-07-07
**Status:** Approved (design)
**Slice:** Second AI slice. Follows the generate slice (`docs/superpowers/specs/2026-07-06-ai-recipe-suggestions-design.md`, PR #58 merged).

## Goal

Let a user improve an existing recipe using its own brew-session history. A button on the recipe detail page calls Claude with the recipe's current parameters plus its recent rated brews; Claude returns adjusted brewing parameters that pre-fill the recipe edit dialog. The result is **ephemeral** — it fills the form for the user to review and save through the existing edit flow; nothing is auto-persisted.

## Non-Goals

- Persisting the suggestion or creating a new/linked recipe version (rejected in brainstorming — beyond one slice).
- A before/after diff panel (rejected — the ephemeral pre-fill reuses the generate slice's pattern).
- Changing the generate slice's endpoint or the `RecipeFormDialog`'s existing behavior.
- Sending unrated sessions, or an unbounded full history, to Claude.

## Decisions (from brainstorming)

1. **Output + entry point:** ephemeral pre-fill on the recipe detail page → opens the existing `RecipeFormDialog` pre-filled.
2. **History scope:** the most recent up-to-10 **rated** sessions (newest first), each with `rating`, `actualGrind`, `actualTemp`, `actualTime`, `tasteResult`, `adjustmentNotes`.
3. **Empty history:** frontend disables the button (with a tooltip) until the recipe has ≥1 rated session; backend still guards and returns **422** if called with no rated history.
4. **Architecture:** extend the existing `RecipeSuggestionPort` + `ClaudeRecipeSuggestionAdapter` with an `improve` method (Approach A) — reuse the Claude client, `SuggestedRecipePayload`, feature toggle, and 503 fail-soft path.

## Architecture

Reuse the generate slice's hexagonal structure and its infrastructure.

- **Domain port (extended):** `RecipeSuggestionPort` gains a second method
  `SuggestedRecipe improve(ImprovementContext context)`. The existing
  `suggest(SuggestionContext)` is unchanged.
- **Adapter (extended):** `ClaudeRecipeSuggestionAdapter` implements `improve`
  using the same `AnthropicClient`, `properties.model()` (`claude-haiku-4-5`),
  and structured output `SuggestedRecipePayload` (the same 8-field POJO the
  generate slice already parses into). A distinct system + user prompt frames
  the task as tuning existing params from rated outcomes. Same fail-soft:
  any `RuntimeException` is logged server-side (cause included) and wrapped in
  `AiUnavailableException` → HTTP 503; no SDK stack trace reaches the client.
  The adapter stays excluded from Sonar coverage.
- **Disabled bean:** `DisabledRecipeSuggestionAdapter` (the load-bearing
  `@ConditionalOnProperty(havingValue="false")` no-op port) gains an `improve`
  override that throws `AiUnavailableException`, so the context still loads
  when AI is disabled.
- **Service (new):** `RecipeImprovementService` — separate from
  `RecipeSuggestionService` because its inputs (a persisted recipe + its brew
  history) and its 422 precondition differ. It:
  1. checks `aiProperties.enabled()` → else `AiUnavailableException` (503);
  2. loads the recipe → else `EntityNotFoundException` (404);
  3. loads the recent rated sessions; if none → `InsufficientBrewHistoryException` (422);
  4. builds `ImprovementContext` and calls `port.improve(...)`;
  5. maps `SuggestedRecipe` → `SuggestedRecipeResponse` (the existing DTO).
- **Controller (new):** `RecipeImprovementController` with
  `POST /api/recipes/{id}/improve`, no request body, returns
  `SuggestedRecipeResponse` directly (not a `PageResponse` — a single result).
- **Exception + handler (new):** `InsufficientBrewHistoryException extends
  RuntimeException`; `GlobalExceptionHandler` maps it to HTTP **422
  Unprocessable Entity** with the standard `ErrorResponse` shape.

### Data flow

```
Recipe detail page
  └─ "Improve with AI" (enabled only if ≥1 rated session in loaded history)
       └─ POST /api/recipes/{id}/improve
            └─ RecipeImprovementService
                 ├─ toggle off  → 503
                 ├─ recipe missing → 404
                 ├─ no rated sessions → 422
                 └─ ImprovementContext → RecipeSuggestionPort.improve → Claude
                      └─ SuggestedRecipeResponse (8 fields)
       └─ onSuccess: open RecipeFormDialog pre-filled + rationale Alert
```

## Backend contract

### `POST /api/recipes/{id}/improve`

- **Path:** `id` — the recipe to improve (Long).
- **Body:** none.
- **200:** `SuggestedRecipeResponse` — the existing record
  `(BigDecimal coffeeGrams, BigDecimal waterGrams, String ratio,
  String grindSetting, Integer waterTemp, String brewTime, String steps,
  String rationale)`.
- **404:** recipe not found (`EntityNotFoundException`).
- **422:** recipe has no rated brew sessions (`InsufficientBrewHistoryException`).
- **503:** AI disabled or the SDK call failed (`AiUnavailableException`).

### New domain types

- `ImprovementContext` — a record carrying: coffee (`coffeeName`, `origin`,
  `roastLevel`, `process`, the four 1–5 tasting scores), method (`methodName`,
  `methodDescription`), the recipe's **current** params (`currentCoffeeGrams`,
  `currentWaterGrams`, `currentRatio`, `currentGrindSetting`,
  `currentWaterTemp`, `currentBrewTime`, `currentSteps`), and
  `List<BrewHistoryEntry> history`.
- `BrewHistoryEntry` — a record: `(Integer rating, String actualGrind,
  Integer actualTemp, String actualTime, String tasteResult,
  String adjustmentNotes)`.

### New repository query

On `BrewSessionRepository`:
`List<BrewSession> findTop10ByRecipeIdAndRatingIsNotNullOrderByBrewedAtDesc(Long recipeId)`
— the most recent up-to-10 rated sessions for the recipe. The service maps
these to `BrewHistoryEntry`.

## Frontend

- **`src/lib/api/ai.ts`:** add `improveRecipe(recipeId: number):
  Promise<SuggestedRecipe>` (POST to `/api/recipes/{id}/improve`, no body),
  reusing the existing `SuggestedRecipe` type.
- **`src/hooks/useImproveRecipe.ts`:** `useImproveRecipe()` — TanStack
  `useMutation` wrapping `improveRecipe`.
- **Recipe detail page:** an "Improve with AI" button near the recipe header.
  Enabled only when the already-loaded brew history contains ≥1 session with a
  non-null `rating`; otherwise disabled with a tooltip
  ("Log a rated brew to enable AI improvements"). No extra fetch — the detail
  page already loads brew history. `onSuccess` opens the existing
  `RecipeFormDialog` (edit mode, current recipe) with the suggested params
  applied, and shows the `rationale` in an info Alert. `onError` shows a
  user-facing Alert (covers the 422 "needs history" and 503 "unavailable"
  paths with distinct messages driven by the API error status).

The exact wiring of "open the edit dialog pre-filled with suggested values"
reuses `RecipeFormDialog`; whether that is done by seeding the dialog's initial
values or by `setValue` after open is an implementation detail for the plan,
consistent with how the generate slice fills fields.

## Error handling

| Condition | Exception | HTTP | Client behavior |
|---|---|---|---|
| AI toggle off / SDK failure | `AiUnavailableException` | 503 | "unavailable right now" Alert |
| Recipe id not found | `EntityNotFoundException` | 404 | error Alert |
| No rated sessions | `InsufficientBrewHistoryException` | 422 | "log a rated brew first" Alert (also pre-empted by the disabled button) |

Fail-soft in the adapter is unchanged from the generate slice: server-side log
with cause, opaque 503 to the client, never the SDK stack trace. No secret ever
logged; `ANTHROPIC_API_KEY` continues to come from the environment only.

## Testing

CI must stay green with **no API key** — the test profile keeps
`brewdeck.ai.enabled=false`, the service/controller tests mock the port, and
the adapter's `improve` mapping is unit-tested against a hand-built
`SuggestedRecipePayload`.

- **Service** (`RecipeImprovementServiceTest`, mocked port + repos): happy path;
  recipe-missing → 404; no-rated-history → 422; AI-disabled → 503; asserts the
  `ImprovementContext` is built from the loaded recipe + mapped history.
- **Controller** (`RecipeImprovementControllerTest`, MockMvc): 200 body shape;
  404; 422; 503.
- **Repository** (`BrewSessionRepositoryTest`): the new
  `findTop10ByRecipeIdAndRatingIsNotNullOrderByBrewedAtDesc` returns only rated
  sessions, newest first, capped at 10.
- **Adapter** (`ClaudeRecipeSuggestionAdapterTest`): the `improve` mapping
  reuses `toSuggestedRecipe(payload)` — assert the mapper (already covered);
  no live SDK call (adapter Sonar-excluded, validated by compile + manual run
  with a real key).
- **Frontend** (`useImproveRecipe`, recipe detail component): button disabled
  with no rated sessions; enabled with ≥1; success pre-fills the edit dialog +
  shows rationale; error shows the Alert. Full `vitest run` must pass — adding
  a hook to a shared component requires checking sibling tests that mount it
  (see the sibling-test regression from the generate slice).

## Global constraints (carried from the generate slice)

- `ANTHROPIC_API_KEY` from environment only — never in source, logs, or
  committed config.
- Model `claude-haiku-4-5` in `application.yaml` only; never hardcoded in Java;
  no `effort`/`thinking`/`budget_tokens` (Haiku rejects them).
- Feature toggle `brewdeck.ai.enabled` (default false); disabled → 503.
- Structured outputs via the typed `SuggestedRecipePayload` POJO — no free-text
  parsing.
- Endpoint returns the DTO directly, not `PageResponse`.
- Adapter excluded from Sonar coverage (mirrors JaCoCo DTO/adapter excludes).
- Validation messages avoid special symbols (write "degrees Celsius").
- Conventional Commits; scopes `api` / `web` / `docs`.
