package com.brewdeck.brewdeck_api.ai;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * What is usual for a family of brew methods: water-to-coffee ratio (by weight), water temperature,
 * coffee dose in grams, and grind. Used twice: as hints in the AI prompt, and as the bounds {@link
 * SuggestionGuardrails} enforce on the answer. Methods are user-defined, so they are recognized by
 * name.
 */
record BrewMethodProfile(
    List<String> nameHints,
    int minDose,
    int maxDose,
    double minRatio,
    double maxRatio,
    int minTemp,
    int maxTemp,
    String grind) {

  // Order matters: "aeropress" must win over "press", "chemex" over the other pour-overs.
  static final List<BrewMethodProfile> PROFILES =
      List.of(
          new BrewMethodProfile(
              List.of("espresso"),
              14,
              22,
              1.5,
              3.0,
              88,
              96,
              "fine, like powdered sugar to table salt"),
          new BrewMethodProfile(
              List.of("cold"),
              40,
              200,
              4,
              16,
              0,
              25,
              "coarse to extra-coarse, like coarse sea salt"),
          new BrewMethodProfile(
              List.of("moka"),
              10,
              25,
              6,
              10,
              80,
              100,
              "fine to medium, slightly coarser than espresso"),
          new BrewMethodProfile(
              List.of("aeropress"), 10, 25, 6, 17, 80, 96, "fine to medium, like table salt"),
          new BrewMethodProfile(
              List.of("french", "prensa", "press"),
              15,
              60,
              12,
              17,
              88,
              96,
              "coarse, like coarse sea salt"),
          new BrewMethodProfile(
              List.of("chemex"), 20, 60, 14, 17, 90, 96, "medium-coarse, like kosher salt"),
          new BrewMethodProfile(
              List.of("v60", "kalita", "origami", "clever", "pour", "dripper", "filter"),
              10,
              40,
              13,
              18,
              85,
              96,
              "medium-fine, like table salt"));

  static Optional<BrewMethodProfile> forMethod(String methodName) {
    String name = methodName == null ? "" : methodName.toLowerCase(Locale.ROOT);
    return PROFILES.stream()
        .filter(p -> p.nameHints().stream().anyMatch(name::contains))
        .findFirst();
  }

  /** One line for the prompt: dose, ratio, water temperature, and grind. */
  String promptHint() {
    return "Usual for this method: coffee dose "
        + minDose
        + "-"
        + maxDose
        + " g, water-to-coffee ratio 1:"
        + format(minRatio)
        + " to 1:"
        + format(maxRatio)
        + ", water "
        + minTemp
        + "-"
        + maxTemp
        + " degrees Celsius, grind "
        + grind
        + ".";
  }

  private static String format(double value) {
    return value == Math.rint(value) ? String.valueOf((long) value) : String.valueOf(value);
  }
}
