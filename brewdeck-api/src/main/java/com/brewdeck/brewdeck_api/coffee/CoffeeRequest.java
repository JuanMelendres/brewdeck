package com.brewdeck.brewdeck_api.coffee;

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
    @Size(max = 80, message = "Acidity must not exceed 80 characters") String acidity,
    @Size(max = 80, message = "Body must not exceed 80 characters") String body,
    @Size(max = 80, message = "Sweetness must not exceed 80 characters") String sweetness,
    @Size(max = 80, message = "Bitterness must not exceed 80 characters") String bitterness,
    @Size(max = 1000, message = "Description must not exceed 1000 characters")
        String description) {}
