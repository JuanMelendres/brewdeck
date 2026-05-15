package com.brewdeck.brewdeck_api.method;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BrewMethodRequest(
    @NotBlank(message = "Method name is required")
        @Size(max = 80, message = "Method name must not exceed 80 characters")
        String name,
    @Size(max = 500, message = "Method description must not exceed 500 characters")
        String description) {}
