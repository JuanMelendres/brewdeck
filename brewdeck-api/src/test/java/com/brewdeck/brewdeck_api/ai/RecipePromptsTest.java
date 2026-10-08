package com.brewdeck.brewdeck_api.ai;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Locale;
import org.junit.jupiter.api.Test;

class RecipePromptsTest {

  @Test
  void stepsAndRationaleFollowTheUsersLanguage() {
    assertThat(RecipePrompts.suggestSystem(Locale.forLanguageTag("es-MX")))
        .endsWith("Write steps and rationale in Spanish.");
    assertThat(RecipePrompts.improveSystem(Locale.ENGLISH))
        .endsWith("Write steps and rationale in English.");
  }

  @Test
  void theTemperatureRuleAllowsColdBrew() {
    assertThat(RecipePrompts.SUGGEST_SYSTEM).contains("between 0 and 100");
  }
}
