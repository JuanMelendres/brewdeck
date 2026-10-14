package com.brewdeck.brewdeck_api.ai;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Locale;
import org.junit.jupiter.api.Test;

class RecipePromptsTest {

  private static SuggestionContext context(String method) {
    return new SuggestionContext(
        "Coffee", null, null, null, null, null, null, null, method, null, null);
  }

  @Test
  void everyTextFieldFollowsTheUsersLanguage() {
    assertThat(RecipePrompts.suggestSystem(Locale.forLanguageTag("es-MX")))
        .endsWith(
            "Write every text field (grind setting, brew time, steps, rationale) in Spanish.");
    assertThat(RecipePrompts.improveSystem(Locale.ENGLISH)).endsWith("in English.");
  }

  @Test
  void theTemperatureRuleAllowsColdBrew() {
    assertThat(RecipePrompts.SUGGEST_SYSTEM).contains("between 0 and 100");
  }

  @Test
  void aKnownMethod_getsItsUsualRatioTemperatureAndGrind() {
    assertThat(RecipePrompts.suggestMessage(context("French Press")))
        .contains(
            "Usual for this method: coffee dose 15-60 g, water-to-coffee ratio 1:12 to 1:17, water"
                + " 88-96 degrees Celsius, grind coarse, like coarse sea salt.");
  }

  @Test
  void anUnknownUserMethod_getsNoHint() {
    assertThat(RecipePrompts.suggestMessage(context("My grandma's pot")))
        .doesNotContain("Usual for");
  }
}
