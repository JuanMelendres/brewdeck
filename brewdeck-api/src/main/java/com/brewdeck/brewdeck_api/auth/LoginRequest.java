package com.brewdeck.brewdeck_api.auth;

import com.brewdeck.brewdeck_api.common.validation.MaxUtf8Bytes;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// No account can have a password over 72 bytes (BCrypt limit), so reject it before hashing.
public record LoginRequest(
    @NotBlank @Email @Size(max = 255) String email,
    @NotBlank
        @MaxUtf8Bytes(
            value = 72,
            message =
                "Password must not exceed 72 bytes (fewer characters if it uses accents or emoji)")
        String password) {

  public LoginRequest {
    email = EmailAddresses.normalize(email);
  }
}
