package com.brewdeck.brewdeck_api.ai;

import com.brewdeck.brewdeck_api.common.i18n.RequestLocale;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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

  // Sanity bounds for a method without a profile (user-defined names).
  private static final double MIN_DOSE = 5;
  private static final double MAX_DOSE = 250;

  // Limits of RecipeRequest, so an accepted suggestion always saves.
  private static final int MAX_GRIND = 120;
  private static final int MAX_BREW_TIME = 20;
  private static final int MAX_STEPS = 1000;

  private final MessageSource messageSource;

  public SuggestedRecipe apply(String methodName, SuggestedRecipe answer) {
    BrewMethodProfile range = BrewMethodProfile.forMethod(methodName).orElse(null);

    BigDecimal water = answer.waterGrams();
    String ratio = answer.ratio();
    Integer temp = answer.waterTemp();
    boolean adjusted = false;

    BigDecimal coffee = answer.coffeeGrams();
    if (coffee != null && coffee.signum() > 0 && water != null && water.signum() > 0) {
      double actual = water.doubleValue() / coffee.doubleValue();
      double target = range == null ? actual : clamp(actual, range.minRatio(), range.maxRatio());
      // A sensible ratio can still hide an absurd dose (seen: 173 g of coffee for one V60).
      double dose =
          clamp(
              coffee.doubleValue(),
              range == null ? MIN_DOSE : range.minDose(),
              range == null ? MAX_DOSE : range.maxDose());
      if (dose != coffee.doubleValue()) {
        coffee = BigDecimal.valueOf(dose).setScale(1, RoundingMode.HALF_UP);
        adjusted = true;
      }
      if (target != actual || adjusted) {
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
    String steps =
        fit(
            syncSteps(
                answer.steps(),
                answer.coffeeGrams(),
                coffee,
                answer.waterGrams(),
                water,
                answer.waterTemp(),
                temp),
            MAX_STEPS);

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

  /**
   * The model writes amounts into the steps too ("pour 225 g"). When the water or temperature was
   * adjusted, the same numbers in the steps are replaced, so the steps match the recipe fields.
   */
  static String syncSteps(
      String steps,
      BigDecimal oldCoffee,
      BigDecimal newCoffee,
      BigDecimal oldWater,
      BigDecimal newWater,
      Integer oldTemp,
      Integer newTemp) {
    if (steps == null) {
      return null;
    }
    String result = replaceGrams(steps, oldWater, newWater);
    result = replaceGrams(result, oldCoffee, newCoffee);
    if (oldTemp != null && newTemp != null && !oldTemp.equals(newTemp)) {
      result =
          result.replaceAll(
              "(?<![\\d.,])" + oldTemp + "(?=\\s?(?:°|º|grados|degrees))", String.valueOf(newTemp));
    }
    return result;
  }

  private static String plain(BigDecimal value) {
    return value.stripTrailingZeros().toPlainString();
  }

  private static String replaceGrams(String text, BigDecimal from, BigDecimal to) {
    if (from == null || to == null || from.compareTo(to) == 0) {
      return text;
    }
    return text.replaceAll(
        "(?<![\\d.,])"
            + Pattern.quote(plain(from))
            + "(?:[.,]0+)?(?=\\s?(?:g\\b|gr\\b|gramos|grams|ml\\b))",
        Matcher.quoteReplacement(plain(to)));
  }
}
