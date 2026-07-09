# AI Recipe Suggestions Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a `POST /api/recipes/suggest` endpoint that asks Claude (via the official Java SDK) to generate brewing parameters for a coffee + brew method, plus a "Suggest with AI" button on the recipe form that pre-fills the fields with the result.

**Architecture:** Hexagonal. A `RecipeSuggestionPort` domain interface with a `ClaudeRecipeSuggestionAdapter` infrastructure implementation (structured outputs, no free-text parsing). A `RecipeSuggestionService` loads the coffee + method, builds a prompt context, and calls the port. Result is ephemeral — the frontend pre-fills the recipe form; nothing is persisted by the suggestion call.

**Tech Stack:** Java 21, Spring Boot 3, `com.anthropic:anthropic-java`, Bean Validation, JUnit 5 + Mockito + MockMvc. Next.js 15 / React 19 / TypeScript, MUI, react-hook-form, TanStack Query, Vitest + React Testing Library.

## Global Constraints

- Model `claude-haiku-4-5`, held in config (`brewdeck.ai.model`) — never hardcode in Java.
- `ANTHROPIC_API_KEY` from the environment only; never in source, logs, or committed config. Commit `.env.example`, not `.env`.
- Feature toggle `brewdeck.ai.enabled` (default `false` via `AI_ENABLED`). Disabled → endpoint returns `503`.
- `POST /api/recipes/suggest` returns the DTO directly (an action, not a browsable collection) — never `PageResponse`.
- Water temperature bounds: 70–100 (write "degrees Celsius", never the degree symbol, in any message).
- Suggested numeric types mirror `RecipeRequest`: `coffeeGrams`/`waterGrams` are `BigDecimal`, `waterTemp` is `Integer`.
- Never leak JPA entities or SDK types from controllers — map through explicit records.
- Adapter fails soft: any SDK error, timeout, refusal, or malformed reply → `AiUnavailableException` → `503`, never a raw stack trace to the client.
- Tests never call the real API. Test profile sets `brewdeck.ai.enabled=false`. CI has no key.
- Backend verify: `./mvnw spotless:apply` then `./mvnw clean verify`. Frontend verify (in `brewdeck-web/`): `npm run test`, `npm run type-check`, `npm run lint:fix -- <changed files>`, `npm run build`.

---

## File Structure

**Backend (new package `com.brewdeck.brewdeck_api.ai`):**
- `AiProperties.java` — `@ConfigurationProperties(prefix = "brewdeck.ai")` record.
- `SuggestRecipeRequest.java` — validated request record.
- `SuggestedRecipeResponse.java` — response DTO record.
- `SuggestionContext.java` — port input record (resolved coffee + method attributes + notes).
- `SuggestedRecipe.java` — port output record (domain).
- `RecipeSuggestionPort.java` — domain interface.
- `AiUnavailableException.java` — runtime exception.
- `RecipeSuggestionService.java` — orchestration.
- `RecipeSuggestionController.java` — `POST /api/recipes/suggest`.
- `ClaudeRecipeSuggestionAdapter.java` — SDK adapter (excluded from Sonar coverage).

**Backend modified:**
- `pom.xml` — add `anthropic-java` dependency; add adapter to `sonar.coverage.exclusions`.
- `src/main/resources/application.yaml` — add `brewdeck.ai.*` block.
- `src/test/resources/application-test.yml` — add `brewdeck.ai.enabled: false`.
- `GlobalExceptionHandler.java` — add `AiUnavailableException` → `503` handler.
- `.env.example` — create at repo root, document `AI_ENABLED` + `ANTHROPIC_API_KEY`.

**Backend tests:**
- `RecipeSuggestionServiceTest.java`, `RecipeSuggestionControllerTest.java`, `ClaudeRecipeSuggestionAdapterTest.java`.

**Frontend:**
- Create `src/lib/api/ai.ts`, `src/hooks/useSuggestRecipe.ts`.
- Modify `src/components/recipes/RecipeFormDialog.tsx`.
- Test `src/hooks/useSuggestRecipe.test.tsx` (optional light), `src/components/recipes/RecipeFormDialog.test.tsx` (extend).

**Docs:**
- `.claude/project-state.md`, `.claude/roadmap.md`, `docs/postman/brewdeck.postman_collection.json`.

---

### Task 1: Backend — suggestion port, adapter, service, endpoint

Java compilation ties the entity/records/service/controller/adapter together, so this is one cohesive task ending in a green `./mvnw clean verify`. TDD order: config + records first, then service (test-first), controller (test-first), adapter (test the mapping), then build.

**Files:**
- Create all 10 files in `src/main/java/com/brewdeck/brewdeck_api/ai/` (listed above).
- Modify: `pom.xml`, `src/main/resources/application.yaml`, `src/test/resources/application-test.yml`, `GlobalExceptionHandler.java`, create `.env.example`.
- Test: `RecipeSuggestionServiceTest.java`, `RecipeSuggestionControllerTest.java`, `ClaudeRecipeSuggestionAdapterTest.java`.

**Interfaces:**
- Consumes: `CoffeeRepository.findById(Long)`, `BrewMethodRepository.findById(Long)`, `Coffee` getters (`getName/getOrigin/getRoastLevel/getProcess/getAcidityScore/getBodyScore/getSweetnessScore/getBitternessScore`), `BrewMethod` getters (`getName/getDescription`).
- Produces:
  - `RecipeSuggestionPort.suggest(SuggestionContext) -> SuggestedRecipe`.
  - `SuggestedRecipe(BigDecimal coffeeGrams, BigDecimal waterGrams, String ratio, String grindSetting, Integer waterTemp, String brewTime, String steps, String rationale)`.
  - `SuggestedRecipeResponse` — same 8 components as `SuggestedRecipe`.
  - `SuggestRecipeRequest(Long coffeeId, Long methodId, String notes)`.
  - `SuggestionContext(String coffeeName, String origin, String roastLevel, String process, Integer acidityScore, Integer bodyScore, Integer sweetnessScore, Integer bitternessScore, String methodName, String methodDescription, String notes)`.
  - `AiUnavailableException extends RuntimeException`.
  - `AiProperties(boolean enabled, String model, int timeoutSeconds, int maxTokens)`.

- [ ] **Step 1: Add the Maven dependency**

In `pom.xml`, inside `<dependencies>`, add (pin the current release; if `2.34.0` is stale the build will report it — bump to the latest on Maven Central):

```xml
<dependency>
  <groupId>com.anthropic</groupId>
  <artifactId>anthropic-java</artifactId>
  <version>2.34.0</version>
</dependency>
```

- [ ] **Step 2: Add config properties**

In `src/main/resources/application.yaml`, append a top-level block:

```yaml
brewdeck:
  ai:
    enabled: ${AI_ENABLED:false}
    model: claude-haiku-4-5
    timeout-seconds: 20
    max-tokens: 1024
```

In `src/test/resources/application-test.yml`, add under the root (so the build needs no key):

```yaml
brewdeck:
  ai:
    enabled: false
    model: claude-haiku-4-5
    timeout-seconds: 20
    max-tokens: 1024
```

Create `.env.example` at the repository root:

```
# Backend
SPRING_PROFILES_ACTIVE=local
SERVER_PORT=8080
CORS_ALLOWED_ORIGINS=http://localhost:3000

# AI recipe suggestions (leave AI_ENABLED=false and the key blank to keep the feature off)
AI_ENABLED=false
ANTHROPIC_API_KEY=
```

- [ ] **Step 3: Create `AiProperties`**

`src/main/java/com/brewdeck/brewdeck_api/ai/AiProperties.java`:

```java
package com.brewdeck.brewdeck_api.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "brewdeck.ai")
public record AiProperties(boolean enabled, String model, int timeoutSeconds, int maxTokens) {}
```

Enable binding by adding `@ConfigurationPropertiesScan` to the main application class (`BrewdeckApiApplication.java`) if it is not already present — check first; if `@ConfigurationPropertiesScan` or an existing `@EnableConfigurationProperties` is absent, add `@ConfigurationPropertiesScan` next to `@SpringBootApplication`.

- [ ] **Step 4: Create the domain records, port, and exception**

`SuggestionContext.java`:

```java
package com.brewdeck.brewdeck_api.ai;

public record SuggestionContext(
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
    String notes) {}
```

`SuggestedRecipe.java`:

```java
package com.brewdeck.brewdeck_api.ai;

import java.math.BigDecimal;

public record SuggestedRecipe(
    BigDecimal coffeeGrams,
    BigDecimal waterGrams,
    String ratio,
    String grindSetting,
    Integer waterTemp,
    String brewTime,
    String steps,
    String rationale) {}
```

`RecipeSuggestionPort.java`:

```java
package com.brewdeck.brewdeck_api.ai;

public interface RecipeSuggestionPort {
  SuggestedRecipe suggest(SuggestionContext context);
}
```

`AiUnavailableException.java`:

```java
package com.brewdeck.brewdeck_api.ai;

public class AiUnavailableException extends RuntimeException {
  public AiUnavailableException(String message) {
    super(message);
  }

  public AiUnavailableException(String message, Throwable cause) {
    super(message, cause);
  }
}
```

- [ ] **Step 5: Create request/response DTOs**

`SuggestRecipeRequest.java`:

```java
package com.brewdeck.brewdeck_api.ai;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SuggestRecipeRequest(
    @NotNull(message = "Coffee id is required") Long coffeeId,
    @NotNull(message = "Brew method id is required") Long methodId,
    @Size(max = 500, message = "Notes must not exceed 500 characters") String notes) {}
```

`SuggestedRecipeResponse.java`:

```java
package com.brewdeck.brewdeck_api.ai;

import java.math.BigDecimal;

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

- [ ] **Step 6: Add the `503` exception handler**

In `GlobalExceptionHandler.java`, add an import `import com.brewdeck.brewdeck_api.ai.AiUnavailableException;` and a handler method (place it above `handleGenericException`):

```java
  @ExceptionHandler(AiUnavailableException.class)
  public ResponseEntity<ErrorResponse> handleAiUnavailable(
      AiUnavailableException exception, HttpServletRequest request) {
    ErrorResponse errorResponse =
        buildErrorResponse(
            HttpStatus.SERVICE_UNAVAILABLE,
            "AI suggestion service is unavailable",
            sanitize(request.getRequestURI()),
            null);

    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(errorResponse);
  }
```

- [ ] **Step 7: Write the failing service test**

`src/test/java/com/brewdeck/brewdeck_api/ai/RecipeSuggestionServiceTest.java`:

```java
package com.brewdeck.brewdeck_api.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.brewdeck.brewdeck_api.coffee.Coffee;
import com.brewdeck.brewdeck_api.coffee.CoffeeRepository;
import com.brewdeck.brewdeck_api.method.BrewMethod;
import com.brewdeck.brewdeck_api.method.BrewMethodRepository;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RecipeSuggestionServiceTest {

  @Mock private CoffeeRepository coffeeRepository;
  @Mock private BrewMethodRepository brewMethodRepository;
  @Mock private RecipeSuggestionPort port;

  private RecipeSuggestionService serviceEnabled() {
    return new RecipeSuggestionService(
        coffeeRepository,
        brewMethodRepository,
        port,
        new AiProperties(true, "claude-haiku-4-5", 20, 1024));
  }

  @Test
  void suggest_shouldThrowAiUnavailable_whenDisabled() {
    RecipeSuggestionService service =
        new RecipeSuggestionService(
            coffeeRepository,
            brewMethodRepository,
            port,
            new AiProperties(false, "claude-haiku-4-5", 20, 1024));

    assertThatThrownBy(() -> service.suggest(new SuggestRecipeRequest(1L, 2L, null)))
        .isInstanceOf(AiUnavailableException.class);
    verify(port, never()).suggest(any());
  }

  @Test
  void suggest_shouldThrowNotFound_whenCoffeeMissing() {
    when(coffeeRepository.findById(1L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> serviceEnabled().suggest(new SuggestRecipeRequest(1L, 2L, null)))
        .isInstanceOf(EntityNotFoundException.class);
  }

  @Test
  void suggest_shouldReturnMappedResponse_whenEnabled() {
    Coffee coffee = Coffee.builder().id(1L).name("Mezcla Veracruz").roastLevel("Medio").build();
    BrewMethod method = BrewMethod.builder().id(2L).name("AeroPress").build();
    when(coffeeRepository.findById(1L)).thenReturn(Optional.of(coffee));
    when(brewMethodRepository.findById(2L)).thenReturn(Optional.of(method));
    when(port.suggest(any()))
        .thenReturn(
            new SuggestedRecipe(
                new BigDecimal("15"),
                new BigDecimal("240"),
                "1:16",
                "Medium-fine",
                92,
                "2:30",
                "Bloom then pour.",
                "Balanced extraction for a medium roast."));

    SuggestedRecipeResponse result =
        serviceEnabled().suggest(new SuggestRecipeRequest(1L, 2L, "fruity please"));

    assertThat(result.coffeeGrams()).isEqualByComparingTo("15");
    assertThat(result.waterTemp()).isEqualTo(92);
    assertThat(result.rationale()).isEqualTo("Balanced extraction for a medium roast.");
  }
}
```

- [ ] **Step 8: Run the service test to verify it fails**

Run: `sh mvnw -Dtest=RecipeSuggestionServiceTest test`
Expected: FAIL — `RecipeSuggestionService` does not exist yet (compilation error).

- [ ] **Step 9: Implement `RecipeSuggestionService`**

`src/main/java/com/brewdeck/brewdeck_api/ai/RecipeSuggestionService.java`:

```java
package com.brewdeck.brewdeck_api.ai;

import com.brewdeck.brewdeck_api.coffee.Coffee;
import com.brewdeck.brewdeck_api.coffee.CoffeeRepository;
import com.brewdeck.brewdeck_api.method.BrewMethod;
import com.brewdeck.brewdeck_api.method.BrewMethodRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class RecipeSuggestionService {

  private static final String COFFEE_NOT_FOUND = "Coffee not found";
  private static final String BREW_METHOD_NOT_FOUND = "Brew method not found";

  private final CoffeeRepository coffeeRepository;
  private final BrewMethodRepository brewMethodRepository;
  private final RecipeSuggestionPort suggestionPort;
  private final AiProperties aiProperties;

  public SuggestedRecipeResponse suggest(SuggestRecipeRequest request) {
    if (!aiProperties.enabled()) {
      throw new AiUnavailableException("AI suggestions are disabled");
    }

    Coffee coffee =
        coffeeRepository
            .findById(request.coffeeId())
            .orElseThrow(() -> new EntityNotFoundException(COFFEE_NOT_FOUND));
    BrewMethod method =
        brewMethodRepository
            .findById(request.methodId())
            .orElseThrow(() -> new EntityNotFoundException(BREW_METHOD_NOT_FOUND));

    SuggestionContext context =
        new SuggestionContext(
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
            request.notes());

    SuggestedRecipe suggested = suggestionPort.suggest(context);
    log.info(
        "Generated recipe suggestion coffeeId={} methodId={} model={}",
        request.coffeeId(),
        request.methodId(),
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

- [ ] **Step 10: Run the service test to verify it passes**

Run: `sh mvnw -Dtest=RecipeSuggestionServiceTest test`
Expected: PASS (3 tests). (`RecipeSuggestionPort` has no bean yet — that is fine; the test injects a mock. The application context is not loaded here.)

- [ ] **Step 11: Write the failing controller test**

`src/test/java/com/brewdeck/brewdeck_api/ai/RecipeSuggestionControllerTest.java`:

```java
package com.brewdeck.brewdeck_api.ai;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(RecipeSuggestionController.class)
class RecipeSuggestionControllerTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @MockitoBean private RecipeSuggestionService service;

  @Test
  void suggest_shouldReturnSuggestion() throws Exception {
    when(service.suggest(any()))
        .thenReturn(
            new SuggestedRecipeResponse(
                new BigDecimal("15"),
                new BigDecimal("240"),
                "1:16",
                "Medium-fine",
                92,
                "2:30",
                "Bloom then pour.",
                "Balanced for a medium roast."));

    mockMvc
        .perform(
            post("/api/recipes/suggest")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"coffeeId\":1,\"methodId\":2}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.waterTemp").value(92))
        .andExpect(jsonPath("$.ratio").value("1:16"))
        .andExpect(jsonPath("$.rationale").value("Balanced for a medium roast."));
  }

  @Test
  void suggest_shouldReturnBadRequest_whenIdsMissing() throws Exception {
    mockMvc
        .perform(
            post("/api/recipes/suggest").contentType(MediaType.APPLICATION_JSON).content("{}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.validationErrors.coffeeId").value("Coffee id is required"))
        .andExpect(jsonPath("$.validationErrors.methodId").value("Brew method id is required"));
  }

  @Test
  void suggest_shouldReturnServiceUnavailable_whenAiUnavailable() throws Exception {
    when(service.suggest(any())).thenThrow(new AiUnavailableException("disabled"));

    mockMvc
        .perform(
            post("/api/recipes/suggest")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"coffeeId\":1,\"methodId\":2}"))
        .andExpect(status().isServiceUnavailable())
        .andExpect(jsonPath("$.status").value(503));
  }

  @Test
  void suggest_shouldReturnNotFound_whenEntityMissing() throws Exception {
    when(service.suggest(any())).thenThrow(new EntityNotFoundException("Coffee not found"));

    mockMvc
        .perform(
            post("/api/recipes/suggest")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"coffeeId\":9,\"methodId\":2}"))
        .andExpect(status().isNotFound());
  }
}
```

- [ ] **Step 12: Run the controller test to verify it fails**

Run: `sh mvnw -Dtest=RecipeSuggestionControllerTest test`
Expected: FAIL — `RecipeSuggestionController` does not exist (compilation error).

- [ ] **Step 13: Implement `RecipeSuggestionController`**

`src/main/java/com/brewdeck/brewdeck_api/ai/RecipeSuggestionController.java`:

```java
package com.brewdeck.brewdeck_api.ai;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/recipes")
@RequiredArgsConstructor
@Tag(name = "AI Recipe Suggestions", description = "AI-generated brewing parameters")
public class RecipeSuggestionController {

  private final RecipeSuggestionService recipeSuggestionService;

  @PostMapping("/suggest")
  @Operation(
      summary = "Suggest brewing parameters",
      description =
          "Generates AI brewing parameters for a coffee and brew method. The result is not"
              + " persisted; the client uses it to pre-fill the recipe form.")
  public ResponseEntity<SuggestedRecipeResponse> suggest(
      @Valid @RequestBody SuggestRecipeRequest request) {
    return ResponseEntity.ok(recipeSuggestionService.suggest(request));
  }
}
```

- [ ] **Step 14: Run the controller test to verify it passes**

Run: `sh mvnw -Dtest=RecipeSuggestionControllerTest test`
Expected: PASS (4 tests).

- [ ] **Step 15: Write the failing adapter mapping test**

The adapter's SDK call is not unit-tested (no network, no key). Its **mapping** from the structured payload to the domain record is. `src/test/java/com/brewdeck/brewdeck_api/ai/ClaudeRecipeSuggestionAdapterTest.java`:

```java
package com.brewdeck.brewdeck_api.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class ClaudeRecipeSuggestionAdapterTest {

  @Test
  void toSuggestedRecipe_shouldMapPayload() {
    ClaudeRecipeSuggestionAdapter.SuggestedRecipePayload payload =
        new ClaudeRecipeSuggestionAdapter.SuggestedRecipePayload(
            new BigDecimal("15"), new BigDecimal("240"), "1:16", "Medium-fine", 92, "2:30",
            "Bloom then pour.", "Balanced for a medium roast.");

    SuggestedRecipe result = ClaudeRecipeSuggestionAdapter.toSuggestedRecipe(payload);

    assertThat(result.coffeeGrams()).isEqualByComparingTo("15");
    assertThat(result.waterTemp()).isEqualTo(92);
    assertThat(result.rationale()).isEqualTo("Balanced for a medium roast.");
  }

  @Test
  void toSuggestedRecipe_shouldThrowAiUnavailable_whenPayloadNull() {
    assertThatThrownBy(() -> ClaudeRecipeSuggestionAdapter.toSuggestedRecipe(null))
        .isInstanceOf(AiUnavailableException.class);
  }
}
```

- [ ] **Step 16: Run the adapter test to verify it fails**

Run: `sh mvnw -Dtest=ClaudeRecipeSuggestionAdapterTest test`
Expected: FAIL — `ClaudeRecipeSuggestionAdapter` does not exist (compilation error).

- [ ] **Step 17: Implement `ClaudeRecipeSuggestionAdapter`**

Uses the Anthropic Java SDK's **class-based structured output** (`.outputConfig(SuggestedRecipePayload.class)`) so the reply is a typed POJO — no manual JSON parsing. The static `toSuggestedRecipe` mapping is package-private and directly tested; the instance `suggest` builds params, calls the client, and wraps all failures.

> **SDK note:** the exact builder symbols come from the `claude-api` skill's Java reference (`MessageCreateParams.builder()`, `.model(String)`, `.maxTokens(long)`, `.system(String)`, `.addUserMessage(String)`, `.outputConfig(Class)`, `AnthropicOkHttpClient.builder()`). If a method name differs in the pinned SDK version, fix from the compiler error — do not guess a different API shape. Keep `toSuggestedRecipe` exactly as written (the test depends on it).

`src/main/java/com/brewdeck/brewdeck_api/ai/ClaudeRecipeSuggestionAdapter.java`:

```java
package com.brewdeck.brewdeck_api.ai;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.StructuredMessageCreateParams;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import java.math.BigDecimal;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@ConditionalOnProperty(prefix = "brewdeck.ai", name = "enabled", havingValue = "true")
public class ClaudeRecipeSuggestionAdapter implements RecipeSuggestionPort {

  private static final String SYSTEM_PROMPT =
      "You are an expert barista. Given a coffee and a brew method, return brewing parameters"
          + " as structured data only. Water temperature is in degrees Celsius, between 70 and 100."
          + " Keep steps and rationale concise.";

  private final AnthropicClient client;
  private final AiProperties properties;

  public ClaudeRecipeSuggestionAdapter(AiProperties properties) {
    this.properties = properties;
    this.client =
        AnthropicOkHttpClient.builder()
            .apiKey(System.getenv("ANTHROPIC_API_KEY"))
            .timeout(Duration.ofSeconds(properties.timeoutSeconds()))
            .build();
  }

  @Override
  public SuggestedRecipe suggest(SuggestionContext context) {
    try {
      StructuredMessageCreateParams<SuggestedRecipePayload> params =
          MessageCreateParams.builder()
              .model(properties.model())
              .maxTokens((long) properties.maxTokens())
              .system(SYSTEM_PROMPT)
              .addUserMessage(buildUserMessage(context))
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
      log.warn("AI suggestion call failed: {}", exception.getClass().getSimpleName());
      throw new AiUnavailableException("AI suggestion call failed", exception);
    }
  }

  static SuggestedRecipe toSuggestedRecipe(SuggestedRecipePayload payload) {
    if (payload == null) {
      throw new AiUnavailableException("Missing AI payload");
    }
    return new SuggestedRecipe(
        payload.coffeeGrams(),
        payload.waterGrams(),
        payload.ratio(),
        payload.grindSetting(),
        payload.waterTemp(),
        payload.brewTime(),
        payload.steps(),
        payload.rationale());
  }

  private String buildUserMessage(SuggestionContext c) {
    return "Coffee: " + c.coffeeName()
        + "\nOrigin: " + orDash(c.origin())
        + "\nRoast: " + orDash(c.roastLevel())
        + "\nProcess: " + orDash(c.process())
        + "\nTasting scores (1-5): acidity=" + orDash(c.acidityScore())
        + ", body=" + orDash(c.bodyScore())
        + ", sweetness=" + orDash(c.sweetnessScore())
        + ", bitterness=" + orDash(c.bitternessScore())
        + "\nBrew method: " + c.methodName()
        + " (" + orDash(c.methodDescription()) + ")"
        + "\nUser notes: " + orDash(c.notes());
  }

  private String orDash(Object value) {
    return value == null || String.valueOf(value).isBlank() ? "n/a" : String.valueOf(value);
  }

  public record SuggestedRecipePayload(
      @JsonPropertyDescription("Coffee dose in grams") BigDecimal coffeeGrams,
      @JsonPropertyDescription("Water in grams") BigDecimal waterGrams,
      @JsonPropertyDescription("Brew ratio, e.g. 1:16") String ratio,
      @JsonPropertyDescription("Grind setting description") String grindSetting,
      @JsonPropertyDescription("Water temperature in degrees Celsius, 70 to 100")
          Integer waterTemp,
      @JsonPropertyDescription("Total brew time, e.g. 2:30") String brewTime,
      @JsonPropertyDescription("Concise brewing steps") String steps,
      @JsonPropertyDescription("One-sentence rationale for these parameters") String rationale) {}
}
```

- [ ] **Step 18: Run the adapter test to verify it passes**

Run: `sh mvnw -Dtest=ClaudeRecipeSuggestionAdapterTest test`
Expected: PASS (2 tests). If the SDK builder method names differ, fix the compiler errors in `suggest(...)` only — the mapping method and test stay unchanged.

- [ ] **Step 19: Exclude the adapter from Sonar coverage**

The adapter's SDK-calling path is not unit-tested, so it would drag new-code coverage below the 80% gate (same trap as the DTO records — see `.claude/napkin.md`). In `pom.xml`, extend `sonar.coverage.exclusions` to include the adapter:

```xml
<sonar.coverage.exclusions>
  **/*Application.java,**/*Request.java,**/*Response.java,**/config/**,**/ai/ClaudeRecipeSuggestionAdapter.java
</sonar.coverage.exclusions>
```

- [ ] **Step 20: Format**

Run: `sh mvnw spotless:apply`
Expected: files reformatted, no errors.

- [ ] **Step 21: Full backend build**

Run: `sh mvnw clean verify`
Expected: BUILD SUCCESS. The context loads with `brewdeck.ai.enabled=false` (test profile), so the adapter bean is not created and no key is needed. All AI tests plus the existing suite pass.

- [ ] **Step 22: Commit**

```bash
git add brewdeck-api/pom.xml \
        brewdeck-api/src/main/resources/application.yaml \
        brewdeck-api/src/test/resources/application-test.yml \
        brewdeck-api/src/main/java/com/brewdeck/brewdeck_api/ai/ \
        brewdeck-api/src/test/java/com/brewdeck/brewdeck_api/ai/ \
        brewdeck-api/src/main/java/com/brewdeck/brewdeck_api/common/error/GlobalExceptionHandler.java \
        .env.example
git commit -m "feat(api): add Claude recipe-suggestion port, adapter and endpoint"
```

(If `@ConfigurationPropertiesScan` was added to `BrewdeckApiApplication.java` in Step 3, include it in the `git add`.)

---

### Task 2: Frontend — AI suggest button on the recipe form

**Files:**
- Create: `src/lib/api/ai.ts`, `src/hooks/useSuggestRecipe.ts`.
- Modify: `src/components/recipes/RecipeFormDialog.tsx`, `src/components/recipes/RecipeFormDialog.test.tsx`.

**Interfaces:**
- Consumes: Task 1's `POST /api/recipes/suggest` contract; `apiFetch` from `@/lib/api/client`; `useForm` `setValue`/`watch` already available in `RecipeFormDialog`.
- Produces:
  - `SuggestRecipeInput { coffeeId: number; methodId: number; notes?: string }`.
  - `SuggestedRecipe { coffeeGrams: number | null; waterGrams: number | null; ratio: string | null; grindSetting: string | null; waterTemp: number | null; brewTime: string | null; steps: string | null; rationale: string }`.
  - `suggestRecipe(body: SuggestRecipeInput): Promise<SuggestedRecipe>`.
  - `useSuggestRecipe()` — TanStack `useMutation` returning the above.

- [ ] **Step 1: Create the API module**

`src/lib/api/ai.ts`:

```ts
import { apiFetch } from './client';

export type SuggestRecipeInput = {
  coffeeId: number;
  methodId: number;
  notes?: string;
};

export type SuggestedRecipe = {
  coffeeGrams: number | null;
  waterGrams: number | null;
  ratio: string | null;
  grindSetting: string | null;
  waterTemp: number | null;
  brewTime: string | null;
  steps: string | null;
  rationale: string;
};

export function suggestRecipe(body: SuggestRecipeInput): Promise<SuggestedRecipe> {
  return apiFetch<SuggestedRecipe>('/api/recipes/suggest', {
    method: 'POST',
    body: JSON.stringify(body),
  });
}
```

- [ ] **Step 2: Create the mutation hook**

`src/hooks/useSuggestRecipe.ts`:

```ts
'use client';

import { useMutation } from '@tanstack/react-query';
import { suggestRecipe, type SuggestRecipeInput } from '@/lib/api/ai';

export function useSuggestRecipe() {
  return useMutation({
    mutationFn: (body: SuggestRecipeInput) => suggestRecipe(body),
  });
}
```

- [ ] **Step 3: Write the failing form test**

In `src/components/recipes/RecipeFormDialog.test.tsx`, mock the new hook and add tests. First add these to the existing imports/mocks (the file already mocks `@/hooks/useRecipeMutations` and the resource-options hooks — mirror that pattern):

```tsx
import * as suggest from '@/hooks/useSuggestRecipe';

const suggestMutate = vi.fn();

function mockSuggest(overrides: Record<string, unknown> = {}) {
  vi.spyOn(suggest, 'useSuggestRecipe').mockReturnValue({
    mutate: suggestMutate,
    isPending: false,
    ...overrides,
  } as never);
}
```

Then add tests (call `mockSuggest()` alongside the existing hook mocks in each; if the file has a shared setup helper, add it there):

```tsx
it('disables the suggest button until coffee and method are selected', () => {
  mockSuggest();
  // ...existing mocks + render with no recipe (empty coffee/method)
  expect(screen.getByRole('button', { name: /suggest with ai/i })).toBeDisabled();
});

it('fills fields and shows rationale on a successful suggestion', async () => {
  mockSuggest();
  suggestMutate.mockImplementation((_body, opts) => {
    opts.onSuccess({
      coffeeGrams: 15,
      waterGrams: 240,
      ratio: '1:16',
      grindSetting: 'Medium-fine',
      waterTemp: 92,
      brewTime: '2:30',
      steps: 'Bloom then pour.',
      rationale: 'Balanced for a medium roast.',
    });
  });
  // ...render with a recipe fixture that has coffeeId + methodId set so the button is enabled
  fireEvent.click(screen.getByRole('button', { name: /suggest with ai/i }));
  expect(await screen.findByText(/balanced for a medium roast/i)).toBeInTheDocument();
  expect(screen.getByLabelText(/ratio/i)).toHaveValue('1:16');
});
```

> Match the file's existing render helper and query style. If tests render with `recipe={undefined}`, pass a fixture recipe (with `coffeeId`/`methodId`) for the enabled-button case so `watch` sees selected values.

- [ ] **Step 4: Run the form test to verify it fails**

Run: `npm run test -- src/components/recipes/RecipeFormDialog.test.tsx`
Expected: FAIL — no "Suggest with AI" button in the DOM.

- [ ] **Step 5: Wire the suggest button into the form**

In `src/components/recipes/RecipeFormDialog.tsx`:

Add imports:

```tsx
import { useSuggestRecipe } from '@/hooks/useSuggestRecipe';
```

Destructure `setValue` and `watch` from `useForm` (add to the existing destructure):

```tsx
  const {
    control,
    register,
    handleSubmit,
    setError,
    setValue,
    watch,
    formState: { errors },
  } = useForm<RecipeFormInput, unknown, RecipeFormValues>({
```

Add suggestion state + handler inside the component (after the existing `pending` line):

```tsx
  const suggestion = useSuggestRecipe();
  const [rationale, setRationale] = useState<string | null>(null);
  const [suggestError, setSuggestError] = useState<string | null>(null);

  const coffeeId = watch('coffeeId');
  const methodId = watch('methodId');
  const canSuggest = Boolean(coffeeId) && Boolean(methodId) && !suggestion.isPending;

  const onSuggest = () => {
    setRationale(null);
    setSuggestError(null);
    suggestion.mutate(
      { coffeeId: Number(coffeeId), methodId: Number(methodId) },
      {
        onSuccess: (data) => {
          const set = (name: keyof RecipeFormValues, value: string | number | null) => {
            if (value !== null && value !== undefined) {
              setValue(name, value as never, { shouldValidate: true });
            }
          };
          set('coffeeGrams', data.coffeeGrams);
          set('waterGrams', data.waterGrams);
          set('ratio', data.ratio);
          set('grindSetting', data.grindSetting);
          set('waterTemp', data.waterTemp);
          set('brewTime', data.brewTime);
          set('steps', data.steps);
          setRationale(data.rationale);
        },
        onError: () =>
          setSuggestError('AI suggestions are unavailable right now. Please try again later.'),
      },
    );
  };
```

Inside `<Stack spacing={2}>`, after the `methodId` `Controller` block and before the `TEXT_FIELDS.map(...)`, add the button + feedback:

```tsx
            <Button
              variant="outlined"
              onClick={onSuggest}
              disabled={!canSuggest}
              startIcon={suggestion.isPending ? <CircularProgress size={16} /> : undefined}
            >
              Suggest with AI
            </Button>
            {suggestError ? <Alert severity="error">{suggestError}</Alert> : null}
            {rationale ? <Alert severity="info">{rationale}</Alert> : null}
```

(`Button`, `Alert`, `CircularProgress`, `useState` are already imported in this file.)

- [ ] **Step 6: Run the form test to verify it passes**

Run: `npm run test -- src/components/recipes/RecipeFormDialog.test.tsx`
Expected: PASS — button present, disabled when unselected, fills fields and shows rationale on success.

- [ ] **Step 7: Type-check, lint, build**

Run: `npm run type-check`
Expected: no errors.
Run: `npm run lint:fix -- src/lib/api/ai.ts src/hooks/useSuggestRecipe.ts src/components/recipes/RecipeFormDialog.tsx src/components/recipes/RecipeFormDialog.test.tsx`
Expected: clean.
Run: `npm run build`
Expected: build succeeds.

- [ ] **Step 8: Commit**

```bash
git add brewdeck-web/src/lib/api/ai.ts \
        brewdeck-web/src/hooks/useSuggestRecipe.ts \
        brewdeck-web/src/components/recipes/RecipeFormDialog.tsx \
        brewdeck-web/src/components/recipes/RecipeFormDialog.test.tsx
git commit -m "feat(web): add AI suggest-recipe button to the recipe form"
```

---

### Task 3: Docs — Postman, roadmap, project-state

**Files:**
- Modify: `docs/postman/brewdeck.postman_collection.json`, `.claude/roadmap.md`, `.claude/project-state.md`.

- [ ] **Step 1: Add a Postman request**

In `docs/postman/brewdeck.postman_collection.json`, add a `POST {{baseUrl}}/api/recipes/suggest` request (in the recipes folder) with a JSON body:

```json
{
  "coffeeId": {{coffeeId}},
  "methodId": {{methodId}},
  "notes": "fruity and bright"
}
```

Keep valid JSON; use existing Long ID vars (`{{coffeeId}}`, `{{methodId}}`), base URL from the environment. Validate the file parses (`python3 -m json.tool <file> > /dev/null`).

- [ ] **Step 2: Update the roadmap**

In `.claude/roadmap.md`, change the Phase 5 line:

```
- AI-assisted recipe suggestions — Not Started
```

to:

```
- AI-assisted recipe suggestions (generate brew params; Claude via Java SDK) — In Progress (generate slice)
```

- [ ] **Step 3: Update project state**

In `.claude/project-state.md`: this branch already carries the pending edit marking PR #57 (tasting radar) merged — keep it. Add to "Recently Worked On":

```
- AI recipe suggestions (generate slice): POST /api/recipes/suggest, Claude Java SDK behind a RecipeSuggestionPort with a claude-haiku-4-5 adapter (structured outputs), feature-toggled, and a "Suggest with AI" button pre-filling the recipe form
```

Update the "Immediate Next Steps" to note the remaining AI flow (improve an existing recipe from history) and the other Phase 5 items (PDF export, share links).

- [ ] **Step 4: Commit**

```bash
git add docs/postman/brewdeck.postman_collection.json .claude/roadmap.md .claude/project-state.md
git commit -m "docs(project): record AI recipe suggestions (generate slice)"
```

---

## Self-Review

**Spec coverage:** provider/model in config → Task 1 Steps 2–3, 17. Port + adapter split → Task 1 Steps 4, 17. Structured outputs → Task 1 Step 17. Ephemeral (no persist) → service returns DTO, no repository write → Task 1 Step 9; frontend pre-fill → Task 2 Step 5. Endpoint returns DTO not PageResponse → Task 1 Step 13. Validation 400 / 404 / 503 paths → Task 1 Steps 6, 11, 13. Secret + toggle → Task 1 Steps 2, 17. CI-without-key → test profile `enabled=false`, port mocked, adapter mapping unit-tested → Task 1 Steps 2, 7, 15, 21. Frontend button/hook/tests → Task 2. Postman + roadmap + state → Task 3. Sonar coverage guardrail → Task 1 Step 19. ✓

**Placeholder scan:** only deferred value is the `anthropic-java` version (pin at implementation) and the "match existing test render helper" note in Task 2 Step 3 (the file's harness varies; a concrete fallback fixture is given). Every code step shows code. ✓

**Type consistency:** `SuggestedRecipe` / `SuggestedRecipeResponse` / `SuggestedRecipePayload` share the same 8 fields in the same order across service, controller, adapter, and tests. `SuggestRecipeRequest(coffeeId, methodId, notes)` matches the frontend `SuggestRecipeInput`. `toSuggestedRecipe` named identically in the adapter (Step 17) and its test (Step 15). Frontend `SuggestedRecipe` fields match the backend response JSON. ✓

**Scope:** one endpoint, one adapter, one frontend button, one docs pass. Generate slice only; "improve existing recipe" explicitly deferred. Right-sized for one plan. ✓
