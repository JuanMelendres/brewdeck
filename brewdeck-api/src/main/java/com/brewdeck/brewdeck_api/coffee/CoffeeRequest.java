package com.brewdeck.brewdeck_api.coffee;

import jakarta.validation.constraints.NotBlank;

public record CoffeeRequest(
    @NotBlank(message = "Coffee name is required") String name,
    String brand,
    String origin,
    String region,
    String farm,
    String producer,
    String variety,
    String process,
    String roastLevel,
    String notesPrimary,
    String notesSecondary,
    String acidity,
    String body,
    String sweetness,
    String bitterness,
    String description) {}
