package com.brewdeck.brewdeck_api.recipe;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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
    // DECIMAL(6,2) columns: at most 9999.99.
    @Positive(message = "{validation.coffeeGrams.positive}")
        @DecimalMax(value = "9999.99", message = "{validation.coffeeGrams.max}")
        BigDecimal coffeeGrams,
    @Positive(message = "{validation.waterGrams.positive}")
        @DecimalMax(value = "9999.99", message = "{validation.waterGrams.max}")
        BigDecimal waterGrams,
    @Size(max = 20, message = "{validation.ratio.tooLong}") String ratio,
    @Size(max = 120, message = "{validation.grindSetting.tooLong}") String grindSetting,
    @Min(value = 0, message = "{validation.waterTemp.min}")
        @Max(value = 100, message = "{validation.waterTemp.max}")
        Integer waterTemp,
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
      Integer waterTemp,
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
