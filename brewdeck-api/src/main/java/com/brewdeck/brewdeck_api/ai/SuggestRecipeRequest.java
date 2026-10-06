package com.brewdeck.brewdeck_api.ai;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SuggestRecipeRequest(
    @NotNull(message = "{validation.coffeeId.required}") Long coffeeId,
    @NotNull(message = "{validation.methodId.required}") Long methodId,
    @Size(max = 500, message = "{validation.notes.tooLong}") String notes) {}
