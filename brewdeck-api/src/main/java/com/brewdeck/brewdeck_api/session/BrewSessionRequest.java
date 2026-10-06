package com.brewdeck.brewdeck_api.session;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record BrewSessionRequest(
    @NotNull(message = "{validation.recipeId.required}") Long recipeId,
    @Size(max = 120, message = "{validation.actualGrind.tooLong}") String actualGrind,
    @Min(value = 70, message = "{validation.actualTemp.min}")
        @Max(value = 100, message = "{validation.actualTemp.max}")
        Integer actualTemp,
    @Size(max = 20, message = "{validation.actualTime.tooLong}") String actualTime,
    @Size(max = 1000, message = "{validation.tasteResult.tooLong}") String tasteResult,
    @Min(value = 1, message = "{validation.rating.min}")
        @Max(value = 10, message = "{validation.rating.max}")
        Integer rating,
    @Size(max = 1000, message = "{validation.adjustmentNotes.tooLong}") String adjustmentNotes) {}
