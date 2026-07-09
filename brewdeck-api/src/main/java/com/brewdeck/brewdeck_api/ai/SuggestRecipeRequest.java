package com.brewdeck.brewdeck_api.ai;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SuggestRecipeRequest(
    @NotNull(message = "Coffee id is required") Long coffeeId,
    @NotNull(message = "Brew method id is required") Long methodId,
    @Size(max = 500, message = "Notes must not exceed 500 characters") String notes) {}
