package com.brewdeck.brewdeck_api.session;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record BrewSessionRequest(
    @NotNull(message = "Recipe id is required") Long recipeId,
    String actualGrind,
    Integer actualTemp,
    String actualTime,
    String tasteResult,
    @Min(1) @Max(10) Integer rating,
    String adjustmentNotes) {}
