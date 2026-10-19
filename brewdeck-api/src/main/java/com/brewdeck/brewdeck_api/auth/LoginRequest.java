package com.brewdeck.brewdeck_api.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// No account can have a password over 128 characters (ADR-018), so reject it before hashing.
public record LoginRequest(
    @NotBlank @Email @Size(max = 255) String email,
    @NotBlank @Size(max = 128, message = "{validation.password.tooLong}") String password) {

  public LoginRequest {
    email = EmailAddresses.normalize(email);
  }
}
