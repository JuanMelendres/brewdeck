# Recipe Public Share Links Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let a user create an unguessable, revocable public link to a single recipe, and let anyone with the link open a standalone read-only recipe card.

**Architecture:** Backend adds a nullable `share_token` to `recipes` (partial-unique), share/unshare service methods that generate/clear a secure-random base64url token, and a curated `PublicRecipeResponse` served from a new `/api/public/recipes/{token}` endpoint (404 on unknown/revoked). Frontend adds share/unshare mutations invalidating the recipe detail query, a `ShareRecipeDialog` opened from the recipe detail, and a client-rendered `/share/[token]` public page.

**Tech Stack:** Java 21 / Spring Boot 3 / Spring Data JPA / Flyway / PostgreSQL 16 / Testcontainers / JUnit 5 / Mockito / MockMvc — Next.js 15 App Router / React 19 / TypeScript / MUI / TanStack Query / Vitest + RTL.

## Global Constraints

- **Backend:** organize by domain (`recipe`); never return JPA entities from controllers — map to records; single-resource GET returns the DTO directly (not `PageResponse`); no special symbols like `°C` in messages (write "degrees Celsius").
- **Token:** 16 bytes from a single static `SecureRandom`, `Base64.getUrlEncoder().withoutPadding().encodeToString(...)`. No external dependency.
- **`PublicRecipeResponse` fields:** `name`, `coffeeName`, `methodName`, `coffeeGrams` (BigDecimal), `waterGrams` (BigDecimal), `ratio`, `grindSetting`, `waterTemp` (Integer), `brewTime`, `steps`, `expectedTaste`. **No id, no favorite, no timestamps.**
- **Idempotency:** re-sharing returns the existing token (no new token); unsharing an already-unshared recipe is a safe no-op returning 200.
- **Not-found:** `throw new EntityNotFoundException(RECIPE_NOT_FOUND)` → `GlobalExceptionHandler` 404.
- **Frontend:** strict TypeScript, no `any`; import domain types from `@/lib/api`; named exports everywhere except Next.js `page.tsx` (which uses `export default`); absolute `@/` imports; TanStack Query for all server state with correct key invalidation (`recipe(id)` on share/unshare); handle loading / error / empty visibly (no `console.error`).
- **Commands:** frontend from `brewdeck-web/` (`npm run test`, `npm run type-check`, `npm run build`; scope `lint:fix` to changed files). Backend `./mvnw spotless:apply` then `./mvnw clean verify` from `brewdeck-api/`.
- **Full `vitest run`** after Task 3 — the Share button is added to the shared `RecipeDetailView` (sibling tests mount it; see the sibling-test regression in project memory).
- Conventional Commits; scopes `api` (backend), `web` (frontend), `docs`.

---

### Task 1: Backend — share token, service, endpoints, public controller

**Files:**
- Create: `brewdeck-api/src/main/resources/db/migration/V4__recipe_share_token.sql`
- Modify: `brewdeck-api/src/main/java/com/brewdeck/brewdeck_api/recipe/Recipe.java`
- Modify: `brewdeck-api/src/main/java/com/brewdeck/brewdeck_api/recipe/RecipeRepository.java`
- Modify: `brewdeck-api/src/main/java/com/brewdeck/brewdeck_api/recipe/RecipeResponse.java`
- Create: `brewdeck-api/src/main/java/com/brewdeck/brewdeck_api/recipe/PublicRecipeResponse.java`
- Modify: `brewdeck-api/src/main/java/com/brewdeck/brewdeck_api/recipe/RecipeService.java`
- Modify: `brewdeck-api/src/main/java/com/brewdeck/brewdeck_api/recipe/RecipeController.java`
- Create: `brewdeck-api/src/main/java/com/brewdeck/brewdeck_api/recipe/PublicRecipeController.java`
- Test: `brewdeck-api/src/test/java/com/brewdeck/brewdeck_api/recipe/RecipeServiceTest.java` (modify)
- Test: `brewdeck-api/src/test/java/com/brewdeck/brewdeck_api/recipe/RecipeRepositoryTest.java` (modify)
- Test: `brewdeck-api/src/test/java/com/brewdeck/brewdeck_api/recipe/RecipeControllerTest.java` (modify)
- Test: `brewdeck-api/src/test/java/com/brewdeck/brewdeck_api/recipe/PublicRecipeControllerTest.java` (create)
- Test: an existing integration test class under `brewdeck-api/src/test/java/com/brewdeck/brewdeck_api/integration/` (add a share-flow test)

**Interfaces:**
- Consumes: existing `Recipe` entity (Lombok `@Getter/@Setter`), `RecipeResponse.fromEntity(Recipe)`, `findRecipeById(Long)` (throws `EntityNotFoundException(RECIPE_NOT_FOUND)`), `GlobalExceptionHandler` (maps `EntityNotFoundException` → 404).
- Produces (later tasks / other backend code rely on these exact signatures):
  - `RecipeService.share(Long id): RecipeResponse`
  - `RecipeService.unshare(Long id): RecipeResponse`
  - `RecipeService.getByShareToken(String token): PublicRecipeResponse`
  - `RecipeRepository.findByShareToken(String): Optional<Recipe>`
  - `RecipeResponse` gains trailing field `String shareToken`
  - `PublicRecipeResponse(name, coffeeName, methodName, coffeeGrams, waterGrams, ratio, grindSetting, waterTemp, brewTime, steps, expectedTaste)` with `fromEntity(Recipe)`
  - `PATCH /api/recipes/{id}/share` → 200 `RecipeResponse`
  - `PATCH /api/recipes/{id}/unshare` → 200 `RecipeResponse`
  - `GET /api/public/recipes/{token}` → 200 `PublicRecipeResponse` or 404

- [ ] **Step 1: Write the migration**

Create `brewdeck-api/src/main/resources/db/migration/V4__recipe_share_token.sql`:

```sql
ALTER TABLE recipes ADD COLUMN share_token VARCHAR(32);

CREATE UNIQUE INDEX ux_recipes_share_token
    ON recipes (share_token)
    WHERE share_token IS NOT NULL;
```

- [ ] **Step 2: Add the entity field**

In `Recipe.java`, add the field alongside the other columns (e.g. after `favorite`):

```java
  @Column(name = "share_token", unique = true)
  private String shareToken;
```

- [ ] **Step 3: Write the failing repository test**

In `RecipeRepositoryTest.java`, add (adapt the existing helpers this class already uses to persist a `Recipe` with a coffee + method):

```java
  @Test
  void findByShareToken_returnsRecipeWhenTokenPresent() {
    Recipe recipe = persistRecipe();
    recipe.setShareToken("tok-abc123");
    recipeRepository.saveAndFlush(recipe);

    Optional<Recipe> found = recipeRepository.findByShareToken("tok-abc123");

    assertThat(found).isPresent();
    assertThat(found.get().getId()).isEqualTo(recipe.getId());
  }

  @Test
  void findByShareToken_returnsEmptyWhenTokenAbsent() {
    assertThat(recipeRepository.findByShareToken("does-not-exist")).isEmpty();
  }
```

> If `RecipeRepositoryTest` has no `persistRecipe()` helper, inline the same persistence steps the other tests in the file use (create + save coffee, method, then recipe). Reuse the file's existing imports; add `import java.util.Optional;` if missing.

- [ ] **Step 4: Run the repository test to verify it fails**

Run: `./mvnw -Dtest=RecipeRepositoryTest test`
Expected: FAIL — `findByShareToken` is not defined on `RecipeRepository`.

- [ ] **Step 5: Add the repository method**

In `RecipeRepository.java`, add (and `import java.util.Optional;` is already present):

```java
  @EntityGraph(attributePaths = {"coffee", "method"})
  Optional<Recipe> findByShareToken(String shareToken);
```

- [ ] **Step 6: Run the repository test to verify it passes**

Run: `./mvnw -Dtest=RecipeRepositoryTest test`
Expected: PASS.

- [ ] **Step 7: Create the public DTO**

Create `PublicRecipeResponse.java`:

```java
package com.brewdeck.brewdeck_api.recipe;

import java.math.BigDecimal;

public record PublicRecipeResponse(
    String name,
    String coffeeName,
    String methodName,
    BigDecimal coffeeGrams,
    BigDecimal waterGrams,
    String ratio,
    String grindSetting,
    Integer waterTemp,
    String brewTime,
    String steps,
    String expectedTaste) {
  public static PublicRecipeResponse fromEntity(Recipe recipe) {
    return new PublicRecipeResponse(
        recipe.getName(),
        recipe.getCoffee().getName(),
        recipe.getMethod().getName(),
        recipe.getCoffeeGrams(),
        recipe.getWaterGrams(),
        recipe.getRatio(),
        recipe.getGrindSetting(),
        recipe.getWaterTemp(),
        recipe.getBrewTime(),
        recipe.getSteps(),
        recipe.getExpectedTaste());
  }
}
```

- [ ] **Step 8: Add `shareToken` to `RecipeResponse`**

In `RecipeResponse.java`, add `shareToken` as the final record component and map it in `fromEntity`:

```java
public record RecipeResponse(
    Long id,
    Long coffeeId,
    String coffeeName,
    Long methodId,
    String methodName,
    String name,
    BigDecimal coffeeGrams,
    BigDecimal waterGrams,
    String ratio,
    String grindSetting,
    Integer waterTemp,
    String brewTime,
    String steps,
    String expectedTaste,
    Boolean favorite,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    String shareToken) {
  public static RecipeResponse fromEntity(Recipe recipe) {
    return new RecipeResponse(
        recipe.getId(),
        recipe.getCoffee().getId(),
        recipe.getCoffee().getName(),
        recipe.getMethod().getId(),
        recipe.getMethod().getName(),
        recipe.getName(),
        recipe.getCoffeeGrams(),
        recipe.getWaterGrams(),
        recipe.getRatio(),
        recipe.getGrindSetting(),
        recipe.getWaterTemp(),
        recipe.getBrewTime(),
        recipe.getSteps(),
        recipe.getExpectedTaste(),
        recipe.getFavorite(),
        recipe.getCreatedAt(),
        recipe.getUpdatedAt(),
        recipe.getShareToken());
  }
}
```

> If any existing test constructs `RecipeResponse` positionally (not via `fromEntity`), add the trailing `null`/token argument there too. Prefer `fromEntity` in tests.

- [ ] **Step 9: Write the failing service tests**

In `RecipeServiceTest.java`, add (match the file's existing Mockito setup — `@Mock RecipeRepository recipeRepository;`, `@InjectMocks RecipeService recipeService;`, and the existing helper that builds a `Recipe` with a coffee + method; call it `recipe()` below — adapt to the real helper name):

```java
  @Test
  void share_generatesTokenOnFirstShare() {
    Recipe recipe = recipe();
    recipe.setShareToken(null);
    when(recipeRepository.findById(1L)).thenReturn(Optional.of(recipe));
    when(recipeRepository.save(any(Recipe.class))).thenAnswer(inv -> inv.getArgument(0));

    RecipeResponse response = recipeService.share(1L);

    assertThat(response.shareToken()).isNotBlank();
    assertThat(recipe.getShareToken()).isEqualTo(response.shareToken());
    verify(recipeRepository).save(recipe);
  }

  @Test
  void share_isIdempotentWhenAlreadyShared() {
    Recipe recipe = recipe();
    recipe.setShareToken("existing-token");
    when(recipeRepository.findById(1L)).thenReturn(Optional.of(recipe));

    RecipeResponse response = recipeService.share(1L);

    assertThat(response.shareToken()).isEqualTo("existing-token");
    verify(recipeRepository, never()).save(any(Recipe.class));
  }

  @Test
  void share_throwsWhenRecipeMissing() {
    when(recipeRepository.findById(99L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> recipeService.share(99L))
        .isInstanceOf(EntityNotFoundException.class);
  }

  @Test
  void unshare_clearsToken() {
    Recipe recipe = recipe();
    recipe.setShareToken("existing-token");
    when(recipeRepository.findById(1L)).thenReturn(Optional.of(recipe));
    when(recipeRepository.save(any(Recipe.class))).thenAnswer(inv -> inv.getArgument(0));

    RecipeResponse response = recipeService.unshare(1L);

    assertThat(response.shareToken()).isNull();
    assertThat(recipe.getShareToken()).isNull();
  }

  @Test
  void getByShareToken_returnsCuratedDto() {
    Recipe recipe = recipe();
    recipe.setShareToken("tok-1");
    when(recipeRepository.findByShareToken("tok-1")).thenReturn(Optional.of(recipe));

    PublicRecipeResponse response = recipeService.getByShareToken("tok-1");

    assertThat(response.name()).isEqualTo(recipe.getName());
    assertThat(response.coffeeName()).isEqualTo(recipe.getCoffee().getName());
    assertThat(response.methodName()).isEqualTo(recipe.getMethod().getName());
  }

  @Test
  void getByShareToken_throwsWhenTokenUnknown() {
    when(recipeRepository.findByShareToken("nope")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> recipeService.getByShareToken("nope"))
        .isInstanceOf(EntityNotFoundException.class);
  }
```

> Ensure these static imports exist in the test file: `org.assertj.core.api.Assertions.assertThat`, `assertThatThrownBy`, and `org.mockito.Mockito.*` (`when`, `verify`, `never`, `any`). Add `import java.util.Optional;` and `import jakarta.persistence.EntityNotFoundException;` if missing.

- [ ] **Step 10: Run the service tests to verify they fail**

Run: `./mvnw -Dtest=RecipeServiceTest test`
Expected: FAIL — `share`, `unshare`, `getByShareToken` not defined.

- [ ] **Step 11: Add the service methods**

In `RecipeService.java`, add the imports and methods. Add near the top of the class body a single static `SecureRandom`:

```java
  private static final java.security.SecureRandom SECURE_RANDOM = new java.security.SecureRandom();
```

Add the write + read methods (place the writes beside `removeFromFavorites`):

```java
  @Transactional
  public RecipeResponse share(Long id) {
    Recipe recipe = findRecipeById(id);
    if (recipe.getShareToken() == null) {
      recipe.setShareToken(generateToken());
      recipe = recipeRepository.save(recipe);
      log.info("Shared recipe id={}", recipe.getId());
    }
    return RecipeResponse.fromEntity(recipe);
  }

  @Transactional
  public RecipeResponse unshare(Long id) {
    Recipe recipe = findRecipeById(id);
    recipe.setShareToken(null);
    Recipe saved = recipeRepository.save(recipe);
    log.info("Unshared recipe id={}", saved.getId());
    return RecipeResponse.fromEntity(saved);
  }

  public PublicRecipeResponse getByShareToken(String token) {
    return recipeRepository
        .findByShareToken(token)
        .map(PublicRecipeResponse::fromEntity)
        .orElseThrow(() -> new EntityNotFoundException(RECIPE_NOT_FOUND));
  }

  private String generateToken() {
    byte[] bytes = new byte[16];
    SECURE_RANDOM.nextBytes(bytes);
    return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }
```

> Prefer top-of-file imports over fully-qualified names if the file's style uses explicit imports; `spotless:apply` will normalize either way. `EntityNotFoundException` is already imported in this file.

- [ ] **Step 12: Run the service tests to verify they pass**

Run: `./mvnw -Dtest=RecipeServiceTest test`
Expected: PASS.

- [ ] **Step 13: Add the controller endpoints**

In `RecipeController.java`, add after `removeFromFavorites` (before the closing brace):

```java
  @PatchMapping("/{id}/share")
  @Operation(summary = "Create a public share link for a recipe")
  public ResponseEntity<RecipeResponse> share(@PathVariable Long id) {
    return ResponseEntity.ok(recipeService.share(id));
  }

  @PatchMapping("/{id}/unshare")
  @Operation(summary = "Revoke a recipe's public share link")
  public ResponseEntity<RecipeResponse> unshare(@PathVariable Long id) {
    return ResponseEntity.ok(recipeService.unshare(id));
  }
```

- [ ] **Step 14: Create the public controller**

Create `PublicRecipeController.java`:

```java
package com.brewdeck.brewdeck_api.recipe;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/recipes")
@RequiredArgsConstructor
@Tag(name = "Public Recipes", description = "Read-only public recipe access via share token")
public class PublicRecipeController {

  private final RecipeService recipeService;

  @GetMapping("/{token}")
  @Operation(summary = "Get a shared recipe by its public token")
  public ResponseEntity<PublicRecipeResponse> getByToken(@PathVariable String token) {
    return ResponseEntity.ok(recipeService.getByShareToken(token));
  }
}
```

- [ ] **Step 15: Write the failing controller tests**

In `RecipeControllerTest.java` (MockMvc + `@MockBean RecipeService recipeService;` — match the file's existing setup), add:

```java
  @Test
  void share_returns200WithShareToken() throws Exception {
    RecipeResponse shared = sampleResponseWithShareToken("Xk7pQ2mN8vL4wR9tYbZ0aQ");
    when(recipeService.share(1L)).thenReturn(shared);

    mockMvc
        .perform(patch("/api/recipes/1/share"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.shareToken").value("Xk7pQ2mN8vL4wR9tYbZ0aQ"));
  }

  @Test
  void unshare_returns200WithNullShareToken() throws Exception {
    RecipeResponse unshared = sampleResponseWithShareToken(null);
    when(recipeService.unshare(1L)).thenReturn(unshared);

    mockMvc
        .perform(patch("/api/recipes/1/unshare"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.shareToken").doesNotExist());
  }
```

Add a small helper in the test class (adapt to the existing sample-builder if one exists):

```java
  private RecipeResponse sampleResponseWithShareToken(String token) {
    return new RecipeResponse(
        1L, 1L, "Ethiopia", 1L, "V60", "Morning Cup",
        new java.math.BigDecimal("15.0"), new java.math.BigDecimal("250.0"),
        "1:16", "Medium", 94, "3:00", "Bloom then pour", "Floral",
        false, java.time.LocalDateTime.now(), null, token);
  }
```

> `jsonPath("$.shareToken").doesNotExist()` holds because a `null` record component is omitted by the default Jackson config in this project. If the project serializes nulls, change the assertion to `.value(nullValue())` with `org.hamcrest.Matchers.nullValue`. Ensure `import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;` is present.

Create `PublicRecipeControllerTest.java` (mirror the structure of `RecipeControllerTest` — `@WebMvcTest(PublicRecipeController.class)`, `@MockBean RecipeService recipeService;`):

```java
package com.brewdeck.brewdeck_api.recipe;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PublicRecipeController.class)
class PublicRecipeControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockBean private RecipeService recipeService;

  @Test
  void getByToken_returns200WithCuratedBodyAndNoInternalFields() throws Exception {
    PublicRecipeResponse dto =
        new PublicRecipeResponse(
            "Morning Cup",
            "Ethiopia",
            "V60",
            new BigDecimal("15.0"),
            new BigDecimal("250.0"),
            "1:16",
            "Medium",
            94,
            "3:00",
            "Bloom then pour",
            "Floral");
    when(recipeService.getByShareToken("tok-1")).thenReturn(dto);

    mockMvc
        .perform(get("/api/public/recipes/tok-1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Morning Cup"))
        .andExpect(jsonPath("$.coffeeName").value("Ethiopia"))
        .andExpect(jsonPath("$.methodName").value("V60"))
        .andExpect(jsonPath("$.id").doesNotExist())
        .andExpect(jsonPath("$.favorite").doesNotExist())
        .andExpect(jsonPath("$.createdAt").doesNotExist());
  }

  @Test
  void getByToken_returns404WhenTokenUnknown() throws Exception {
    when(recipeService.getByShareToken("nope"))
        .thenThrow(new EntityNotFoundException("Recipe not found"));

    mockMvc.perform(get("/api/public/recipes/nope")).andExpect(status().isNotFound());
  }
}
```

> If `@WebMvcTest` in this project needs `GlobalExceptionHandler` imported (i.e. it lives outside the scanned controllers), mirror exactly how `RecipeControllerTest` wires the handler (usually `@Import(GlobalExceptionHandler.class)` or it is picked up automatically). Copy that class's pattern verbatim.

- [ ] **Step 16: Run the controller tests to verify they fail, then pass**

Run: `./mvnw -Dtest=RecipeControllerTest,PublicRecipeControllerTest test`
Expected: they compile against the Step 13/14 code and PASS. (If run before Steps 13–14, they FAIL to compile — that is the red state.)

- [ ] **Step 17: Add the integration flow test**

In an existing integration test class under `.../integration/` (follow the file's Testcontainers + `TestRestTemplate`/`MockMvc` setup — control the dataset; do not assume a single row), add a test that: creates a recipe via `POST /api/recipes`; `PATCH /api/recipes/{id}/share` → 200, capture `shareToken`; `GET /api/public/recipes/{token}` → 200 with `name` matching; `PATCH /api/recipes/{id}/unshare` → 200; `GET /api/public/recipes/{token}` → 404.

```java
  @Test
  void shareFlow_createShareFetchUnshareRefetch() throws Exception {
    // create a coffee + method + recipe using this class's existing helpers,
    // capture the created recipe id as `recipeId`.
    // (Reuse the same helper calls the other integration tests here use.)

    String token =
        JsonPath.read(
            mockMvc
                .perform(patch("/api/recipes/" + recipeId + "/share"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            "$.shareToken");

    mockMvc
        .perform(get("/api/public/recipes/" + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").exists());

    mockMvc.perform(patch("/api/recipes/" + recipeId + "/unshare")).andExpect(status().isOk());

    mockMvc.perform(get("/api/public/recipes/" + token)).andExpect(status().isNotFound());
  }
```

> Use whatever HTTP mechanism the chosen integration class already uses (`MockMvc` shown). If it uses `TestRestTemplate`, translate the calls accordingly and read `shareToken` off the response body object.

- [ ] **Step 18: Format and run the full backend build**

Run: `./mvnw spotless:apply` then `./mvnw clean verify`
Expected: BUILD SUCCESS — all tests green, Flyway V4 applies cleanly, JaCoCo/Sonar gates pass.

- [ ] **Step 19: Commit**

```bash
git add brewdeck-api
git commit -m "feat(api): add recipe public share links (token, share/unshare, public endpoint)"
```

---

### Task 2: Frontend API layer, types, and hooks

**Files:**
- Modify: `brewdeck-web/src/lib/api/types.ts`
- Modify: `brewdeck-web/src/lib/api/recipes.ts`
- Create: `brewdeck-web/src/lib/api/publicRecipes.ts`
- Modify: `brewdeck-web/src/lib/query/keys.ts`
- Create: `brewdeck-web/src/hooks/useShareRecipe.ts`
- Create: `brewdeck-web/src/hooks/usePublicRecipe.ts`
- Test: `brewdeck-web/src/lib/api/publicRecipes.test.ts` (create)
- Test: `brewdeck-web/src/hooks/useShareRecipe.test.tsx` (create)

**Interfaces:**
- Consumes: `apiFetch<T>` from `@/lib/api/client`, `keys.recipes.detail(id)` from `@/lib/query/keys`, `Recipe` type from `@/lib/api/types`.
- Produces (Tasks 3 & 4 rely on these exact signatures):
  - `Recipe.shareToken: string | null`
  - `PublicRecipe` type (card fields only, see Global Constraints)
  - `shareRecipe(id: number): Promise<Recipe>`, `unshareRecipe(id: number): Promise<Recipe>`
  - `getPublicRecipe(token: string): Promise<PublicRecipe>`
  - `keys.recipes.public(token: string)`
  - `useShareRecipe(id: number)`, `useUnshareRecipe(id: number)` (mutations; `onSuccess` invalidates `keys.recipes.detail(id)`)
  - `usePublicRecipe(token: string)` (query; `retry: false`)

- [ ] **Step 1: Add the types**

In `types.ts`, add `shareToken` to `Recipe` (as the final field) and add `PublicRecipe`:

```ts
export type Recipe = {
  id: number;
  coffeeId: number;
  coffeeName: string;
  methodId: number;
  methodName: string;
  name: string;
  coffeeGrams: number | null;
  waterGrams: number | null;
  ratio: string | null;
  grindSetting: string | null;
  waterTemp: number | null;
  brewTime: string | null;
  steps: string | null;
  expectedTaste: string | null;
  favorite: boolean;
  createdAt: string;
  updatedAt: string | null;
  shareToken: string | null;
};

export type PublicRecipe = {
  name: string;
  coffeeName: string;
  methodName: string;
  coffeeGrams: number | null;
  waterGrams: number | null;
  ratio: string | null;
  grindSetting: string | null;
  waterTemp: number | null;
  brewTime: string | null;
  steps: string | null;
  expectedTaste: string | null;
};
```

- [ ] **Step 2: Add share/unshare to the recipes API module**

In `recipes.ts`, append:

```ts
export function shareRecipe(id: number): Promise<Recipe> {
  return apiFetch<Recipe>(`/api/recipes/${id}/share`, { method: 'PATCH' });
}

export function unshareRecipe(id: number): Promise<Recipe> {
  return apiFetch<Recipe>(`/api/recipes/${id}/unshare`, { method: 'PATCH' });
}
```

- [ ] **Step 3: Write the failing publicRecipes api test**

Create `publicRecipes.test.ts`:

```ts
import { afterEach, describe, expect, it, vi } from 'vitest';
import { getPublicRecipe } from './publicRecipes';
import * as client from './client';

describe('getPublicRecipe', () => {
  afterEach(() => vi.restoreAllMocks());

  it('fetches the public recipe endpoint with the encoded token', async () => {
    const body = { name: 'Morning Cup', coffeeName: 'Ethiopia', methodName: 'V60' };
    const spy = vi.spyOn(client, 'apiFetch').mockResolvedValue(body as never);

    const result = await getPublicRecipe('tok 1');

    expect(spy).toHaveBeenCalledWith('/api/public/recipes/tok%201');
    expect(result).toEqual(body);
  });
});
```

- [ ] **Step 4: Run it to verify it fails**

Run (from `brewdeck-web/`): `npm run test -- src/lib/api/publicRecipes.test.ts`
Expected: FAIL — `./publicRecipes` module not found.

- [ ] **Step 5: Create the publicRecipes api module**

Create `publicRecipes.ts`:

```ts
import { apiFetch } from './client';
import type { PublicRecipe } from './types';

export function getPublicRecipe(token: string): Promise<PublicRecipe> {
  return apiFetch<PublicRecipe>(`/api/public/recipes/${encodeURIComponent(token)}`);
}
```

- [ ] **Step 6: Run it to verify it passes**

Run: `npm run test -- src/lib/api/publicRecipes.test.ts`
Expected: PASS.

- [ ] **Step 7: Add the query key**

In `keys.ts`, add to the `recipes` object:

```ts
    public: (token: string) => ['recipes', 'public', token] as const,
```

- [ ] **Step 8: Write the failing hook test**

Create `useShareRecipe.test.tsx`:

```tsx
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { act, renderHook, waitFor } from '@testing-library/react';
import type { ReactNode } from 'react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { useShareRecipe, useUnshareRecipe } from './useShareRecipe';
import * as recipesApi from '@/lib/api/recipes';
import { keys } from '@/lib/query/keys';

function wrapper(client: QueryClient) {
  return function Wrapper({ children }: { children: ReactNode }) {
    return <QueryClientProvider client={client}>{children}</QueryClientProvider>;
  };
}

describe('useShareRecipe / useUnshareRecipe', () => {
  afterEach(() => vi.restoreAllMocks());

  it('shares and invalidates the recipe detail query', async () => {
    const client = new QueryClient();
    const invalidate = vi.spyOn(client, 'invalidateQueries');
    vi.spyOn(recipesApi, 'shareRecipe').mockResolvedValue({ id: 1, shareToken: 'tok' } as never);

    const { result } = renderHook(() => useShareRecipe(1), { wrapper: wrapper(client) });
    await act(async () => {
      await result.current.mutateAsync();
    });

    await waitFor(() =>
      expect(invalidate).toHaveBeenCalledWith({ queryKey: keys.recipes.detail(1) }),
    );
  });

  it('unshares and invalidates the recipe detail query', async () => {
    const client = new QueryClient();
    const invalidate = vi.spyOn(client, 'invalidateQueries');
    vi.spyOn(recipesApi, 'unshareRecipe').mockResolvedValue({ id: 1, shareToken: null } as never);

    const { result } = renderHook(() => useUnshareRecipe(1), { wrapper: wrapper(client) });
    await act(async () => {
      await result.current.mutateAsync();
    });

    await waitFor(() =>
      expect(invalidate).toHaveBeenCalledWith({ queryKey: keys.recipes.detail(1) }),
    );
  });
});
```

- [ ] **Step 9: Run it to verify it fails**

Run: `npm run test -- src/hooks/useShareRecipe.test.tsx`
Expected: FAIL — `./useShareRecipe` module not found.

- [ ] **Step 10: Create the hooks**

Create `useShareRecipe.ts`:

```ts
'use client';

import { useMutation, useQueryClient } from '@tanstack/react-query';
import { shareRecipe, unshareRecipe } from '@/lib/api/recipes';
import { keys } from '@/lib/query/keys';

export function useShareRecipe(id: number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: () => shareRecipe(id),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: keys.recipes.detail(id) }),
  });
}

export function useUnshareRecipe(id: number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: () => unshareRecipe(id),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: keys.recipes.detail(id) }),
  });
}
```

Create `usePublicRecipe.ts`:

```ts
'use client';

import { useQuery } from '@tanstack/react-query';
import { getPublicRecipe } from '@/lib/api/publicRecipes';
import { keys } from '@/lib/query/keys';

export function usePublicRecipe(token: string) {
  return useQuery({
    queryKey: keys.recipes.public(token),
    queryFn: () => getPublicRecipe(token),
    retry: false,
  });
}
```

- [ ] **Step 11: Run the hook test to verify it passes**

Run: `npm run test -- src/hooks/useShareRecipe.test.tsx`
Expected: PASS.

- [ ] **Step 12: Type-check and lint the changed files**

Run: `npm run type-check`
Run: `npm run lint:fix -- src/lib/api/types.ts src/lib/api/recipes.ts src/lib/api/publicRecipes.ts src/lib/api/publicRecipes.test.ts src/lib/query/keys.ts src/hooks/useShareRecipe.ts src/hooks/useShareRecipe.test.tsx src/hooks/usePublicRecipe.ts`
Expected: no type errors; no lint errors.

- [ ] **Step 13: Commit**

```bash
git add brewdeck-web/src/lib brewdeck-web/src/hooks
git commit -m "feat(web): add recipe share API client, types, and TanStack Query hooks"
```

---

### Task 3: Frontend share UI — dialog + Share button

**Files:**
- Create: `brewdeck-web/src/components/recipes/ShareRecipeDialog.tsx`
- Create: `brewdeck-web/src/components/recipes/ShareRecipeDialog.test.tsx`
- Modify: `brewdeck-web/src/components/recipes/RecipeDetailView.tsx`
- Modify: `brewdeck-web/src/components/recipes/RecipeDetailView.test.tsx`

**Interfaces:**
- Consumes: `useShareRecipe(id)`, `useUnshareRecipe(id)` from `@/hooks/useShareRecipe`; `Recipe` from `@/lib/api/types`.
- Produces: `ShareRecipeDialog({ open, recipe, onClose })` — a named export; Share button on `RecipeDetailView` that opens it.

- [ ] **Step 1: Write the failing dialog test**

Create `ShareRecipeDialog.test.tsx`:

```tsx
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import type { ReactNode } from 'react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { ShareRecipeDialog } from './ShareRecipeDialog';
import type { Recipe } from '@/lib/api/types';
import * as recipesApi from '@/lib/api/recipes';

function baseRecipe(overrides: Partial<Recipe> = {}): Recipe {
  return {
    id: 1,
    coffeeId: 1,
    coffeeName: 'Ethiopia',
    methodId: 1,
    methodName: 'V60',
    name: 'Morning Cup',
    coffeeGrams: 15,
    waterGrams: 250,
    ratio: '1:16',
    grindSetting: 'Medium',
    waterTemp: 94,
    brewTime: '3:00',
    steps: 'Bloom then pour',
    expectedTaste: 'Floral',
    favorite: false,
    createdAt: '2026-07-01T00:00:00Z',
    updatedAt: null,
    shareToken: null,
    ...overrides,
  };
}

function renderDialog(recipe: Recipe) {
  const client = new QueryClient();
  return render(
    <QueryClientProvider client={client}>
      <ShareRecipeDialog open recipe={recipe} onClose={() => {}} />
    </QueryClientProvider>,
  );
}

describe('ShareRecipeDialog', () => {
  beforeEach(() => {
    Object.assign(navigator, { clipboard: { writeText: vi.fn().mockResolvedValue(undefined) } });
  });
  afterEach(() => vi.restoreAllMocks());

  it('renders the not-shared state with a Create link button', () => {
    renderDialog(baseRecipe({ shareToken: null }));
    expect(screen.getByRole('button', { name: /create link/i })).toBeInTheDocument();
  });

  it('calls shareRecipe when Create link is clicked', async () => {
    const spy = vi
      .spyOn(recipesApi, 'shareRecipe')
      .mockResolvedValue(baseRecipe({ shareToken: 'tok-1' }));
    renderDialog(baseRecipe({ shareToken: null }));

    await userEvent.click(screen.getByRole('button', { name: /create link/i }));

    await waitFor(() => expect(spy).toHaveBeenCalledWith(1));
  });

  it('renders the shared state with the link, Copy, and Stop sharing', () => {
    renderDialog(baseRecipe({ shareToken: 'tok-1' }));
    expect(screen.getByRole('button', { name: /copy/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /stop sharing/i })).toBeInTheDocument();
    expect(screen.getByDisplayValue(/\/share\/tok-1$/)).toBeInTheDocument();
  });

  it('calls unshareRecipe when Stop sharing is clicked', async () => {
    const spy = vi
      .spyOn(recipesApi, 'unshareRecipe')
      .mockResolvedValue(baseRecipe({ shareToken: null }));
    renderDialog(baseRecipe({ shareToken: 'tok-1' }));

    await userEvent.click(screen.getByRole('button', { name: /stop sharing/i }));

    await waitFor(() => expect(spy).toHaveBeenCalledWith(1));
  });

  it('copies the link to the clipboard when Copy is clicked', async () => {
    renderDialog(baseRecipe({ shareToken: 'tok-1' }));

    await userEvent.click(screen.getByRole('button', { name: /copy/i }));

    await waitFor(() =>
      expect(navigator.clipboard.writeText).toHaveBeenCalledWith(
        expect.stringMatching(/\/share\/tok-1$/),
      ),
    );
  });
});
```

- [ ] **Step 2: Run it to verify it fails**

Run: `npm run test -- src/components/recipes/ShareRecipeDialog.test.tsx`
Expected: FAIL — `./ShareRecipeDialog` module not found.

- [ ] **Step 3: Create the dialog**

Create `ShareRecipeDialog.tsx`:

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
import Stack from '@mui/material/Stack';
import TextField from '@mui/material/TextField';
import { useState } from 'react';
import type { Recipe } from '@/lib/api/types';
import { useShareRecipe, useUnshareRecipe } from '@/hooks/useShareRecipe';

export function ShareRecipeDialog({
  open,
  recipe,
  onClose,
}: {
  open: boolean;
  recipe: Recipe;
  onClose: () => void;
}) {
  const share = useShareRecipe(recipe.id);
  const unshare = useUnshareRecipe(recipe.id);
  const [copyError, setCopyError] = useState(false);

  const origin = typeof window === 'undefined' ? '' : window.location.origin;
  const link = recipe.shareToken ? `${origin}/share/${recipe.shareToken}` : '';

  const onCopy = async () => {
    setCopyError(false);
    try {
      await navigator.clipboard.writeText(link);
    } catch {
      setCopyError(true);
    }
  };

  return (
    <Dialog open={open} onClose={onClose} fullWidth maxWidth="sm">
      <DialogTitle>Share recipe</DialogTitle>
      <DialogContent>
        {recipe.shareToken ? (
          <Stack spacing={2} sx={{ mt: 1 }}>
            <DialogContentText>
              Anyone with this link can view a read-only copy of this recipe.
            </DialogContentText>
            <TextField
              label="Public link"
              value={link}
              slotProps={{ input: { readOnly: true } }}
              fullWidth
            />
            {copyError ? (
              <Alert severity="error">Couldn&apos;t copy — copy it manually.</Alert>
            ) : null}
          </Stack>
        ) : (
          <DialogContentText sx={{ mt: 1 }}>
            Create a public link to let anyone view this recipe. You can stop sharing at any time.
          </DialogContentText>
        )}
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>Close</Button>
        {recipe.shareToken ? (
          <>
            <Button onClick={onCopy}>Copy</Button>
            <Button
              color="error"
              onClick={() => unshare.mutate()}
              disabled={unshare.isPending}
              startIcon={unshare.isPending ? <CircularProgress size={16} /> : undefined}
            >
              Stop sharing
            </Button>
          </>
        ) : (
          <Button
            variant="contained"
            onClick={() => share.mutate()}
            disabled={share.isPending}
            startIcon={share.isPending ? <CircularProgress size={16} /> : undefined}
          >
            Create link
          </Button>
        )}
      </DialogActions>
    </Dialog>
  );
}
```

> `slotProps={{ input: { readOnly: true } }}` is the MUI v6 API. If this project's MUI version rejects it, use `InputProps={{ readOnly: true }}` — match whatever `RecipeFormDialog.tsx` uses for read-only/adornment fields.

- [ ] **Step 4: Run the dialog test to verify it passes**

Run: `npm run test -- src/components/recipes/ShareRecipeDialog.test.tsx`
Expected: PASS.

- [ ] **Step 5: Add the Share button to RecipeDetailView**

In `RecipeDetailView.tsx`:

Add the import beside the other component imports:

```tsx
import { ShareRecipeDialog } from './ShareRecipeDialog';
```

Add dialog open state beside the other `useState` calls:

```tsx
  const [shareOpen, setShareOpen] = useState(false);
```

Add the Share button inside the action-button `Box` (after the Export PDF button):

```tsx
        <Button variant="outlined" size="small" onClick={() => setShareOpen(true)}>
          Share
        </Button>
```

Render the dialog beside the existing `improved` dialog block (before the closing `</>`):

```tsx
      <ShareRecipeDialog open={shareOpen} recipe={recipe} onClose={() => setShareOpen(false)} />
```

- [ ] **Step 6: Update the RecipeDetailView test**

In `RecipeDetailView.test.tsx`, add a test that the Share button renders and opens the dialog (the file already mounts the component with mocked hooks; the recipe fixture must include `shareToken: null` — add it if the fixture predates this field):

```tsx
  it('opens the share dialog when Share is clicked', async () => {
    renderDetail(); // use the file's existing render helper
    await userEvent.click(await screen.findByRole('button', { name: /^share$/i }));
    expect(await screen.findByRole('dialog', { name: /share recipe/i })).toBeInTheDocument();
  });
```

> If the file's recipe fixture is a shared object, add `shareToken: null` to it so `Recipe` stays type-correct. If `userEvent` isn't imported in this file, add `import userEvent from '@testing-library/user-event';`.

- [ ] **Step 7: Run the FULL vitest suite**

Run: `npm run test`
Expected: entire suite green. The Share button is now on the shared `RecipeDetailView`; per project memory, a change to that component can break sibling tests — the full run is mandatory here, not the single file.

- [ ] **Step 8: Type-check and lint the changed files**

Run: `npm run type-check`
Run: `npm run lint:fix -- src/components/recipes/ShareRecipeDialog.tsx src/components/recipes/ShareRecipeDialog.test.tsx src/components/recipes/RecipeDetailView.tsx src/components/recipes/RecipeDetailView.test.tsx`
Expected: no type errors; no lint errors.

- [ ] **Step 9: Commit**

```bash
git add brewdeck-web/src/components/recipes
git commit -m "feat(web): add share dialog and Share button to recipe detail"
```

---

### Task 4: Frontend public page — /share/[token] + PublicRecipeView

**Files:**
- Create: `brewdeck-web/src/app/share/[token]/page.tsx`
- Create: `brewdeck-web/src/components/recipes/PublicRecipeView.tsx`
- Create: `brewdeck-web/src/components/recipes/PublicRecipeView.test.tsx`

**Interfaces:**
- Consumes: `usePublicRecipe(token)` from `@/hooks/usePublicRecipe`; `Spinner` from `@/components/ui/Spinner`, `EmptyState` from `@/components/ui/EmptyState`.
- Produces: `PublicRecipeView({ token })` named export; the page default export reading `params.token`.

- [ ] **Step 1: Write the failing PublicRecipeView test**

Create `PublicRecipeView.test.tsx`:

```tsx
import { render, screen } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { PublicRecipeView } from './PublicRecipeView';
import * as hook from '@/hooks/usePublicRecipe';
import type { PublicRecipe } from '@/lib/api/types';

function mockHook(value: Partial<ReturnType<typeof hook.usePublicRecipe>>) {
  vi.spyOn(hook, 'usePublicRecipe').mockReturnValue(
    value as ReturnType<typeof hook.usePublicRecipe>,
  );
}

const sample: PublicRecipe = {
  name: 'Morning Cup',
  coffeeName: 'Ethiopia',
  methodName: 'V60',
  coffeeGrams: 15,
  waterGrams: 250,
  ratio: '1:16',
  grindSetting: 'Medium',
  waterTemp: 94,
  brewTime: '3:00',
  steps: 'Bloom then pour',
  expectedTaste: 'Floral',
};

describe('PublicRecipeView', () => {
  afterEach(() => vi.restoreAllMocks());

  it('shows a spinner while loading', () => {
    mockHook({ isLoading: true, data: undefined, isError: false });
    render(<PublicRecipeView token="tok-1" />);
    expect(screen.getByRole('progressbar')).toBeInTheDocument();
  });

  it('shows the unavailable empty state on error (404)', () => {
    mockHook({ isLoading: false, data: undefined, isError: true });
    render(<PublicRecipeView token="tok-1" />);
    expect(screen.getByText(/isn't available/i)).toBeInTheDocument();
  });

  it('renders the recipe card on success', () => {
    mockHook({ isLoading: false, data: sample, isError: false });
    render(<PublicRecipeView token="tok-1" />);
    expect(screen.getByRole('heading', { name: /morning cup/i })).toBeInTheDocument();
    expect(screen.getByText('Ethiopia')).toBeInTheDocument();
    expect(screen.getByText('V60')).toBeInTheDocument();
  });
});
```

- [ ] **Step 2: Run it to verify it fails**

Run: `npm run test -- src/components/recipes/PublicRecipeView.test.tsx`
Expected: FAIL — `./PublicRecipeView` module not found.

- [ ] **Step 3: Create PublicRecipeView**

Create `PublicRecipeView.tsx`:

```tsx
'use client';

import Box from '@mui/material/Box';
import Grid from '@mui/material/Grid';
import Paper from '@mui/material/Paper';
import Typography from '@mui/material/Typography';
import type { ReactNode } from 'react';
import { usePublicRecipe } from '@/hooks/usePublicRecipe';
import { Spinner } from '@/components/ui/Spinner';
import { EmptyState } from '@/components/ui/EmptyState';

function orDash(value: string | number | null): string {
  return value === null || value === '' ? '—' : String(value);
}

export function PublicRecipeView({ token }: { token: string }) {
  const query = usePublicRecipe(token);

  if (query.isLoading) {
    return <Spinner />;
  }

  if (query.isError || !query.data) {
    return <EmptyState message="This recipe isn't available." />;
  }

  const recipe = query.data;
  const details: Array<{ label: string; value: string }> = [
    { label: 'Coffee', value: recipe.coffeeName },
    { label: 'Method', value: recipe.methodName },
    { label: 'Coffee (g)', value: orDash(recipe.coffeeGrams) },
    { label: 'Water (g)', value: orDash(recipe.waterGrams) },
    { label: 'Ratio', value: orDash(recipe.ratio) },
    { label: 'Grind', value: orDash(recipe.grindSetting) },
    { label: 'Water Temp', value: orDash(recipe.waterTemp) },
    { label: 'Brew Time', value: orDash(recipe.brewTime) },
  ];

  let card: ReactNode = null;
  card = (
    <Paper sx={{ p: 3, maxWidth: 720, mx: 'auto', mt: 4 }}>
      <Typography variant="overline" color="text.secondary">
        BrewDeck · Shared recipe
      </Typography>
      <Typography variant="h5" component="h1" gutterBottom>
        {recipe.name}
      </Typography>

      <Grid container spacing={2} sx={{ mb: 3 }}>
        {details.map((item) => (
          <Grid key={item.label} size={{ xs: 6, sm: 4, md: 3 }}>
            <Typography variant="caption" color="text.secondary">
              {item.label}
            </Typography>
            <Typography variant="body1">{item.value}</Typography>
          </Grid>
        ))}
      </Grid>

      {recipe.steps ? (
        <Box sx={{ mb: 3 }}>
          <Typography variant="subtitle1" gutterBottom>
            Steps
          </Typography>
          <Typography variant="body2" color="text.secondary" sx={{ whiteSpace: 'pre-wrap' }}>
            {recipe.steps}
          </Typography>
        </Box>
      ) : null}

      {recipe.expectedTaste ? (
        <Box>
          <Typography variant="subtitle1" gutterBottom>
            Expected taste
          </Typography>
          <Typography variant="body2" color="text.secondary">
            {recipe.expectedTaste}
          </Typography>
        </Box>
      ) : null}
    </Paper>
  );

  return card;
}
```

> `orDash` is duplicated from the PDF helper deliberately — the public view must not import app-internal modules that pull in the recipe detail graph. Keep this local copy; it is a 1-line function, not a logic block worth sharing across the public/app boundary.

- [ ] **Step 4: Run the test to verify it passes**

Run: `npm run test -- src/components/recipes/PublicRecipeView.test.tsx`
Expected: PASS.

- [ ] **Step 5: Create the route page**

Create `src/app/share/[token]/page.tsx`:

```tsx
import { PublicRecipeView } from '@/components/recipes/PublicRecipeView';

export default async function SharedRecipePage({
  params,
}: {
  params: Promise<{ token: string }>;
}) {
  const { token } = await params;
  return <PublicRecipeView token={token} />;
}
```

> Next.js 15 route `params` is a Promise — `await` it. If this project pins an earlier Next.js where `params` is a plain object, drop the `Promise<>`/`await` and read `params.token` directly. Match the signature of an existing dynamic page such as `src/app/recipes/[id]/page.tsx`.

- [ ] **Step 6: Type-check, lint, and build**

Run: `npm run type-check`
Run: `npm run lint:fix -- src/app/share/[token]/page.tsx src/components/recipes/PublicRecipeView.tsx src/components/recipes/PublicRecipeView.test.tsx`
Run: `npm run build`
Expected: no type errors, no lint errors, production build succeeds (the new route compiles).

- [ ] **Step 7: Commit**

```bash
git add brewdeck-web/src/app/share brewdeck-web/src/components/recipes/PublicRecipeView.tsx brewdeck-web/src/components/recipes/PublicRecipeView.test.tsx
git commit -m "feat(web): add public /share/[token] page rendering a read-only recipe card"
```

---

### Task 5: Docs — roadmap, project-state, Postman

**Files:**
- Modify: `.claude/roadmap.md`
- Modify: `.claude/project-state.md`
- Modify: `docs/postman/brewdeck.postman_collection.json`

**Interfaces:**
- Consumes: the endpoints delivered in Task 1 (`PATCH /api/recipes/{id}/share`, `/unshare`, `GET /api/public/recipes/{token}`).
- Produces: documentation only — no code.

- [ ] **Step 1: Mark the roadmap item Done**

In `.claude/roadmap.md`, change the Phase 5 line:

```
- Public share links — Not Started
```

to:

```
- Public share links (opt-in revocable token, public /share/[token] page) — Done
```

If every Phase 5 item is now Done, update the phase status line from `Status: In Progress` to `Status: Completed`.

- [ ] **Step 2: Update project-state**

In `.claude/project-state.md`, add the shipped feature to the completed/recent-work section in the same style as the surrounding entries (backend: share token + `PATCH /api/recipes/{id}/share|unshare`, `GET /api/public/recipes/{token}`, curated `PublicRecipeResponse`; frontend: `ShareRecipeDialog`, Share button, public `/share/[token]` page). Follow the file's existing wording conventions — read it first and mirror the closest existing entry (e.g. the PDF export or AI-improve entries).

- [ ] **Step 3: Add the Postman requests**

In `docs/postman/brewdeck.postman_collection.json`, under the Recipes folder, add three requests mirroring the existing favorite/unfavorite request shapes (base URL from the environment `{{baseUrl}}`; use the `{{recipeId}}` Long var; no real tokens):

- `PATCH {{baseUrl}}/api/recipes/{{recipeId}}/share` — "Share recipe"
- `PATCH {{baseUrl}}/api/recipes/{{recipeId}}/unshare` — "Unshare recipe"
- `GET {{baseUrl}}/api/public/recipes/{{shareToken}}` — "Get public recipe by token" (add a `shareToken` collection/environment variable placeholder, empty by default)

Copy the exact request JSON structure (headers, `method`, `url.raw` + parsed `host`/`path`) from the existing favorite request so the collection stays schema-valid. Validate the JSON parses (e.g. open it or run `python -m json.tool` on it).

- [ ] **Step 4: Commit**

```bash
git add .claude/roadmap.md .claude/project-state.md docs/postman/brewdeck.postman_collection.json
git commit -m "docs: mark public share links done; add share endpoints to Postman"
```

---

## Notes for the executor

- Task 1 is the only backend task and must land first — Tasks 2–4 depend on its endpoints and DTO shapes. Tasks 2→3→4 are ordered by dependency (hooks → dialog → page). Task 5 is docs-only and last.
- The token is 16 base64url-no-pad bytes (~22 chars) but the column is `VARCHAR(32)` for headroom — do not tighten it.
- Never assert on `id`/`favorite`/`createdAt` presence in the public response except to assert their **absence** (curated-exposure guarantee).
