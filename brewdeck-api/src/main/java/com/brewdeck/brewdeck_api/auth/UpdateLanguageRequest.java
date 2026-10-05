package com.brewdeck.brewdeck_api.auth;

import jakarta.validation.constraints.NotNull;

public record UpdateLanguageRequest(@NotNull Language language) {}
