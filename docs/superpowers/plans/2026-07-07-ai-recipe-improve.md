# AI Recipe Improve-from-History Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a feature-toggled `POST /api/recipes/{id}/improve` endpoint that asks Claude to tune an existing recipe from its recent rated brew sessions, plus an "Improve with AI" button on the recipe detail page that pre-fills the edit dialog with the suggestion.

**Architecture:** Extend the generate slice's hexagonal port `RecipeSuggestionPort` with an `improve(ImprovementContext)` method (Approach A) — reuse the existing `ClaudeRecipeSuggestionAdapter`, `SuggestedRecipePayload` structured output, feature toggle, and 503 fail-soft path. A new `RecipeImprovementService` loads the recipe plus its top-10 rated sessions, guards an empty-history precondition (422), and maps the port result to the existing `SuggestedRecipeResponse`. The frontend adds an ephemeral pre-fill flow that opens the existing `RecipeFormDialog`.

**Tech Stack:** Java 21, Spring Boot 3, Spring Data JPA, `com.anthropic:anthropic-java`, JUnit 5 + Mockito + MockMvc, Testcontainers (`@DataJpaTest` + `PostgresRepositoryTest`); Next.js 15 / React 19 / TypeScript, MUI, TanStack Query, Vitest + React Testing Library.

## Global Constraints

- `ANTHROPIC_API_KEY` comes from the environment only — never in source, logs, or committed config.
- Model `claude-haiku-4-5` lives in `application.yaml` only; never hardcoded in Java. No `effort`/`thinking`/`budget_tokens` (Haiku rejects them).
- Feature toggle `brewdeck.ai.enabled` (default false); disabled → 503 via `AiUnavailableException`.
- The adapter fails soft: any `RuntimeException` is logged server-side with cause and wrapped in `AiUnavailableException`; no SDK stack trace ever reaches the client.
- Structured outputs via the typed `SuggestedRecipePayload` POJO — no free-text parsing.
- The endpoint returns the `SuggestedRecipeResponse` DTO directly, not a `PageResponse` (single result, not a browsable collection).
- `ClaudeRecipeSuggestionAdapter.java` stays excluded from Sonar coverage (already listed in `pom.xml` `sonar.coverage.exclusions`).
- Validation/messages avoid special symbols — write "degrees Celsius", never the degree symbol.
- CI must stay green with **no API key**: the test profile keeps `brewdeck.ai.enabled=false`, service/controller tests mock the port, and no test performs a live SDK call.
- Conventional Commits; scopes `api` / `web` / `docs`.
- Backend build runs as `sh mvnw ...` from `brewdeck-api/` (NOT `./mvnw`). Frontend commands run from `brewdeck-web/`.

---

### Task 1: Backend — improve endpoint end-to-end

Extends the domain port and both adapters, adds the context types, the 422 exception + handler, the repository query, and the new service + controller. Deliverable: `POST /api/recipes/{id}/improve` works behind the toggle with full unit/repository/controller coverage; `sh mvnw clean verify` green.

**Files:**
- Create: `brewdeck-api/src/main/java/com/brewdeck/brewdeck_api/ai/BrewHistoryEntry.java`
- Create: `brewdeck-api/src/main/java/com/brewdeck/brewdeck_api/ai/ImprovementContext.java`
- Create: `brewdeck-api/src/main/java/com/brewdeck/brewdeck_api/ai/InsufficientBrewHistoryException.java`
- Create: `brewdeck-api/src/main/java/com/brewdeck/brewdeck_api/ai/RecipeImprovementService.java`
- Create: `brewdeck-api/src/main/java/com/brewdeck/brewdeck_api/ai/RecipeImprovementController.java`
- Modify: `brewdeck-api/src/main/java/com/brewdeck/brewdeck_api/ai/RecipeSuggestionPort.java`
- Modify: `brewdeck-api/src/main/java/com/brewdeck/brewdeck_api/ai/ClaudeRecipeSuggestionAdapter.java`
- Modify: `brewdeck-api/src/main/java/com/brewdeck/brewdeck_api/ai/DisabledRecipeSuggestionAdapter.java`
- Modify: `brewdeck-api/src/main/java/com/brewdeck/brewdeck_api/session/BrewSessionRepository.java`
- Modify: `brewdeck-api/src/main/java/com/brewdeck/brewdeck_api/common/error/GlobalExceptionHandler.java`
- Create test: `brewdeck-api/src/test/java/com/brewdeck/brewdeck_api/ai/RecipeImprovementServiceTest.java`
- Create test: `brewdeck-api/src/test/java/com/brewdeck/brewdeck_api/ai/RecipeImprovementControllerTest.java`
- Modify test: `brewdeck-api/src/test/java/com/brewdeck/brewdeck_api/session/BrewSessionRepositoryTest.java`

**Interfaces:**
- Consumes (existing, unchanged): `SuggestedRecipe(BigDecimal coffeeGrams, BigDecimal waterGrams, String ratio, String grindSetting, Integer waterTemp, String brewTime, String steps, String rationale)`; `SuggestedRecipeResponse(...)` — same 8 fields; `AiProperties(boolean enabled, String model, int timeoutSeconds, int maxTokens)`; `AiUnavailableException`; `ClaudeRecipeSuggestionAdapter.toSuggestedRecipe(SuggestedRecipePayload)` (static, package-private); `ClaudeRecipeSuggestionAdapter.SuggestedRecipePayload`; `RecipeRepository.findById(Long)` (fetches `coffee` + `method` via `@EntityGraph`); `Recipe`, `Coffee`, `BrewMethod`, `BrewSession` entities.
- Produces (later tasks rely on):
  - `RecipeSuggestionPort.improve(ImprovementContext) : SuggestedRecipe`
  - `BrewHistoryEntry(Integer rating, String actualGrind, Integer actualTemp, String actualTime, String tasteResult, String adjustmentNotes)`
  - `ImprovementContext(String coffeeName, String origin, String roastLevel, String process, Integer acidityScore, Integer bodyScore, Integer sweetnessScore, Integer bitternessScore, String methodName, String methodDescription, BigDecimal currentCoffeeGrams, BigDecimal currentWaterGrams, String currentRatio, String currentGrindSetting, Integer currentWaterTemp, String currentBrewTime, String currentSteps, List<BrewHistoryEntry> history)`
  - `RecipeImprovementService.improve(Long recipeId) : SuggestedRecipeResponse`
  - `POST /api/recipes/{id}/improve` → `SuggestedRecipeResponse` (200); 404 / 422 / 503
  - `BrewSessionRepository.findTop10ByRecipeIdAndRatingIsNotNullOrderByBrewedAtDesc(Long) : List<BrewSession>`

- [ ] **Step 1: Add the repository query**

In `BrewSessionRepository.java`, add this method (inside the interface, after `findByRecipeIdOrderByBrewedAtDesc(Long recipeId)`):

```java
  List<BrewSession> findTop10ByRecipeIdAndRatingIsNotNullOrderByBrewedAtDesc(Long recipeId);
```

`java.util.List` is already imported in this file.

- [ ] **Step 2: Write the failing repository test**

In `BrewSessionRepositoryTest.java`, add `import java.util.List;` near the other imports, then add this test method inside the class (before the private `persistRecipe` helper):

```java
  @Test
  void findTop10ByRecipeIdAndRatingIsNotNull_shouldReturnOnlyRatedNewestFirstCappedAt10() {
    Recipe recipe = persistRecipe("Mezcla Veracruz AeroPress");

    // Unrated session (rating null) must be excluded.
    entityManager.persist(
        BrewSession.builder()
            .recipe(recipe)
            .brewedAt(LocalDateTime.of(2026, 5, 1, 10, 0))
            .actualTemp(90)
            .build());

    // 11 rated sessions with ascending brewedAt: only the newest 10 come back, newest first.
    for (int i = 1; i <= 11; i++) {
      entityManager.persist(
          BrewSession.builder()
              .recipe(recipe)
              .brewedAt(LocalDateTime.of(2026, 4, i, 10, 0))
              .actualTemp(88 + i)
              .actualTime("2:30")
              .tasteResult("taste " + i)
              .rating((i % 10) + 1)
              .build());
    }

    entityManager.flush();
    entityManager.clear();

    List<BrewSession> result =
        brewSessionRepository.findTop10ByRecipeIdAndRatingIsNotNullOrderByBrewedAtDesc(
            recipe.getId());

    assertThat(result).hasSize(10);
    assertThat(result).allSatisfy(session -> assertThat(session.getRating()).isNotNull());
    assertThat(result.get(0).getBrewedAt()).isEqualTo(LocalDateTime.of(2026, 4, 11, 10, 0));
    assertThat(result.get(0).getBrewedAt()).isAfter(result.get(1).getBrewedAt());
  }
```

- [ ] **Step 3: Run the repository test to verify it passes**

Run: `cd brewdeck-api && sh mvnw -Dtest=BrewSessionRepositoryTest test`
Expected: PASS (the derived query is resolved by Spring Data; new test green alongside the existing ones).

- [ ] **Step 4: Add the `BrewHistoryEntry` record**

Create `brewdeck-api/src/main/java/com/brewdeck/brewdeck_api/ai/BrewHistoryEntry.java`:

```java
package com.brewdeck.brewdeck_api.ai;

public record BrewHistoryEntry(
    Integer rating,
    String actualGrind,
    Integer actualTemp,
    String actualTime,
    String tasteResult,
    String adjustmentNotes) {}
```

- [ ] **Step 5: Add the `ImprovementContext` record**

Create `brewdeck-api/src/main/java/com/brewdeck/brewdeck_api/ai/ImprovementContext.java`:

```java
package com.brewdeck.brewdeck_api.ai;

import java.math.BigDecimal;
import java.util.List;

public record ImprovementContext(
    String coffeeName,
    String origin,
    String roastLevel,
    String process,
    Integer acidityScore,
    Integer bodyScore,
    Integer sweetnessScore,
    Integer bitternessScore,
    String methodName,
    String methodDescription,
    BigDecimal currentCoffeeGrams,
    BigDecimal currentWaterGrams,
    String currentRatio,
    String currentGrindSetting,
    Integer currentWaterTemp,
    String currentBrewTime,
    String currentSteps,
    List<BrewHistoryEntry> history) {}
```

- [ ] **Step 6: Add the `InsufficientBrewHistoryException`**

Create `brewdeck-api/src/main/java/com/brewdeck/brewdeck_api/ai/InsufficientBrewHistoryException.java`:

```java
package com.brewdeck.brewdeck_api.ai;

public class InsufficientBrewHistoryException extends RuntimeException {

  public InsufficientBrewHistoryException(String message) {
    super(message);
  }
}
```

- [ ] **Step 7: Extend the port**

Replace the body of `RecipeSuggestionPort.java` with:

```java
package com.brewdeck.brewdeck_api.ai;

public interface RecipeSuggestionPort {
  SuggestedRecipe suggest(SuggestionContext context);

  SuggestedRecipe improve(ImprovementContext context);
}
```

- [ ] **Step 8: Implement `improve` on the disabled adapter**

In `DisabledRecipeSuggestionAdapter.java`, add the override after the existing `suggest` method:

```java
  @Override
  public SuggestedRecipe improve(ImprovementContext context) {
    throw new AiUnavailableException("AI suggestions are disabled");
  }
```

- [ ] **Step 9: Implement `improve` on the Claude adapter**

In `ClaudeRecipeSuggestionAdapter.java`:

Add this constant next to the existing `SYSTEM_PROMPT`:

```java
  private static final String IMPROVE_SYSTEM_PROMPT =
      "You are an expert barista tuning an existing coffee recipe using its brew history."
          + " Given the current parameters and recent rated brews, return improved brewing"
          + " parameters as structured data only. Water temperature is in degrees Celsius,"
          + " between 70 and 100. Keep steps concise, and use the rationale to explain what you"
          + " changed and why.";
```

Add the `improve` method after the existing `suggest` method:

```java
  @Override
  public SuggestedRecipe improve(ImprovementContext context) {
    try {
      StructuredMessageCreateParams<SuggestedRecipePayload> params =
          MessageCreateParams.builder()
              .model(properties.model())
              .maxTokens((long) properties.maxTokens())
              .system(IMPROVE_SYSTEM_PROMPT)
              .addUserMessage(buildImproveMessage(context))
              .outputConfig(SuggestedRecipePayload.class)
              .build();

      SuggestedRecipePayload payload =
          client.messages().create(params).content().stream()
              .flatMap(block -> block.text().stream())
              .map(text -> text.text())
              .findFirst()
              .orElseThrow(() -> new AiUnavailableException("Empty AI response"));

      return toSuggestedRecipe(payload);
    } catch (AiUnavailableException exception) {
      throw exception;
    } catch (RuntimeException exception) {
      log.warn("AI improvement call failed", exception);
      throw new AiUnavailableException("AI improvement call failed", exception);
    }
  }

  private String buildImproveMessage(ImprovementContext c) {
    StringBuilder message = new StringBuilder();
    message
        .append("Coffee: ")
        .append(c.coffeeName())
        .append("\nOrigin: ")
        .append(orDash(c.origin()))
        .append("\nRoast: ")
        .append(orDash(c.roastLevel()))
        .append("\nProcess: ")
        .append(orDash(c.process()))
        .append("\nTasting scores (1-5): acidity=")
        .append(orDash(c.acidityScore()))
        .append(", body=")
        .append(orDash(c.bodyScore()))
        .append(", sweetness=")
        .append(orDash(c.sweetnessScore()))
        .append(", bitterness=")
        .append(orDash(c.bitternessScore()))
        .append("\nBrew method: ")
        .append(c.methodName())
        .append(" (")
        .append(orDash(c.methodDescription()))
        .append(")")
        .append("\n\nCurrent recipe parameters:")
        .append("\n  Coffee grams: ")
        .append(orDash(c.currentCoffeeGrams()))
        .append("\n  Water grams: ")
        .append(orDash(c.currentWaterGrams()))
        .append("\n  Ratio: ")
        .append(orDash(c.currentRatio()))
        .append("\n  Grind: ")
        .append(orDash(c.currentGrindSetting()))
        .append("\n  Water temp: ")
        .append(orDash(c.currentWaterTemp()))
        .append("\n  Brew time: ")
        .append(orDash(c.currentBrewTime()))
        .append("\n  Steps: ")
        .append(orDash(c.currentSteps()))
        .append("\n\nRecent rated brews (newest first):");

    int index = 1;
    for (BrewHistoryEntry entry : c.history()) {
      message
          .append("\n  ")
          .append(index++)
          .append(". rating=")
          .append(orDash(entry.rating()))
          .append(", grind=")
          .append(orDash(entry.actualGrind()))
          .append(", temp=")
          .append(orDash(entry.actualTemp()))
          .append(", time=")
          .append(orDash(entry.actualTime()))
          .append(", taste=")
          .append(orDash(entry.tasteResult()))
          .append(", notes=")
          .append(orDash(entry.adjustmentNotes()));
    }

    return message.toString();
  }
```

(The existing private `orDash(Object)` helper is reused; no new import is needed — `StructuredMessageCreateParams`, `MessageCreateParams`, and `BigDecimal` are already imported in this file.)

- [ ] **Step 10: Add the 422 exception handler**

In `GlobalExceptionHandler.java`, add the import next to the existing AI import:

```java
import com.brewdeck.brewdeck_api.ai.InsufficientBrewHistoryException;
```

Add this handler method after `handleAiUnavailable`:

```java
  @ExceptionHandler(InsufficientBrewHistoryException.class)
  public ResponseEntity<ErrorResponse> handleInsufficientBrewHistory(
      InsufficientBrewHistoryException exception, HttpServletRequest request) {
    ErrorResponse errorResponse =
        buildErrorResponse(
            HttpStatus.UNPROCESSABLE_ENTITY,
            "Recipe has no rated brew sessions to improve from",
            sanitize(request.getRequestURI()),
            null);

    return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(errorResponse);
  }
```

- [ ] **Step 11: Write the failing service test**

Create `brewdeck-api/src/test/java/com/brewdeck/brewdeck_api/ai/RecipeImprovementServiceTest.java`:

```java
package com.brewdeck.brewdeck_api.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.brewdeck.brewdeck_api.coffee.Coffee;
import com.brewdeck.brewdeck_api.method.BrewMethod;
import com.brewdeck.brewdeck_api.recipe.Recipe;
import com.brewdeck.brewdeck_api.recipe.RecipeRepository;
import com.brewdeck.brewdeck_api.session.BrewSession;
import com.brewdeck.brewdeck_api.session.BrewSessionRepository;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RecipeImprovementServiceTest {

  @Mock private RecipeRepository recipeRepository;
  @Mock private BrewSessionRepository brewSessionRepository;
  @Mock private RecipeSuggestionPort port;

  private RecipeImprovementService serviceEnabled() {
    return new RecipeImprovementService(
        recipeRepository,
        brewSessionRepository,
        port,
        new AiProperties(true, "claude-haiku-4-5", 20, 1024));
  }

  private Recipe sampleRecipe() {
    Coffee coffee =
        Coffee.builder()
            .id(1L)
            .name("Mezcla Veracruz")
            .origin("Veracruz")
            .process("Lavado")
            .roastLevel("Medio")
            .acidityScore(3)
            .bodyScore(3)
            .sweetnessScore(3)
            .bitternessScore(2)
            .build();
    BrewMethod method =
        BrewMethod.builder().id(2L).name("AeroPress").description("Immersion").build();
    return Recipe.builder()
        .id(5L)
        .coffee(coffee)
        .method(method)
        .name("Mezcla AeroPress")
        .coffeeGrams(new BigDecimal("15"))
        .waterGrams(new BigDecimal("230"))
        .ratio("1:15")
        .grindSetting("Timemore S3 - 5.5")
        .waterTemp(90)
        .brewTime("2:30")
        .steps("Bloom then press.")
        .favorite(false)
        .build();
  }

  @Test
  void improve_shouldThrowAiUnavailable_whenDisabled() {
    RecipeImprovementService service =
        new RecipeImprovementService(
            recipeRepository,
            brewSessionRepository,
            port,
            new AiProperties(false, "claude-haiku-4-5", 20, 1024));

    assertThatThrownBy(() -> service.improve(5L)).isInstanceOf(AiUnavailableException.class);
    verify(port, never()).improve(any());
  }

  @Test
  void improve_shouldThrowNotFound_whenRecipeMissing() {
    when(recipeRepository.findById(5L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> serviceEnabled().improve(5L))
        .isInstanceOf(EntityNotFoundException.class);
    verify(port, never()).improve(any());
  }

  @Test
  void improve_shouldThrowInsufficientHistory_whenNoRatedSessions() {
    when(recipeRepository.findById(5L)).thenReturn(Optional.of(sampleRecipe()));
    when(brewSessionRepository.findTop10ByRecipeIdAndRatingIsNotNullOrderByBrewedAtDesc(5L))
        .thenReturn(List.of());

    assertThatThrownBy(() -> serviceEnabled().improve(5L))
        .isInstanceOf(InsufficientBrewHistoryException.class);
    verify(port, never()).improve(any());
  }

  @Test
  void improve_shouldReturnMappedResponse_whenHistoryPresent() {
    Recipe recipe = sampleRecipe();
    BrewSession session =
        BrewSession.builder()
            .recipe(recipe)
            .brewedAt(LocalDateTime.of(2026, 4, 21, 10, 0))
            .actualGrind("Timemore S3 - 5.5")
            .actualTemp(91)
            .actualTime("2:20")
            .tasteResult("Bright")
            .rating(9)
            .adjustmentNotes("Grind finer next time.")
            .build();
    when(recipeRepository.findById(5L)).thenReturn(Optional.of(recipe));
    when(brewSessionRepository.findTop10ByRecipeIdAndRatingIsNotNullOrderByBrewedAtDesc(5L))
        .thenReturn(List.of(session));
    when(port.improve(any()))
        .thenReturn(
            new SuggestedRecipe(
                new BigDecimal("16"),
                new BigDecimal("240"),
                "1:15",
                "Timemore S3 - 5.0",
                92,
                "2:15",
                "Grind finer and shorten the brew.",
                "Finer grind improves sweetness."));

    ArgumentCaptor<ImprovementContext> captor = ArgumentCaptor.forClass(ImprovementContext.class);

    SuggestedRecipeResponse result = serviceEnabled().improve(5L);

    verify(port).improve(captor.capture());
    ImprovementContext context = captor.getValue();
    assertThat(context.coffeeName()).isEqualTo("Mezcla Veracruz");
    assertThat(context.methodName()).isEqualTo("AeroPress");
    assertThat(context.currentWaterTemp()).isEqualTo(90);
    assertThat(context.history()).hasSize(1);
    assertThat(context.history().get(0).rating()).isEqualTo(9);
    assertThat(context.history().get(0).tasteResult()).isEqualTo("Bright");
    assertThat(result.waterTemp()).isEqualTo(92);
    assertThat(result.rationale()).isEqualTo("Finer grind improves sweetness.");
  }
}
```

- [ ] **Step 12: Run the service test to verify it fails to compile**

Run: `cd brewdeck-api && sh mvnw -Dtest=RecipeImprovementServiceTest test`
Expected: FAIL — compilation error, `RecipeImprovementService` does not exist yet.

- [ ] **Step 13: Implement `RecipeImprovementService`**

Create `brewdeck-api/src/main/java/com/brewdeck/brewdeck_api/ai/RecipeImprovementService.java`:

```java
package com.brewdeck.brewdeck_api.ai;

import com.brewdeck.brewdeck_api.coffee.Coffee;
import com.brewdeck.brewdeck_api.method.BrewMethod;
import com.brewdeck.brewdeck_api.recipe.Recipe;
import com.brewdeck.brewdeck_api.recipe.RecipeRepository;
import com.brewdeck.brewdeck_api.session.BrewSession;
import com.brewdeck.brewdeck_api.session.BrewSessionRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class RecipeImprovementService {

  private static final String RECIPE_NOT_FOUND = "Recipe not found";
  private static final String NO_RATED_HISTORY = "Recipe has no rated brew sessions";

  private final RecipeRepository recipeRepository;
  private final BrewSessionRepository brewSessionRepository;
  private final RecipeSuggestionPort suggestionPort;
  private final AiProperties aiProperties;

  public SuggestedRecipeResponse improve(Long recipeId) {
    if (!aiProperties.enabled()) {
      throw new AiUnavailableException("AI suggestions are disabled");
    }

    Recipe recipe =
        recipeRepository
            .findById(recipeId)
            .orElseThrow(() -> new EntityNotFoundException(RECIPE_NOT_FOUND));

    List<BrewSession> sessions =
        brewSessionRepository.findTop10ByRecipeIdAndRatingIsNotNullOrderByBrewedAtDesc(recipeId);
    if (sessions.isEmpty()) {
      throw new InsufficientBrewHistoryException(NO_RATED_HISTORY);
    }

    List<BrewHistoryEntry> history =
        sessions.stream()
            .map(
                session ->
                    new BrewHistoryEntry(
                        session.getRating(),
                        session.getActualGrind(),
                        session.getActualTemp(),
                        session.getActualTime(),
                        session.getTasteResult(),
                        session.getAdjustmentNotes()))
            .toList();

    Coffee coffee = recipe.getCoffee();
    BrewMethod method = recipe.getMethod();

    ImprovementContext context =
        new ImprovementContext(
            coffee.getName(),
            coffee.getOrigin(),
            coffee.getRoastLevel(),
            coffee.getProcess(),
            coffee.getAcidityScore(),
            coffee.getBodyScore(),
            coffee.getSweetnessScore(),
            coffee.getBitternessScore(),
            method.getName(),
            method.getDescription(),
            recipe.getCoffeeGrams(),
            recipe.getWaterGrams(),
            recipe.getRatio(),
            recipe.getGrindSetting(),
            recipe.getWaterTemp(),
            recipe.getBrewTime(),
            recipe.getSteps(),
            history);

    SuggestedRecipe suggested = suggestionPort.improve(context);
    log.info(
        "Generated recipe improvement recipeId={} ratedSessions={} model={}",
        recipeId,
        history.size(),
        aiProperties.model());

    return new SuggestedRecipeResponse(
        suggested.coffeeGrams(),
        suggested.waterGrams(),
        suggested.ratio(),
        suggested.grindSetting(),
        suggested.waterTemp(),
        suggested.brewTime(),
        suggested.steps(),
        suggested.rationale());
  }
}
```

- [ ] **Step 14: Run the service test to verify it passes**

Run: `cd brewdeck-api && sh mvnw -Dtest=RecipeImprovementServiceTest test`
Expected: PASS — all four tests green.

- [ ] **Step 15: Write the failing controller test**

Create `brewdeck-api/src/test/java/com/brewdeck/brewdeck_api/ai/RecipeImprovementControllerTest.java`:

```java
package com.brewdeck.brewdeck_api.ai;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(RecipeImprovementController.class)
class RecipeImprovementControllerTest {

  @Autowired private MockMvc mockMvc;
  @MockitoBean private RecipeImprovementService service;

  @Test
  void improve_shouldReturnImprovement() throws Exception {
    when(service.improve(5L))
        .thenReturn(
            new SuggestedRecipeResponse(
                new BigDecimal("16"),
                new BigDecimal("240"),
                "1:15",
                "Timemore S3 - 5.0",
                92,
                "2:15",
                "Grind finer.",
                "Finer grind improves sweetness."));

    mockMvc
        .perform(post("/api/recipes/5/improve"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.waterTemp").value(92))
        .andExpect(jsonPath("$.rationale").value("Finer grind improves sweetness."));
  }

  @Test
  void improve_shouldReturnNotFound_whenRecipeMissing() throws Exception {
    when(service.improve(5L)).thenThrow(new EntityNotFoundException("Recipe not found"));

    mockMvc.perform(post("/api/recipes/5/improve")).andExpect(status().isNotFound());
  }

  @Test
  void improve_shouldReturnUnprocessable_whenNoRatedHistory() throws Exception {
    when(service.improve(5L))
        .thenThrow(new InsufficientBrewHistoryException("Recipe has no rated brew sessions"));

    mockMvc
        .perform(post("/api/recipes/5/improve"))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.status").value(422));
  }

  @Test
  void improve_shouldReturnServiceUnavailable_whenAiUnavailable() throws Exception {
    when(service.improve(5L)).thenThrow(new AiUnavailableException("disabled"));

    mockMvc
        .perform(post("/api/recipes/5/improve"))
        .andExpect(status().isServiceUnavailable())
        .andExpect(jsonPath("$.status").value(503));
  }
}
```

- [ ] **Step 16: Run the controller test to verify it fails to compile**

Run: `cd brewdeck-api && sh mvnw -Dtest=RecipeImprovementControllerTest test`
Expected: FAIL — compilation error, `RecipeImprovementController` does not exist yet.

- [ ] **Step 17: Implement `RecipeImprovementController`**

Create `brewdeck-api/src/main/java/com/brewdeck/brewdeck_api/ai/RecipeImprovementController.java`:

```java
package com.brewdeck.brewdeck_api.ai;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/recipes")
@RequiredArgsConstructor
@Tag(name = "AI Recipe Improvements", description = "AI-improved brewing parameters from history")
public class RecipeImprovementController {

  private final RecipeImprovementService recipeImprovementService;

  @PostMapping("/{id}/improve")
  @Operation(
      summary = "Improve a recipe from its brew history",
      description =
          "Generates improved AI brewing parameters from the recipe's recent rated brews. The"
              + " result is not persisted; the client uses it to pre-fill the recipe form.")
  public ResponseEntity<SuggestedRecipeResponse> improve(@PathVariable Long id) {
    return ResponseEntity.ok(recipeImprovementService.improve(id));
  }
}
```

- [ ] **Step 18: Run the controller test to verify it passes**

Run: `cd brewdeck-api && sh mvnw -Dtest=RecipeImprovementControllerTest test`
Expected: PASS — all four tests green (the `@RestControllerAdvice` `GlobalExceptionHandler` is loaded into the web slice and produces the 404/422/503 mappings).

- [ ] **Step 19: Format and run the full backend build**

Run: `cd brewdeck-api && sh mvnw spotless:apply && sh mvnw clean verify`
Expected: BUILD SUCCESS — whole suite (unit, controller, repository, integration) green; JaCoCo/Sonar excludes already cover `ClaudeRecipeSuggestionAdapter.java`.

- [ ] **Step 20: Commit**

```bash
cd /Users/jvilla/Documents/brewdeck
git add brewdeck-api/src/main/java/com/brewdeck/brewdeck_api/ai \
        brewdeck-api/src/main/java/com/brewdeck/brewdeck_api/session/BrewSessionRepository.java \
        brewdeck-api/src/main/java/com/brewdeck/brewdeck_api/common/error/GlobalExceptionHandler.java \
        brewdeck-api/src/test/java/com/brewdeck/brewdeck_api/ai/RecipeImprovementServiceTest.java \
        brewdeck-api/src/test/java/com/brewdeck/brewdeck_api/ai/RecipeImprovementControllerTest.java \
        brewdeck-api/src/test/java/com/brewdeck/brewdeck_api/session/BrewSessionRepositoryTest.java
git commit -m "feat(api): add AI recipe improve-from-history endpoint"
```

---

### Task 2: Frontend — Improve with AI on recipe detail

Adds the API function, the mutation hook, an optional pre-filled-rationale prop on the shared dialog, and the "Improve with AI" button on the recipe detail page. Deliverable: full `npx vitest run` and `npm run type-check` green.

**Files:**
- Modify: `brewdeck-web/src/lib/api/ai.ts`
- Create: `brewdeck-web/src/hooks/useImproveRecipe.ts`
- Modify: `brewdeck-web/src/components/recipes/RecipeFormDialog.tsx`
- Modify: `brewdeck-web/src/components/recipes/RecipeDetailView.tsx`
- Modify test: `brewdeck-web/src/components/recipes/RecipeDetailView.test.tsx`

**Interfaces:**
- Consumes: `apiFetch` from `@/lib/api/client`; `ApiError` (has `.status: number`) from `@/lib/api/client`; existing `SuggestedRecipe` type from `@/lib/api/ai`; `Recipe`, `BrewSession` types from `@/lib/api/types`; `useRecipe`, `useRecipeStats` from `@/hooks/useRecipe`; `useRecipeBrewSessions` (returns a query whose `.data.content` is `BrewSession[]`); the existing `RecipeFormDialog` (props `open`, `recipe?`, `onClose`).
- Produces:
  - `improveRecipe(recipeId: number): Promise<SuggestedRecipe>` in `@/lib/api/ai`
  - `useImproveRecipe()` — TanStack `useMutation` whose `mutate(recipeId, options)` accepts `number`
  - `RecipeFormDialog` gains an optional prop `initialRationale?: string | null`

- [ ] **Step 1: Add `improveRecipe` to the API module**

In `brewdeck-web/src/lib/api/ai.ts`, add at the end of the file (after `suggestRecipe`):

```ts
export function improveRecipe(recipeId: number): Promise<SuggestedRecipe> {
  return apiFetch<SuggestedRecipe>(`/api/recipes/${recipeId}/improve`, {
    method: 'POST',
  });
}
```

- [ ] **Step 2: Add the `useImproveRecipe` hook**

Create `brewdeck-web/src/hooks/useImproveRecipe.ts`:

```ts
'use client';

import { useMutation } from '@tanstack/react-query';
import { improveRecipe } from '@/lib/api/ai';

export function useImproveRecipe() {
  return useMutation({
    mutationFn: (recipeId: number) => improveRecipe(recipeId),
  });
}
```

- [ ] **Step 3: Add the `initialRationale` prop to `RecipeFormDialog`**

In `brewdeck-web/src/components/recipes/RecipeFormDialog.tsx`:

Change the component signature to accept the new optional prop:

```tsx
export function RecipeFormDialog({
  open,
  recipe,
  onClose,
  initialRationale,
}: {
  open: boolean;
  recipe?: Recipe;
  onClose: () => void;
  initialRationale?: string | null;
}) {
```

Change the rationale state initializer from `useState<string | null>(null)` to seed from the prop:

```tsx
  const [rationale, setRationale] = useState<string | null>(initialRationale ?? null);
```

(Everything else in the dialog is unchanged. `RecipesView` does not pass `initialRationale`, so its behavior is unchanged — `initialRationale` is `undefined` there and the state seeds to `null`.)

- [ ] **Step 4: Write the failing detail-view tests**

In `brewdeck-web/src/components/recipes/RecipeDetailView.test.tsx`:

Add these mocks near the top of the file, after the existing imports (before the `type` aliases). The `fireEvent` import must be added to the `@testing-library/react` import line — change `import { screen } from '@testing-library/react';` to `import { fireEvent, screen } from '@testing-library/react';`. Also add `import { ApiError } from '@/lib/api/client';`.

```tsx
const { improveMutate } = vi.hoisted(() => ({ improveMutate: vi.fn() }));

vi.mock('@/hooks/useImproveRecipe', () => ({
  useImproveRecipe: () => ({ mutate: improveMutate, isPending: false }),
}));
vi.mock('@/hooks/useRecipeMutations', () => ({
  useCreateRecipe: () => ({ mutate: vi.fn(), isPending: false }),
  useUpdateRecipe: () => ({ mutate: vi.fn(), isPending: false }),
  useDeleteRecipe: () => ({ mutate: vi.fn(), isPending: false }),
}));
vi.mock('@/hooks/useResourceOptions', () => ({
  useCoffeeOptions: () => ({ data: [], isLoading: false }),
  useMethodOptions: () => ({ data: [], isLoading: false }),
}));
vi.mock('@/hooks/useSuggestRecipe', () => ({
  useSuggestRecipe: () => ({ mutate: vi.fn(), isPending: false }),
}));
```

Add a rated/unrated session fixture near the `recipe` fixture:

```tsx
const ratedSession: BrewSession = {
  id: 1,
  recipeId: 1,
  recipeName: 'Mezcla Veracruz AeroPress',
  brewedAt: '2026-04-21T10:00:00',
  actualGrind: 'Timemore S3 - 5.5',
  actualTemp: 91,
  actualTime: '2:20',
  tasteResult: 'Bright',
  rating: 9,
  adjustmentNotes: 'Grind finer next time.',
};

const unratedSession: BrewSession = { ...ratedSession, id: 2, rating: null };
```

Add `beforeEach(() => improveMutate.mockReset());` inside the existing `describe` block setup — put it right after the file's existing top-level `beforeEach` (both may coexist; alternatively add `improveMutate.mockReset();` inside the existing `beforeEach`). Then add these tests inside the `describe('RecipeDetailView', ...)` block:

```tsx
  it('disables Improve with AI when there is no rated history', () => {
    mockRecipe({ isLoading: false, isError: false, data: recipe });
    mockStats({ isLoading: false, isError: false, data: undefined });
    mockHistory({ isLoading: false, isError: false, data: sessionsPage([unratedSession]) });

    renderWithTheme(<RecipeDetailView recipeId={1} />);

    expect(screen.getByRole('button', { name: /improve with ai/i })).toBeDisabled();
  });

  it('enables Improve with AI when at least one session is rated', () => {
    mockRecipe({ isLoading: false, isError: false, data: recipe });
    mockStats({ isLoading: false, isError: false, data: undefined });
    mockHistory({ isLoading: false, isError: false, data: sessionsPage([ratedSession]) });

    renderWithTheme(<RecipeDetailView recipeId={1} />);

    expect(screen.getByRole('button', { name: /improve with ai/i })).toBeEnabled();
  });

  it('opens the pre-filled edit dialog with the rationale on success', () => {
    improveMutate.mockImplementation((_id: number, { onSuccess }: { onSuccess: (data: unknown) => void }) =>
      onSuccess({
        coffeeGrams: 16,
        waterGrams: 240,
        ratio: '1:15',
        grindSetting: 'Timemore S3 - 5.0',
        waterTemp: 92,
        brewTime: '2:15',
        steps: 'Grind finer and shorten the brew.',
        rationale: 'Finer grind improves sweetness.',
      }),
    );
    mockRecipe({ isLoading: false, isError: false, data: recipe });
    mockStats({ isLoading: false, isError: false, data: undefined });
    mockHistory({ isLoading: false, isError: false, data: sessionsPage([ratedSession]) });

    renderWithTheme(<RecipeDetailView recipeId={1} />);
    fireEvent.click(screen.getByRole('button', { name: /improve with ai/i }));

    expect(screen.getByRole('dialog')).toBeInTheDocument();
    expect(screen.getByText('Edit recipe')).toBeInTheDocument();
    expect(screen.getByText('Finer grind improves sweetness.')).toBeInTheDocument();
  });

  it('shows a needs-history message when improve returns 422', () => {
    improveMutate.mockImplementation((_id: number, { onError }: { onError: (error: unknown) => void }) =>
      onError(new ApiError(422, 'Recipe has no rated brew sessions to improve from')),
    );
    mockRecipe({ isLoading: false, isError: false, data: recipe });
    mockStats({ isLoading: false, isError: false, data: undefined });
    mockHistory({ isLoading: false, isError: false, data: sessionsPage([ratedSession]) });

    renderWithTheme(<RecipeDetailView recipeId={1} />);
    fireEvent.click(screen.getByRole('button', { name: /improve with ai/i }));

    expect(screen.getByText(/log a rated brew for this recipe first/i)).toBeInTheDocument();
  });
```

Also add `BrewSession` to the type import from `@/lib/api/types` if not already present (the file already imports `BrewSession`).

- [ ] **Step 5: Run the detail-view tests to verify they fail**

Run: `cd brewdeck-web && npx vitest run src/components/recipes/RecipeDetailView.test.tsx`
Expected: FAIL — no "Improve with AI" button exists yet.

- [ ] **Step 6: Add the improve flow to `RecipeDetailView`**

In `brewdeck-web/src/components/recipes/RecipeDetailView.tsx`:

Add these imports (group with existing MUI / internal imports):

```tsx
import Alert from '@mui/material/Alert';
import CircularProgress from '@mui/material/CircularProgress';
import Tooltip from '@mui/material/Tooltip';
import { useState } from 'react';
import type { Recipe } from '@/lib/api/types';
import { ApiError } from '@/lib/api/client';
import { useImproveRecipe } from '@/hooks/useImproveRecipe';
import { RecipeFormDialog } from './RecipeFormDialog';
```

Immediately after the three existing query hooks (`recipeQuery`, `statsQuery`, `historyQuery`) and **before** the early `return` guards, add:

```tsx
  const improve = useImproveRecipe();
  const [improved, setImproved] = useState<{ recipe: Recipe; rationale: string } | null>(null);
  const [improveError, setImproveError] = useState<string | null>(null);
```

After the line `const recipe = recipeQuery.data;`, add:

```tsx
  const hasRatedHistory =
    historyQuery.data?.content.some((session) => session.rating !== null) ?? false;

  const onImprove = () => {
    setImproveError(null);
    improve.mutate(recipe.id, {
      onSuccess: (data) => {
        setImproved({
          recipe: {
            ...recipe,
            coffeeGrams: data.coffeeGrams ?? recipe.coffeeGrams,
            waterGrams: data.waterGrams ?? recipe.waterGrams,
            ratio: data.ratio ?? recipe.ratio,
            grindSetting: data.grindSetting ?? recipe.grindSetting,
            waterTemp: data.waterTemp ?? recipe.waterTemp,
            brewTime: data.brewTime ?? recipe.brewTime,
            steps: data.steps ?? recipe.steps,
          },
          rationale: data.rationale,
        });
      },
      onError: (error) => {
        if (error instanceof ApiError && error.status === 422) {
          setImproveError('Log a rated brew for this recipe first, then try again.');
        } else {
          setImproveError('AI improvements are unavailable right now. Please try again later.');
        }
      },
    });
  };
```

In the returned JSX, immediately after the `<Box>` that renders the recipe title + favorite chip (the block containing `<Typography variant="h5" component="h1">`), add the button and error alert:

```tsx
      <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 2 }}>
        <Tooltip title={hasRatedHistory ? '' : 'Log a rated brew to enable AI improvements'}>
          <span>
            <Button
              variant="outlined"
              size="small"
              disabled={!hasRatedHistory || improve.isPending}
              onClick={onImprove}
              startIcon={improve.isPending ? <CircularProgress size={16} /> : undefined}
            >
              Improve with AI
            </Button>
          </span>
        </Tooltip>
      </Box>
      {improveError ? (
        <Alert severity="error" sx={{ mb: 2 }}>
          {improveError}
        </Alert>
      ) : null}
```

Immediately before the closing `</>` of the returned fragment, add the dialog:

```tsx
      {improved ? (
        <RecipeFormDialog
          open
          recipe={improved.recipe}
          initialRationale={improved.rationale}
          onClose={() => setImproved(null)}
        />
      ) : null}
```

- [ ] **Step 7: Run the detail-view tests to verify they pass**

Run: `cd brewdeck-web && npx vitest run src/components/recipes/RecipeDetailView.test.tsx`
Expected: PASS — all detail-view tests green (existing + 4 new).

- [ ] **Step 8: Run the full frontend suite and type-check**

Run: `cd brewdeck-web && npx vitest run && npm run type-check`
Expected: PASS — full Vitest suite green (adding `useImproveRecipe`/`RecipeFormDialog` prop must not break sibling tests such as `RecipesView.test.tsx`; those already mock the mutation hooks and do not pass `initialRationale`), and `tsc --noEmit` reports no errors.

- [ ] **Step 9: Lint the changed files**

Run: `cd brewdeck-web && npm run lint:fix -- src/lib/api/ai.ts src/hooks/useImproveRecipe.ts src/components/recipes/RecipeFormDialog.tsx src/components/recipes/RecipeDetailView.tsx src/components/recipes/RecipeDetailView.test.tsx`
Expected: No lint errors on the changed files (scope to changed files only — never the whole repo).

- [ ] **Step 10: Commit**

```bash
cd /Users/jvilla/Documents/brewdeck
git add brewdeck-web/src/lib/api/ai.ts \
        brewdeck-web/src/hooks/useImproveRecipe.ts \
        brewdeck-web/src/components/recipes/RecipeFormDialog.tsx \
        brewdeck-web/src/components/recipes/RecipeDetailView.tsx \
        brewdeck-web/src/components/recipes/RecipeDetailView.test.tsx
git commit -m "feat(web): add Improve with AI button on recipe detail"
```

---

### Task 3: Docs — Postman, roadmap, project-state

Registers the new endpoint in Postman and marks the slice done in the project docs. Deliverable: valid Postman JSON, updated roadmap/project-state.

**Files:**
- Modify: `docs/postman/brewdeck.postman_collection.json`
- Modify: `.claude/roadmap.md`
- Modify: `.claude/project-state.md`

**Interfaces:**
- Consumes: endpoint contract from Task 1 (`POST /api/recipes/{id}/improve`); Postman env var `{{recipeId}}` and `{{baseUrl}}` (already defined in the collection/environment).
- Produces: documentation only; nothing downstream depends on it.

- [ ] **Step 1: Add the Postman request**

Open `docs/postman/brewdeck.postman_collection.json` and locate the existing "Suggest recipe" request (the one that POSTs to `/api/recipes/suggest`). Add a sibling request item in the same folder/array, mirroring the existing item's structure. Use exactly this request item (adjust the surrounding commas so the JSON stays valid):

```json
{
  "name": "Improve recipe from history",
  "request": {
    "method": "POST",
    "header": [],
    "url": {
      "raw": "{{baseUrl}}/api/recipes/{{recipeId}}/improve",
      "host": ["{{baseUrl}}"],
      "path": ["api", "recipes", "{{recipeId}}", "improve"]
    }
  },
  "response": []
}
```

If the existing "Suggest recipe" item uses `{{base_url}}` (underscore) rather than `{{baseUrl}}`, match whichever variable name that item already uses so the two requests are consistent.

- [ ] **Step 2: Validate the Postman JSON**

Run: `cd /Users/jvilla/Documents/brewdeck && python3 -m json.tool docs/postman/brewdeck.postman_collection.json > /dev/null && echo VALID`
Expected: prints `VALID` (no JSON parse error).

- [ ] **Step 3: Update the roadmap**

In `.claude/roadmap.md`, replace the AI line under "Phase 5 — Product Improvements":

Old:
```
- AI-assisted recipe suggestions (generate brew params; Claude via Java SDK) — Done, generate slice (PR #58); improve-from-history slice deferred
```

New:
```
- AI-assisted recipe suggestions — Done: generate slice (PR #58) + improve-from-history slice (POST /api/recipes/{id}/improve, "Improve with AI" on recipe detail)
```

- [ ] **Step 4: Update the project state**

In `.claude/project-state.md`:

Update the "Last Updated" date to `2026-07-07` (leave as-is if already that date).

In the "Current Phase" paragraph, replace the trailing `Next: remaining Phase 5 features (AI improve-from-history slice, export recipes to PDF, public share links).` with:
```
AI recipe improve-from-history slice shipped full-stack: feature-toggled POST /api/recipes/{id}/improve tunes an existing recipe from its recent rated brew sessions (extends RecipeSuggestionPort with improve, 422 when no rated history), and an "Improve with AI" button on the recipe detail page pre-fills the edit dialog. Next: remaining Phase 5 features (export recipes to PDF, public share links).
```

Add to the "Recently Worked On" list (at the top):
```
- AI recipe improve-from-history slice — extends RecipeSuggestionPort with improve(ImprovementContext); POST /api/recipes/{id}/improve loads the recipe + its top-10 rated sessions, returns AI-tuned brewing params (SuggestedRecipeResponse); 404 recipe-missing, 422 no-rated-history, 503 AI-off/SDK-fail; frontend "Improve with AI" button (disabled with tooltip until a rated brew exists) opens the existing RecipeFormDialog pre-filled + rationale
```

In "Immediate Next Steps", remove the improve-from-history line (item 1) and renumber so export-to-PDF / public-share-links lead.

- [ ] **Step 5: Commit**

```bash
cd /Users/jvilla/Documents/brewdeck
git add docs/postman/brewdeck.postman_collection.json .claude/roadmap.md .claude/project-state.md
git commit -m "docs: record AI recipe improve-from-history slice"
```

---

## Self-Review

**1. Spec coverage:**
- Endpoint `POST /api/recipes/{id}/improve`, no body, returns `SuggestedRecipeResponse` — Task 1 Steps 17.
- 404 recipe-missing / 422 no-rated-history / 503 AI-off — Task 1 Steps 10, 13, 15–18.
- Extend port + both adapters (Approach A) — Task 1 Steps 7–9.
- New types `ImprovementContext`, `BrewHistoryEntry` — Task 1 Steps 4–5.
- Repo query `findTop10ByRecipeIdAndRatingIsNotNullOrderByBrewedAtDesc` (rated-only, newest-first, cap 10) — Task 1 Steps 1–3.
- Frontend `improveRecipe` + `useImproveRecipe` + button gated on loaded rated history + tooltip + success pre-fill + rationale + error messages (422 vs other) — Task 2 Steps 1–6.
- Adapter fail-soft / no live SDK call in tests / Sonar exclude — carried by reusing the generate-slice adapter; no new live path.
- Postman + roadmap + project-state — Task 3.

**2. Placeholder scan:** No TBD/TODO/"handle edge cases"/"similar to Task N" — every code and test step contains full content.

**3. Type consistency:** `improve(ImprovementContext)` returns `SuggestedRecipe` (port/adapters/service), mapped to `SuggestedRecipeResponse` (controller); `ImprovementContext` field names match between the record (Task 1 Step 5), the service constructor call (Step 13), and the adapter message builder (Step 9); `BrewHistoryEntry` accessors (`rating`, `actualGrind`, `actualTemp`, `actualTime`, `tasteResult`, `adjustmentNotes`) match the record, the service mapping, and the adapter loop; frontend `SuggestedRecipe` field names (`coffeeGrams`…`rationale`) match the merge in `RecipeDetailView` and the dialog; `initialRationale` prop name is consistent between `RecipeFormDialog` (Task 2 Step 3) and `RecipeDetailView` (Step 6).

## Execution Handoff

**Plan complete and saved to `docs/superpowers/plans/2026-07-07-ai-recipe-improve.md`. Two execution options:**

**1. Subagent-Driven (recommended)** — fresh subagent per task, review between tasks, fast iteration.

**2. Inline Execution** — execute tasks in this session with checkpoints.

**Which approach?**
