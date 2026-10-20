package com.brewdeck.brewdeck_api.account;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DeleteAccountRequest(
    @NotBlank @Size(max = 128, message = "{validation.password.tooLong}") String currentPassword) {}
