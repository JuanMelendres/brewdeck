package com.brewdeck.brewdeck_api.recipe;

import static org.assertj.core.api.Assertions.assertThat;

import com.brewdeck.brewdeck_api.session.BrewSessionRequest;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

/** Cold brew uses cold water, so brew temperatures go down to 0 degrees Celsius. */
class BrewTemperatureValidationTest {

  private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

  private static RecipeRequest recipeAt(Integer waterTemp) {
    return new RecipeRequest(
        1L,
        9L,
        "Cold brew",
        BigDecimal.valueOf(100),
        BigDecimal.valueOf(800),
        "1:8",
        "Coarse",
        waterTemp,
        "16:00:00",
        "Steep in the fridge, then filter.",
        "Smooth, chocolate.",
        false);
  }

  private boolean violates(Object request, String property) {
    return validator.validate(request).stream()
        .anyMatch(v -> v.getPropertyPath().toString().equals(property));
  }

  @Test
  void coldBrewRecipe_withFridgeColdWater_isValid() {
    assertThat(violates(recipeAt(4), "waterTemp")).isFalse();
    assertThat(violates(recipeAt(0), "waterTemp")).isFalse();
  }

  @Test
  void recipe_temperatureOutsideLiquidWater_isRejected() {
    assertThat(violates(recipeAt(-1), "waterTemp")).isTrue();
    assertThat(violates(recipeAt(101), "waterTemp")).isTrue();
  }

  @Test
  void coldBrewSession_withColdWater_isValid() {
    BrewSessionRequest session =
        new BrewSessionRequest(1L, "Coarse", 4, "16:00:00", "Smooth", 8, null);

    assertThat(violates(session, "actualTemp")).isFalse();
  }
}
