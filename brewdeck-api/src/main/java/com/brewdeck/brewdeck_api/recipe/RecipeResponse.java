package com.brewdeck.brewdeck_api.recipe;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record RecipeResponse(
    Long id,
    Long coffeeId,
    String coffeeName,
    Long methodId,
    String methodName,
    String name,
    BigDecimal coffeeGrams,
    BigDecimal waterGrams,
    String ratio,
    String grindSetting,
    Integer waterTemp,
    String brewTime,
    String steps,
    String expectedTaste,
    Boolean favorite,
    LocalDateTime createdAt,
    LocalDateTime updatedAt) {
  public static RecipeResponse fromEntity(Recipe recipe) {
    return new RecipeResponse(
        recipe.getId(),
        recipe.getCoffee().getId(),
        recipe.getCoffee().getName(),
        recipe.getMethod().getId(),
        recipe.getMethod().getName(),
        recipe.getName(),
        recipe.getCoffeeGrams(),
        recipe.getWaterGrams(),
        recipe.getRatio(),
        recipe.getGrindSetting(),
        recipe.getWaterTemp(),
        recipe.getBrewTime(),
        recipe.getSteps(),
        recipe.getExpectedTaste(),
        recipe.getFavorite(),
        recipe.getCreatedAt(),
        recipe.getUpdatedAt());
  }
}
