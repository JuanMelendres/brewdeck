package com.brewdeck.brewdeck_api.recipe;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record RecipeRequest(
    @NotNull(message = "Coffee id is required") Long coffeeId,
    @NotNull(message = "Brew method id is required") Long methodId,
    @NotBlank(message = "Recipe name is required") String name,
    BigDecimal coffeeGrams,
    BigDecimal waterGrams,
    String ratio,
    String grindSetting,
    Integer waterTemp,
    String brewTime,
    String steps,
    String expectedTaste,
    Boolean favorite) {}
