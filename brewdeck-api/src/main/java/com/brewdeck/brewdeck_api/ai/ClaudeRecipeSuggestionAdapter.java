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

  private static final String IMPROVE_SYSTEM_PROMPT =
      "You are an expert barista tuning an existing coffee recipe using its brew history."
          + " Given the current parameters and recent rated brews, return improved brewing"
          + " parameters as structured data only. Water temperature is in degrees Celsius,"
          + " between 70 and 100. Keep steps concise, and use the rationale to explain what you"
          + " changed and why.";

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
    return "Coffee: "
        + c.coffeeName()
        + "\nOrigin: "
        + orDash(c.origin())
        + "\nRoast: "
        + orDash(c.roastLevel())
        + "\nProcess: "
        + orDash(c.process())
        + "\nTasting scores (1-5): acidity="
        + orDash(c.acidityScore())
        + ", body="
        + orDash(c.bodyScore())
        + ", sweetness="
        + orDash(c.sweetnessScore())
        + ", bitterness="
        + orDash(c.bitternessScore())
        + "\nBrew method: "
        + c.methodName()
        + " ("
        + orDash(c.methodDescription())
        + ")"
        + "\nUser notes: "
        + orDash(c.notes());
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

  private String orDash(Object value) {
    return value == null || String.valueOf(value).isBlank() ? "n/a" : String.valueOf(value);
  }

  public record SuggestedRecipePayload(
      @JsonPropertyDescription("Coffee dose in grams") BigDecimal coffeeGrams,
      @JsonPropertyDescription("Water in grams") BigDecimal waterGrams,
      @JsonPropertyDescription("Brew ratio, e.g. 1:16") String ratio,
      @JsonPropertyDescription("Grind setting description") String grindSetting,
      @JsonPropertyDescription("Water temperature in degrees Celsius, 70 to 100") Integer waterTemp,
      @JsonPropertyDescription("Total brew time, e.g. 2:30") String brewTime,
      @JsonPropertyDescription("Concise brewing steps") String steps,
      @JsonPropertyDescription("One-sentence rationale for these parameters") String rationale) {}
}
