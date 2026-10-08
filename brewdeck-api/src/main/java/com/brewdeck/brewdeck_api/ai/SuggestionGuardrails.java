package com.brewdeck.brewdeck_api.ai;

import com.brewdeck.brewdeck_api.common.i18n.RequestLocale;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

/**
 * Deterministic checks on AI answers. The local LLM spike showed 8B models get recipe ratios wrong
 * for some methods (docs/product/spikes/ai-local-llm-spike.md, Finding 2), so the water-to-coffee
 * ratio and the water temperature are kept inside the usual range for the brew method. A changed
 * answer says so in its rationale. Methods are user-defined, so they are recognized by name; an
 * unrecognized method only gets the 0-100 degrees Celsius bound.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class SuggestionGuardrails {

  /** Usual water/coffee ratio (by weight) and water temperature for a family of methods. */
  record MethodRange(
      List<String> nameHints, double minRatio, double maxRatio, int minTemp, int maxTemp) {
    boolean matches(String methodName) {
      return nameHints.stream().anyMatch(methodName::contains);
    }
  }

  // Order matters: "aeropress" must win over "press", "cold brew" over pour-over hints.
  static final List<MethodRange> RANGES =
      List.of(
          new MethodRange(List.of("espresso"), 1.5, 3.0, 88, 96),
          new MethodRange(List.of("cold"), 4, 16, 0, 25),
          new MethodRange(List.of("moka"), 6, 10, 80, 100),
          new MethodRange(List.of("aeropress"), 6, 17, 80, 96),
          new MethodRange(List.of("french", "prensa", "press"), 12, 17, 88, 96),
          new MethodRange(
              List.of("v60", "chemex", "kalita", "origami", "clever", "pour", "dripper", "filter"),
              13,
              18,
              85,
              96));

  // Limits of RecipeRequest, so an accepted suggestion always saves.
  private static final int MAX_GRIND = 120;
  private static final int MAX_BREW_TIME = 20;
  private static final int MAX_STEPS = 1000;

  private final MessageSource messageSource;

  public SuggestedRecipe apply(String methodName, SuggestedRecipe answer) {
    MethodRange range =
        RANGES.stream()
            .filter(r -> r.matches(methodName == null ? "" : methodName.toLowerCase(Locale.ROOT)))
            .findFirst()
            .orElse(null);

    BigDecimal water = answer.waterGrams();
    String ratio = answer.ratio();
    Integer temp = answer.waterTemp();
    boolean adjusted = false;

    BigDecimal coffee = answer.coffeeGrams();
    if (coffee != null && coffee.signum() > 0 && water != null && water.signum() > 0) {
      double actual = water.doubleValue() / coffee.doubleValue();
      double target = range == null ? actual : clamp(actual, range.minRatio(), range.maxRatio());
      if (target != actual) {
        water = coffee.multiply(BigDecimal.valueOf(target)).setScale(1, RoundingMode.HALF_UP);
        adjusted = true;
      }
      // The ratio text always matches the grams, in the form the recipe form accepts ("1:16.7").
      ratio =
          "1:"
              + BigDecimal.valueOf(target)
                  .setScale(1, RoundingMode.HALF_UP)
                  .stripTrailingZeros()
                  .toPlainString();
    }

    if (temp != null) {
      int min = range == null ? 0 : range.minTemp();
      int max = range == null ? 100 : range.maxTemp();
      int bounded = Math.clamp(temp, min, max);
      if (bounded != temp) {
        temp = bounded;
        adjusted = true;
      }
    }

    // Models ignore length limits; trim to what the recipe form accepts so the suggestion saves.
    String grind = fit(answer.grindSetting(), MAX_GRIND);
    String brewTime = fit(answer.brewTime(), MAX_BREW_TIME);
    String steps = fit(answer.steps(), MAX_STEPS);

    if (!adjusted
        && Objects.equals(ratio, answer.ratio())
        && Objects.equals(grind, answer.grindSetting())
        && Objects.equals(brewTime, answer.brewTime())
        && Objects.equals(steps, answer.steps())) {
      return answer;
    }
    log.info("Adjusted AI answer for method '{}' to its usual range", methodName);
    String rationale = answer.rationale();
    if (adjusted) {
      String note =
          messageSource.getMessage("ai.adjustedToMethodRange", null, RequestLocale.current());
      rationale = rationale == null || rationale.isBlank() ? note : rationale + " " + note;
    }
    return new SuggestedRecipe(coffee, water, ratio, grind, temp, brewTime, steps, rationale);
  }

  private static double clamp(double value, double min, double max) {
    return Math.max(min, Math.min(max, value));
  }

  private static String fit(String text, int max) {
    return text == null || text.length() <= max ? text : text.substring(0, max - 1).strip() + "…";
  }
}
