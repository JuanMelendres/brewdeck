package com.brewdeck.brewdeck_api.coffee;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CoffeeRequest(
    @NotBlank(message = "{validation.coffeeName.required}")
        @Size(max = 120, message = "{validation.coffeeName.tooLong}")
        String name,
    @Size(max = 120, message = "{validation.brand.tooLong}") String brand,
    @Size(max = 120, message = "{validation.origin.tooLong}") String origin,
    @Size(max = 120, message = "{validation.region.tooLong}") String region,
    @Size(max = 120, message = "{validation.farm.tooLong}") String farm,
    @Size(max = 120, message = "{validation.producer.tooLong}") String producer,
    @Size(max = 120, message = "{validation.variety.tooLong}") String variety,
    @Size(max = 80, message = "{validation.process.tooLong}") String process,
    @Size(max = 80, message = "{validation.roastLevel.tooLong}") String roastLevel,
    @Size(max = 255, message = "{validation.notesPrimary.tooLong}") String notesPrimary,
    @Size(max = 500, message = "{validation.notesSecondary.tooLong}") String notesSecondary,
    @Min(value = 1, message = "{validation.acidityScore.min}")
        @Max(value = 5, message = "{validation.acidityScore.max}")
        Integer acidityScore,
    @Min(value = 1, message = "{validation.bodyScore.min}")
        @Max(value = 5, message = "{validation.bodyScore.max}")
        Integer bodyScore,
    @Min(value = 1, message = "{validation.sweetnessScore.min}")
        @Max(value = 5, message = "{validation.sweetnessScore.max}")
        Integer sweetnessScore,
    @Min(value = 1, message = "{validation.bitternessScore.min}")
        @Max(value = 5, message = "{validation.bitternessScore.max}")
        Integer bitternessScore,
    @Size(max = 1000, message = "{validation.description.tooLong}") String description) {}
