package com.brewdeck.brewdeck_api.session;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record BrewSessionRequest(
    @NotNull(message = "Recipe id is required") Long recipeId,
    @Size(max = 120, message = "Actual grind must not exceed 120 characters") String actualGrind,
    @Min(value = 70, message = "Actual temperature must be at least 70 degrees Celsius")
        @Max(value = 100, message = "Actual temperature must not exceed 100 degrees Celsius")
        Integer actualTemp,
    @Size(max = 20, message = "Actual time must not exceed 20 characters") String actualTime,
    @Size(max = 1000, message = "Taste result must not exceed 1000 characters") String tasteResult,
    @Min(value = 1, message = "Rating must be at least 1")
        @Max(value = 10, message = "Rating must not exceed 10")
        Integer rating,
    @Size(max = 1000, message = "Adjustment notes must not exceed 1000 characters")
        String adjustmentNotes) {}
