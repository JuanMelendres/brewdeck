package com.brewdeck.brewdeck_api.auth;

import com.brewdeck.brewdeck_api.common.validation.MaxUtf8Bytes;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// Passwords are capped at 72 UTF-8 bytes: BCrypt rejects longer input.
public record RegisterRequest(
    @NotBlank @Email String email,
    @NotBlank
        @Size(min = 8, message = "Password must be at least 8 characters")
        @MaxUtf8Bytes(
            value = 72,
            message =
                "Password must not exceed 72 bytes (fewer characters if it uses accents or emoji)")
        String password) {

  public RegisterRequest {
    email = EmailAddresses.normalize(email);
  }
}
