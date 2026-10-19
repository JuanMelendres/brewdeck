package com.brewdeck.brewdeck_api.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// Passwords may be up to 128 characters (ADR-018: Argon2id has no 72-byte limit).
public record ChangePasswordRequest(
    @NotBlank @Size(max = 128, message = "{validation.password.tooLong}") String currentPassword,
    @NotBlank
        @Size(min = 8, message = "{validation.newPassword.tooShort}")
        @Size(max = 128, message = "{validation.password.tooLong}")
        String newPassword) {}
