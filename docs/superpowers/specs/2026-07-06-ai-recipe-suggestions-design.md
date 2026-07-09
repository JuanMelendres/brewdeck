# AI Recipe Suggestions — Design Spec (Generate Slice)

> **Status:** Approved for planning. This spec covers the **first slice only**: AI-generated brew parameters for a coffee + brew method. The second flow (improve an existing recipe from its brew-session history) is a separate, later spec.

**Goal:** Let a user pick a coffee and a brew method and get Claude-generated brewing parameters (grind, ratio, dose, water, temperature, time, steps) plus a short rationale. The result is **ephemeral** — it pre-fills the recipe form so the user can review, edit, and save through the existing create flow. No recipe is persisted by the suggestion call itself.

**Tech stack:** Java 21, Spring Boot 3, `com.anthropic:anthropic-java` SDK, Bean Validation, JUnit 5 + Mockito + MockMvc. Next.js 15 / React 19 / TypeScript, MUI, react-hook-form + zod, TanStack Query, Vitest + React Testing Library.

---

## Decisions (locked)

- **Provider/model:** Real Claude API (Anthropic) via the official Java SDK. Model `claude-haiku-4-5`, held in config (`brewdeck.ai.model`) so it can be bumped without code changes.
- **Architecture:** Hexagonal. A `RecipeSuggestionPort` domain interface; a `ClaudeRecipeSuggestionAdapter` infrastructure implementation. The domain never imports the SDK.
- **Structured outputs:** The adapter uses Claude structured outputs (`output_config.format` = `json_schema`, strict) so the model reply validates against the brew-params schema and maps 1:1 to a record — no free-text parsing.
- **Ephemeral result:** the endpoint returns a DTO; nothing is written to the database. The frontend pre-fills the recipe form.
- **Result surface:** a "Suggest with AI" button inside `RecipeFormDialog` (not a separate screen).
- **Secrets:** `ANTHROPIC_API_KEY` from the environment only. `.env.example` documents it; never commit a real key.
- **Feature toggle:** `brewdeck.ai.enabled` (default `false`, driven by `AI_ENABLED`). When off (or key absent), the endpoint returns `503` cleanly.
- **CI:** tests never call the real API. The port is mocked in service/controller tests; the adapter's mapping is unit-tested against a hand-built SDK response. The test profile sets `brewdeck.ai.enabled=false`.

---

## Global Constraints

- Never leak JPA entities or SDK types from controllers — map through explicit records.
- Bean Validation messages contain no special symbols (responses are sanitized). Write "degrees Celsius", not the degree symbol.
- `POST /api/recipes/suggest` returns the DTO directly (it is an action, not a browsable collection) — **not** `PageResponse`.
- Water temperature bounds mirror `RecipeRequest`: 70–100 degrees Celsius.
- Suggested numeric fields reuse `RecipeRequest` types: `coffeeGrams`/`waterGrams` are `BigDecimal`, `waterTemp` is `Integer`.
- No secret ever appears in logs, source, or committed config.
- The adapter must fail soft: any SDK error, timeout, refusal, or malformed reply surfaces as an `AiUnavailableException` → `503`, never a raw stack trace to the client.

---

## Backend

### New package: `com.brewdeck.brewdeck_api.ai`

```
ai/
  RecipeSuggestionPort.java          (domain interface)
  SuggestionContext.java             (record: coffee + method + notes, port input)
  SuggestedRecipe.java               (record: port output — pure domain)
  RecipeSuggestionService.java       (loads entities, builds context, calls port)
  RecipeSuggestionController.java     (POST /api/recipes/suggest)
  SuggestRecipeRequest.java          (validated request record)
  SuggestedRecipeResponse.java       (response DTO record)
  AiUnavailableException.java        (thrown when disabled / upstream fails)
  ClaudeRecipeSuggestionAdapter.java (infrastructure: SDK adapter)
  AiProperties.java                  (@ConfigurationProperties for brewdeck.ai.*)
```

> The controller lives under `ai` (not `recipe`) to keep the AI concern isolated, even though the route is `/api/recipes/suggest` for REST consistency with the recipe resource.

### Config — `application.yaml`

```yaml
brewdeck:
  ai:
    enabled: ${AI_ENABLED:false}
    model: claude-haiku-4-5
    timeout-seconds: 20
    max-tokens: 1024
```

`AiProperties` binds `brewdeck.ai.*`. The Anthropic SDK reads `ANTHROPIC_API_KEY` from the environment (do not bind the key into a property — keep it out of the config tree). `application.yaml` used by the test profile sets `brewdeck.ai.enabled: false`.

`.env.example` gains:

```
# AI recipe suggestions (leave blank to keep the feature disabled)
AI_ENABLED=false
ANTHROPIC_API_KEY=
```

### Maven dependency — `pom.xml`

Add (pin the current version at implementation time):

```xml
<dependency>
  <groupId>com.anthropic</groupId>
  <artifactId>anthropic-java</artifactId>
  <version>2.34.0</version>
</dependency>
```

### Request / response contracts

`SuggestRecipeRequest` (validated):

```java
public record SuggestRecipeRequest(
    @NotNull(message = "Coffee id is required") Long coffeeId,
    @NotNull(message = "Brew method id is required") Long methodId,
    @Size(max = 500, message = "Notes must not exceed 500 characters") String notes) {}
```

`SuggestedRecipeResponse` (returned directly, ephemeral):

```java
public record SuggestedRecipeResponse(
    BigDecimal coffeeGrams,
    BigDecimal waterGrams,
    String ratio,
    String grindSetting,
    Integer waterTemp,
    String brewTime,
    String steps,
    String rationale) {}
```

`SuggestionContext` (port input) carries the resolved coffee attributes (name, origin, roastLevel, process, the four tasting scores) and the brew method (name, description) and the optional user notes — everything the prompt needs, with no entity leak. `SuggestedRecipe` is the port's output record with the same brew fields as the response (mapped by the service into `SuggestedRecipeResponse`).

### Port

```java
public interface RecipeSuggestionPort {
  SuggestedRecipe suggest(SuggestionContext context);
}
```

### Service — `RecipeSuggestionService`

1. If `!aiProperties.enabled()` → throw `AiUnavailableException` (before any lookup).
2. Load `Coffee` by `coffeeId` (→ `EntityNotFoundException` if missing) and `BrewMethod` by `methodId` (→ `EntityNotFoundException`).
3. Build `SuggestionContext` from those entities + `request.notes()`.
4. `port.suggest(context)` → map `SuggestedRecipe` → `SuggestedRecipeResponse`.
5. Structured service log at info on success (coffeeId, methodId, model) — never the key, never the full prompt.

### Controller — `RecipeSuggestionController`

```java
@PostMapping("/api/recipes/suggest")
public ResponseEntity<SuggestedRecipeResponse> suggest(@Valid @RequestBody SuggestRecipeRequest request)
```

Returns `200 OK` with the DTO (it does not create a resource, so not `201`).

### Adapter — `ClaudeRecipeSuggestionAdapter implements RecipeSuggestionPort`

- Constructed only when `brewdeck.ai.enabled=true` (`@ConditionalOnProperty`) — but the **service** owns the disabled-path `503`, so when disabled the bean simply isn't wired and the service short-circuits before touching the port. (Wire a no-op/throwing fallback bean is unnecessary because the service guards first.)
- Builds an `AnthropicClient` from `AnthropicOkHttpClient.fromEnv()` (reads `ANTHROPIC_API_KEY`), with the configured timeout.
- Calls `client.messages().create(...)` with:
  - `model` from config, `maxTokens` from config,
  - a system prompt establishing the barista role and the strict "return only the JSON matching the schema" instruction,
  - a user message describing the coffee + method + notes,
  - `output_config.format` = a `json_schema` with `additionalProperties:false` and the brew-params fields required, so the reply is schema-valid.
- Parse the structured reply into `SuggestedRecipe`.
- Any `AnthropicServiceException`, timeout, `stop_reason == "refusal"`, or missing/invalid JSON → wrap in `AiUnavailableException` (fail soft). Log the cause at warn (no key, no PII).

### Error handling — `GlobalExceptionHandler`

Add a handler for `AiUnavailableException` → `503 Service Unavailable` with the standard `ErrorResponse` shape and message `"AI suggestion service is unavailable"`. Existing `EntityNotFoundException` handler already yields `404` for unknown coffee/method; existing `MethodArgumentNotValidException` handler yields the `400` validation body.

---

## Frontend

### API client + hook

- `src/lib/api/ai.ts`:
  - types `SuggestRecipeInput { coffeeId: number; methodId: number; notes?: string }` and `SuggestedRecipe { coffeeGrams: number | null; waterGrams: number | null; ratio: string | null; grindSetting: string | null; waterTemp: number | null; brewTime: string | null; steps: string | null; rationale: string }`.
  - `suggestRecipe(body: SuggestRecipeInput): Promise<SuggestedRecipe>` via `apiFetch`.
- `src/hooks/useSuggestRecipe.ts` — `useMutation` wrapping `suggestRecipe`. No cache invalidation (nothing is persisted).

### `RecipeFormDialog` changes

- Add a **"Suggest with AI"** button near the top of the form.
- Enabled only when `coffeeId` and `methodId` are both selected; disabled + spinner while the mutation is pending.
- On success: `setValue` for `coffeeGrams`, `waterGrams`, `ratio`, `grindSetting`, `waterTemp`, `brewTime`, `steps` from the response (only for non-null fields), and surface `rationale` as helper text / an info `Alert` above the fields. The user then edits and submits through the existing create mutation.
- On error (including `503`): show a user-visible error state (inline `Alert` or toast) — "AI suggestions are unavailable right now." Never `console.error` as the only signal.
- The feature is safe to leave visible when the backend is disabled: a click returns `503` and shows the error state. (No separate public config flag needed for the first slice.)

### Frontend tests (Vitest + RTL)

- Button is disabled until coffee + method are selected.
- On mocked hook success, the form fields are populated and the rationale text renders.
- On mocked hook error, the error state renders and no fields are populated.
- Mock the `useSuggestRecipe` hook (and the mutation hooks already mocked in the existing `RecipeFormDialog.test.tsx`).

---

## Testing (backend)

- **`RecipeSuggestionServiceTest`** (Mockito): mock `RecipeSuggestionPort`, `CoffeeRepository`, `BrewMethodRepository`, `AiProperties`.
  - disabled → `AiUnavailableException`, port never called.
  - unknown coffee → `EntityNotFoundException`.
  - unknown method → `EntityNotFoundException`.
  - happy path → context built from entities + notes, `SuggestedRecipe` mapped to `SuggestedRecipeResponse`.
- **`RecipeSuggestionControllerTest`** (MockMvc, `@WebMvcTest`): mock `RecipeSuggestionService`.
  - `200` + body shape on success.
  - `400` + `validationErrors` when `coffeeId`/`methodId` missing.
  - `503` when the service throws `AiUnavailableException`.
  - `404` when the service throws `EntityNotFoundException`.
- **`ClaudeRecipeSuggestionAdapterTest`** (pure unit): feed a hand-built SDK message object (structured-output content) to the mapping method and assert the `SuggestedRecipe`; feed a refusal / malformed reply and assert `AiUnavailableException`. **No network, no real key.**
- No Testcontainers/integration test hits the real Anthropic API. The real adapter is exercised only manually with a local key.

---

## Verification

- Backend: `./mvnw spotless:apply` then `./mvnw clean verify` (test profile has `brewdeck.ai.enabled=false`, so no key is needed for the build to pass).
- Frontend (in `brewdeck-web/`): `npm run test`, `npm run type-check`, `npm run lint:fix -- <changed files>`, `npm run build`.
- Manual smoke (local, optional): set `AI_ENABLED=true` + `ANTHROPIC_API_KEY`, hit `POST /api/recipes/suggest`, confirm a schema-valid body.

---

## Commits (Conventional)

1. `feat(api): add Claude recipe-suggestion port, adapter and endpoint`
2. `feat(web): add AI suggest-recipe button to the recipe form`
3. `docs(project): record AI recipe suggestions (generate slice)` — also folds the pending project-state update (PR #57 tasting radar merged), updates the roadmap AI line to In Progress, and adds a Postman `POST /api/recipes/suggest` request.

---

## Self-Review

**Spec coverage:** provider/model (Haiku, config) ✓; port + adapter split ✓; structured outputs ✓; ephemeral pre-fill (no persist) ✓; endpoint shape (DTO not PageResponse) ✓; validation + 404 + 503 paths ✓; secret handling + toggle ✓; CI-without-key test strategy ✓; frontend button + hook + tests ✓; verification commands ✓; commit plan ✓.

**Placeholder scan:** the only deferred item is the exact `anthropic-java` version, pinned at implementation time against the current release. No TBD in behavior.

**Ambiguity check:** "improve an existing recipe" is explicitly out of scope for this spec (second slice). The controller package (`ai`) vs route (`/api/recipes/suggest`) split is stated so it isn't misread as belonging in the `recipe` package.

**Scope:** one endpoint, one adapter, one frontend button. Right-sized for a single implementation plan.
