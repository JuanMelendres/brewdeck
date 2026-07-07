package com.brewdeck.brewdeck_api.coffee;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CoffeeRequest(
    @NotBlank(message = "Coffee name is required")
        @Size(max = 120, message = "Coffee name must not exceed 120 characters")
        String name,
    @Size(max = 120, message = "Brand must not exceed 120 characters") String brand,
    @Size(max = 120, message = "Origin must not exceed 120 characters") String origin,
    @Size(max = 120, message = "Region must not exceed 120 characters") String region,
    @Size(max = 120, message = "Farm must not exceed 120 characters") String farm,
    @Size(max = 120, message = "Producer must not exceed 120 characters") String producer,
    @Size(max = 120, message = "Variety must not exceed 120 characters") String variety,
    @Size(max = 80, message = "Process must not exceed 80 characters") String process,
    @Size(max = 80, message = "Roast level must not exceed 80 characters") String roastLevel,
    @Size(max = 255, message = "Primary notes must not exceed 255 characters") String notesPrimary,
    @Size(max = 500, message = "Secondary notes must not exceed 500 characters")
        String notesSecondary,
    @Min(value = 1, message = "Acidity score must be at least 1")
        @Max(value = 5, message = "Acidity score must not exceed 5")
        Integer acidityScore,
    @Min(value = 1, message = "Body score must be at least 1")
        @Max(value = 5, message = "Body score must not exceed 5")
        Integer bodyScore,
    @Min(value = 1, message = "Sweetness score must be at least 1")
        @Max(value = 5, message = "Sweetness score must not exceed 5")
        Integer sweetnessScore,
    @Min(value = 1, message = "Bitterness score must be at least 1")
        @Max(value = 5, message = "Bitterness score must not exceed 5")
        Integer bitternessScore,
    @Size(max = 1000, message = "Description must not exceed 1000 characters")
        String description) {}
