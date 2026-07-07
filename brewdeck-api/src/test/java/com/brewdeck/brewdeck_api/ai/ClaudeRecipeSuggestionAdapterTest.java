package com.brewdeck.brewdeck_api.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class ClaudeRecipeSuggestionAdapterTest {

  @Test
  void toSuggestedRecipe_shouldMapPayload() {
    ClaudeRecipeSuggestionAdapter.SuggestedRecipePayload payload =
        new ClaudeRecipeSuggestionAdapter.SuggestedRecipePayload(
            new BigDecimal("15"),
            new BigDecimal("240"),
            "1:16",
            "Medium-fine",
            92,
            "2:30",
            "Bloom then pour.",
            "Balanced for a medium roast.");

    SuggestedRecipe result = ClaudeRecipeSuggestionAdapter.toSuggestedRecipe(payload);

    assertThat(result.coffeeGrams()).isEqualByComparingTo("15");
    assertThat(result.waterTemp()).isEqualTo(92);
    assertThat(result.rationale()).isEqualTo("Balanced for a medium roast.");
  }

  @Test
  void toSuggestedRecipe_shouldThrowAiUnavailable_whenPayloadNull() {
    assertThatThrownBy(() -> ClaudeRecipeSuggestionAdapter.toSuggestedRecipe(null))
        .isInstanceOf(AiUnavailableException.class);
  }
}
