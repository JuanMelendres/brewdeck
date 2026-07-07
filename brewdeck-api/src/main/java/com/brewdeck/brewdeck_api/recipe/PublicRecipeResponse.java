package com.brewdeck.brewdeck_api.recipe;

import java.math.BigDecimal;

public record PublicRecipeResponse(
    String name,
    String coffeeName,
    String methodName,
    BigDecimal coffeeGrams,
    BigDecimal waterGrams,
    String ratio,
    String grindSetting,
    Integer waterTemp,
    String brewTime,
    String steps,
    String expectedTaste) {
  public static PublicRecipeResponse fromEntity(Recipe recipe) {
    return new PublicRecipeResponse(
        recipe.getName(),
        recipe.getCoffee().getName(),
        recipe.getMethod().getName(),
        recipe.getCoffeeGrams(),
        recipe.getWaterGrams(),
        recipe.getRatio(),
        recipe.getGrindSetting(),
        recipe.getWaterTemp(),
        recipe.getBrewTime(),
        recipe.getSteps(),
        recipe.getExpectedTaste());
  }
}
