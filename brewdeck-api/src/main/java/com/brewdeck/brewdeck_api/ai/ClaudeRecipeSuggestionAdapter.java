package com.brewdeck.brewdeck_api.ai;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.StructuredMessageCreateParams;
import com.brewdeck.brewdeck_api.common.i18n.RequestLocale;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import java.math.BigDecimal;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@ConditionalOnExpression(
    "${brewdeck.ai.enabled:false} and '${brewdeck.ai.provider:claude}' == 'claude'")
public class ClaudeRecipeSuggestionAdapter implements RecipeSuggestionPort {

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
              .system(RecipePrompts.suggestSystem(RequestLocale.current()))
              .addUserMessage(RecipePrompts.suggestMessage(context))
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
      log.warn("AI suggestion call failed", exception);
      throw new AiUnavailableException("AI suggestion call failed", exception);
    }
  }

  @Override
  public SuggestedRecipe improve(ImprovementContext context) {
    try {
      StructuredMessageCreateParams<SuggestedRecipePayload> params =
          MessageCreateParams.builder()
              .model(properties.model())
              .maxTokens((long) properties.maxTokens())
              .system(RecipePrompts.improveSystem(RequestLocale.current()))
              .addUserMessage(RecipePrompts.improveMessage(context))
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

  public record SuggestedRecipePayload(
      @JsonPropertyDescription("Coffee dose in grams") BigDecimal coffeeGrams,
      @JsonPropertyDescription("Water in grams") BigDecimal waterGrams,
      @JsonPropertyDescription("Brew ratio, e.g. 1:16") String ratio,
      @JsonPropertyDescription("Grind setting description") String grindSetting,
      @JsonPropertyDescription("Water temperature in degrees Celsius, 0 to 100") Integer waterTemp,
      @JsonPropertyDescription("Total brew time, e.g. 2:30") String brewTime,
      @JsonPropertyDescription("Concise brewing steps") String steps,
      @JsonPropertyDescription("One-sentence rationale for these parameters") String rationale) {}
}
