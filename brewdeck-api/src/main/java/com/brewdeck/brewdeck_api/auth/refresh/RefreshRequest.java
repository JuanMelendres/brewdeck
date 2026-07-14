package com.brewdeck.brewdeck_api.auth.refresh;

import jakarta.validation.constraints.NotBlank;

public record RefreshRequest(@NotBlank String refreshToken) {}
