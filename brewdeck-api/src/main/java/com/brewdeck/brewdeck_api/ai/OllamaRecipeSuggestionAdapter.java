package com.brewdeck.brewdeck_api.ai;

import com.brewdeck.brewdeck_api.ai.ClaudeRecipeSuggestionAdapter.SuggestedRecipePayload;
import com.brewdeck.brewdeck_api.common.i18n.RequestLocale;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Suggests and improves recipes with a free, local open-weight model served by Ollama (ADR-016,
 * docs/product/spikes/ai-local-llm-spike.md). Calls Ollama's {@code /api/chat} directly: Spring AI
 * 2 would pull in Jackson 3, which BrewDeck does not use yet (ADR-008).
 *
 * <p>Local models are slow and can generate without end, so every call caps its output tokens and
 * treats a truncated answer as a failure. Numbers are checked afterwards by {@link
 * SuggestionGuardrails}.
 */
@Component
@Slf4j
@ConditionalOnExpression(
    "${brewdeck.ai.enabled:false} and '${brewdeck.ai.provider:claude}' == 'ollama'")
public class OllamaRecipeSuggestionAdapter implements RecipeSuggestionPort {

  /** The SuggestedRecipe fields as a JSON schema, so Ollama constrains the answer to it. */
  static final Map<String, Object> RESPONSE_SCHEMA =
      Map.of(
          "type",
          "object",
          "properties",
          Map.of(
              "coffeeGrams", Map.of("type", "number"),
              "waterGrams", Map.of("type", "number"),
              "ratio", Map.of("type", "string"),
              "grindSetting", Map.of("type", "string"),
              "waterTemp", Map.of("type", "integer"),
              "brewTime", Map.of("type", "string"),
              "steps", Map.of("type", "string"),
              "rationale", Map.of("type", "string")),
          "required",
          List.of(
              "coffeeGrams",
              "waterGrams",
              "ratio",
              "grindSetting",
              "waterTemp",
              "brewTime",
              "steps",
              "rationale"));

  private final RestClient client;
  private final ObjectMapper objectMapper;
  private final AiProperties.Ollama settings;

  @Autowired
  public OllamaRecipeSuggestionAdapter(AiProperties properties, ObjectMapper objectMapper) {
    this(
        properties,
        RestClient.builder().requestFactory(timeouts(properties.ollama())),
        objectMapper);
  }

  /** Tests pass a builder bound to a mock server. */
  OllamaRecipeSuggestionAdapter(
      AiProperties properties, RestClient.Builder builder, ObjectMapper objectMapper) {
    this.settings = properties.ollama();
    this.objectMapper = objectMapper;
    this.client = builder.baseUrl(settings.baseUrl()).build();
  }

  /** A slow local model must not hang a request forever (spike Finding 4). */
  private static SimpleClientHttpRequestFactory timeouts(AiProperties.Ollama settings) {
    SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
    requestFactory.setConnectTimeout(Duration.ofSeconds(5));
    requestFactory.setReadTimeout(Duration.ofSeconds(settings.timeoutSeconds()));
    return requestFactory;
  }

  @Override
  public SuggestedRecipe suggest(SuggestionContext context) {
    return chat(
        "suggestion",
        RecipePrompts.suggestSystem(RequestLocale.current()),
        RecipePrompts.suggestMessage(context));
  }

  @Override
  public SuggestedRecipe improve(ImprovementContext context) {
    return chat(
        "improvement",
        RecipePrompts.improveSystem(RequestLocale.current()),
        RecipePrompts.improveMessage(context));
  }

  private SuggestedRecipe chat(String kind, String system, String user) {
    String responseBody;
    try {
      responseBody =
          client
              .post()
              .uri("/api/chat")
              .contentType(MediaType.APPLICATION_JSON)
              .body(objectMapper.writeValueAsString(requestBody(system, user)))
              .retrieve()
              .body(String.class);
    } catch (JsonProcessingException | RestClientException exception) {
      log.warn("Ollama {} call failed: {}", kind, exception.toString());
      throw new AiUnavailableException("Local AI model is unavailable", exception);
    }
    return parse(kind, responseBody);
  }

  Map<String, Object> requestBody(String system, String user) {
    return Map.of(
        "model",
        settings.model(),
        "messages",
        List.of(
            Map.of("role", "system", "content", system), Map.of("role", "user", "content", user)),
        "format",
        RESPONSE_SCHEMA,
        "stream",
        false,
        // Reasoning traces (Qwen3) add latency and are not used; other models accept the flag.
        "think",
        false,
        "options",
        Map.of("temperature", 0.3, "num_predict", settings.maxTokens()));
  }

  SuggestedRecipe parse(String kind, String responseBody) {
    try {
      JsonNode response = objectMapper.readTree(responseBody == null ? "" : responseBody);
      if ("length".equals(response.path("done_reason").asText())) {
        log.warn("Ollama {} hit the {}-token cap", kind, settings.maxTokens());
        throw new AiUnavailableException("Local AI model did not finish its answer");
      }
      String content = response.path("message").path("content").asText("");
      if (content.isBlank()) {
        throw new AiUnavailableException("Empty answer from the local AI model");
      }
      return ClaudeRecipeSuggestionAdapter.toSuggestedRecipe(
          objectMapper.readValue(content, SuggestedRecipePayload.class));
    } catch (JsonProcessingException exception) {
      log.warn("Ollama {} answer was not valid JSON", kind);
      throw new AiUnavailableException("Local AI model returned an invalid answer", exception);
    }
  }
}
