package com.brewdeck.brewdeck_api.method;

import jakarta.validation.constraints.NotBlank;

public record BrewMethodRequest(
    @NotBlank(message = "Method name is required") String name, String description) {}
