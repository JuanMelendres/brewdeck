package com.brewdeck.brewdeck_api.recipe;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record RecipeRequest(
    @NotNull(message = "{validation.coffeeId.required}") Long coffeeId,
    @NotNull(message = "{validation.methodId.required}") Long methodId,
    @NotBlank(message = "{validation.recipeName.required}")
        @Size(max = 120, message = "{validation.recipeName.tooLong}")
        String name,
    // DECIMAL(6,2) columns: at most 9999.99 and two decimals. A third decimal is rejected instead
    // of
    // being rounded by the column (DATA-01 G2). @Digits only guards the scale here: its integer
    // limit is loose on purpose, so an oversized value reports @DecimalMax alone.
    @Positive(message = "{validation.coffeeGrams.positive}")
        @DecimalMax(value = "9999.99", message = "{validation.coffeeGrams.max}")
        @Digits(integer = 10, fraction = 2, message = "{validation.coffeeGrams.scale}")
        BigDecimal coffeeGrams,
    @Positive(message = "{validation.waterGrams.positive}")
        @DecimalMax(value = "9999.99", message = "{validation.waterGrams.max}")
        @Digits(integer = 10, fraction = 2, message = "{validation.waterGrams.scale}")
        BigDecimal waterGrams,
    @Size(max = 20, message = "{validation.ratio.tooLong}") String ratio,
    @Size(max = 120, message = "{validation.grindSetting.tooLong}") String grindSetting,
    @DecimalMin(value = "0", message = "{validation.waterTemp.min}")
        @DecimalMax(value = "100", message = "{validation.waterTemp.max}")
        @Digits(integer = 3, fraction = 1, message = "{validation.waterTemp.scale}")
        BigDecimal waterTemp,
    @Size(max = 20, message = "{validation.brewTime.tooLong}") String brewTime,
    @Size(max = 1000, message = "{validation.steps.tooLong}") String steps,
    @Size(max = 500, message = "{validation.expectedTaste.tooLong}") String expectedTaste,
    Boolean favorite,
    // Set when the recipe comes from an AI suggestion (ADR-016); informational, sent by the client.
    @Size(max = 80) String aiModel,
    @Size(max = 20) String aiPromptVersion) {

  /** A recipe written by hand: no AI provenance. */
  public RecipeRequest(
      Long coffeeId,
      Long methodId,
      String name,
      BigDecimal coffeeGrams,
      BigDecimal waterGrams,
      String ratio,
      String grindSetting,
      BigDecimal waterTemp,
      String brewTime,
      String steps,
      String expectedTaste,
      Boolean favorite) {
    this(
        coffeeId,
        methodId,
        name,
        coffeeGrams,
        waterGrams,
        ratio,
        grindSetting,
        waterTemp,
        brewTime,
        steps,
        expectedTaste,
        favorite,
        null,
        null);
  }
}
