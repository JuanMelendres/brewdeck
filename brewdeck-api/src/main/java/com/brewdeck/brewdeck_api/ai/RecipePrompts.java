package com.brewdeck.brewdeck_api.ai;

import java.util.Locale;

/**
 * Prompts shared by every {@link RecipeSuggestionPort} adapter, so providers can be compared on
 * equal terms (docs/product/spikes/ai-local-llm-spike.md). Bump {@link #VERSION} when the wording
 * changes; it is logged with each suggestion.
 */
final class RecipePrompts {

  static final String VERSION = "2026-10-09";

  static final String SUGGEST_SYSTEM =
      "You are an expert barista. Given a coffee and a brew method, return brewing parameters"
          + " as structured data only. Water temperature is in degrees Celsius, between 0 and 100:"
          + " hot methods usually use 85 to 96, cold brew uses cold water."
          + " Keep steps and rationale concise.";

  static final String IMPROVE_SYSTEM =
      "You are an expert barista tuning an existing coffee recipe using its brew history."
          + " Given the current parameters and recent rated brews, return improved brewing"
          + " parameters as structured data only. Water temperature is in degrees Celsius,"
          + " between 0 and 100 (cold brew uses cold water). Keep steps concise, and use the"
          + " rationale to explain what you changed and why.";

  private RecipePrompts() {}

  static String suggestSystem(Locale locale) {
    return SUGGEST_SYSTEM + languageInstruction(locale);
  }

  static String improveSystem(Locale locale) {
    return IMPROVE_SYSTEM + languageInstruction(locale);
  }

  /**
   * Every text field follows the user's language (ADR-015); numbers stay language-neutral. Asking
   * only for steps and rationale let English leak into the grind setting and brew time.
   */
  static String languageInstruction(Locale locale) {
    String language = "es".equals(locale.getLanguage()) ? "Spanish" : "English";
    return " Write every text field (grind setting, brew time, steps, rationale) in "
        + language
        + ".";
  }

  /** The method's usual ratio, temperature, and grind, so the model starts from sensible values. */
  private static String methodHint(String methodName) {
    return BrewMethodProfile.forMethod(methodName).map(p -> "\n" + p.promptHint()).orElse("");
  }

  static String suggestMessage(SuggestionContext c) {
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
        + methodHint(c.methodName())
        + "\nUser notes: "
        + orDash(c.notes());
  }

  static String improveMessage(ImprovementContext c) {
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
        .append(methodHint(c.methodName()))
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

  private static String orDash(Object value) {
    return value == null || String.valueOf(value).isBlank() ? "n/a" : String.valueOf(value);
  }
}
