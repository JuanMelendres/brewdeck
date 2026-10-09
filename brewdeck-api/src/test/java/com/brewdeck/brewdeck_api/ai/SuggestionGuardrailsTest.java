package com.brewdeck.brewdeck_api.ai;

import static org.assertj.core.api.Assertions.assertThat;

import com.brewdeck.brewdeck_api.common.i18n.TestMessages;
import java.math.BigDecimal;
import java.util.Locale;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.i18n.LocaleContextHolder;

class SuggestionGuardrailsTest {

  private final SuggestionGuardrails guardrails =
      new SuggestionGuardrails(TestMessages.messageSource());

  @AfterEach
  void clearLocale() {
    LocaleContextHolder.resetLocaleContext();
  }

  private static SuggestedRecipe answer(double coffee, double water, String ratio, Integer temp) {
    return new SuggestedRecipe(
        BigDecimal.valueOf(coffee),
        BigDecimal.valueOf(water),
        ratio,
        "medium",
        temp,
        "2:30",
        "Pour.",
        "Bright cup.");
  }

  @Test
  void aV60RatioTooStrong_isMovedIntoThePourOverRange() {
    // Seen in the spike: qwen3:8b suggested 25 g / 225 g (1:9) for a V60.
    SuggestedRecipe fixed = guardrails.apply("V60", answer(25, 225, "1:9", 88));

    assertThat(fixed.waterGrams()).isEqualByComparingTo("325.0");
    assertThat(fixed.ratio()).isEqualTo("1:13");
    assertThat(fixed.rationale()).endsWith("Adjusted to the usual range for this brew method.");
  }

  @Test
  void aSensibleEspresso_isLeftUntouched() {
    SuggestedRecipe ok = answer(18, 45, "1:2.5", 92);

    assertThat(guardrails.apply("Espresso", ok)).isSameAs(ok);
  }

  @Test
  void coldBrew_keepsColdWater_butNotBoilingWater() {
    assertThat(guardrails.apply("Cold Brew", answer(100, 800, "1:8", 15)).waterTemp())
        .isEqualTo(15);
    assertThat(guardrails.apply("Cold Brew", answer(100, 800, "1:8", 93)).waterTemp())
        .isEqualTo(25);
  }

  @Test
  void aeropress_isNotMistakenForAFrenchPress() {
    // 1:10 is fine for AeroPress, but below the French press range.
    SuggestedRecipe aeropress = answer(15, 150, "1:10", 85);

    assertThat(guardrails.apply("AeroPress", aeropress)).isSameAs(aeropress);
  }

  @Test
  void aRatioTextThatDisagreesWithTheGrams_isRewrittenWithoutANote() {
    SuggestedRecipe fixed = guardrails.apply("Chemex", answer(30, 480, "1:4", 94));

    assertThat(fixed.ratio()).isEqualTo("1:16");
    assertThat(fixed.waterGrams()).isEqualByComparingTo("480");
    assertThat(fixed.rationale()).isEqualTo("Bright cup.");
  }

  @Test
  void anUnknownUserMethod_onlyGetsTheLiquidWaterBound() {
    SuggestedRecipe odd = answer(20, 100, "1:5", 120);

    SuggestedRecipe fixed = guardrails.apply("My grandma's pot", odd);

    assertThat(fixed.waterGrams()).isEqualByComparingTo("100");
    assertThat(fixed.waterTemp()).isEqualTo(100);
  }

  @Test
  void theNoteFollowsTheRequestLanguage() {
    LocaleContextHolder.setLocale(Locale.forLanguageTag("es"));

    SuggestedRecipe fixed = guardrails.apply("V60", answer(25, 225, "1:9", 88));

    assertThat(fixed.rationale()).endsWith("Ajustado al rango habitual de este método.");
  }

  @Test
  void aWordyRatioAndOverlongTexts_fitTheRecipeForm() {
    // Seen end to end: qwen3:8b answered "75 g de café : 1000 g de agua (1:13.33)" for cold brew.
    SuggestedRecipe wordy =
        new SuggestedRecipe(
            BigDecimal.valueOf(75),
            BigDecimal.valueOf(1000),
            "75 g de café : 1000 g de agua (1:13.33)",
            "coarse",
            15,
            "between twelve and twenty-four hours",
            "x".repeat(1200),
            "Smooth.");

    SuggestedRecipe fixed = guardrails.apply("Cold Brew", wordy);

    assertThat(fixed.ratio()).isEqualTo("1:13.3");
    assertThat(fixed.brewTime()).hasSizeLessThanOrEqualTo(20).endsWith("…");
    assertThat(fixed.steps()).hasSizeLessThanOrEqualTo(1000);
    assertThat(fixed.rationale()).isEqualTo("Smooth.");
  }

  @Test
  void stepsFollowTheAdjustedWaterAndTemperature() {
    // Seen end to end: French press 30 g / 225 g was raised to 360 g, but the steps still said 225
    // g.
    SuggestedRecipe answer =
        new SuggestedRecipe(
            BigDecimal.valueOf(30),
            BigDecimal.valueOf(225),
            "1:7.5",
            "coarse",
            99,
            "4:00",
            "Heat water to 99°C. Pour 225 g of water over 30 g of coffee. Bloom with 50 g first.",
            "Rich.");

    SuggestedRecipe fixed = guardrails.apply("French Press", answer);

    assertThat(fixed.waterGrams()).isEqualByComparingTo("360.0");
    assertThat(fixed.waterTemp()).isEqualTo(96);
    assertThat(fixed.steps())
        .isEqualTo(
            "Heat water to 96°C. Pour 360 g of water over 30 g of coffee. Bloom with 50 g first.");
  }

  @Test
  void anAbsurdDose_isBroughtBackAndTheWaterFollows() {
    // Seen end to end: 173 g of coffee and 2249 g of water for one V60 (a fine 1:13 ratio).
    SuggestedRecipe answer =
        new SuggestedRecipe(
            BigDecimal.valueOf(173),
            BigDecimal.valueOf(2249),
            "1:13",
            "medium-fine",
            92,
            "3:00",
            "Pour 2249 g of water over 173 g of coffee.",
            "Bright.");

    SuggestedRecipe fixed = guardrails.apply("V60", answer);

    assertThat(fixed.coffeeGrams()).isEqualByComparingTo("40.0");
    assertThat(fixed.waterGrams()).isEqualByComparingTo("520.0");
    assertThat(fixed.ratio()).isEqualTo("1:13");
    assertThat(fixed.steps()).isEqualTo("Pour 520 g of water over 40 g of coffee.");
    assertThat(fixed.rationale()).endsWith("Adjusted to the usual range for this brew method.");
  }
}
