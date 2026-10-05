package com.brewdeck.brewdeck_api.method;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BrewMethodRequest(
    @NotBlank(message = "{validation.methodName.required}")
        @Size(max = 80, message = "{validation.methodName.tooLong}")
        String name,
    @Size(max = 500, message = "{validation.methodDescription.tooLong}") String description) {}
