package com.brewdeck.brewdeck_api.auth.verification;

import jakarta.validation.constraints.NotBlank;

public record VerifyEmailRequest(@NotBlank String token) {}
