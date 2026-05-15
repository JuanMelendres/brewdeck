package com.brewdeck.brewdeck_api.recipe;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record RecipeRequest(
    @NotNull(message = "Coffee id is required") Long coffeeId,
    @NotNull(message = "Brew method id is required") Long methodId,
    @NotBlank(message = "Recipe name is required")
        @Size(max = 120, message = "Recipe name must not exceed 120 characters")
        String name,
    @Positive(message = "Coffee grams must be greater than zero") BigDecimal coffeeGrams,
    @Positive(message = "Water grams must be greater than zero") BigDecimal waterGrams,
    @Size(max = 20, message = "Ratio must not exceed 20 characters") String ratio,
    @Size(max = 120, message = "Grind setting must not exceed 120 characters") String grindSetting,
    @Min(value = 70, message = "Water temperature must be at least 70 degrees Celsius")
        @Max(value = 100, message = "Water temperature must not exceed 100 degrees Celsius")
        Integer waterTemp,
    @Size(max = 20, message = "Brew time must not exceed 20 characters") String brewTime,
    @Size(max = 1000, message = "Steps must not exceed 1000 characters") String steps,
    @Size(max = 500, message = "Expected taste must not exceed 500 characters")
        String expectedTaste,
    Boolean favorite) {}
