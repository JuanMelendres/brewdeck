package com.brewdeck.brewdeck_api.auth;

import com.brewdeck.brewdeck_api.common.validation.NotCommonPassword;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// Passwords may be up to 128 characters (ADR-018: Argon2id has no 72-byte limit).
public record RegisterRequest(
    @NotBlank @Email @Size(max = 255) String email,
    @NotBlank
        @Size(min = 15, message = "{validation.password.tooShort}")
        @NotCommonPassword
        @Size(max = 128, message = "{validation.password.tooLong}")
        String password) {

  public RegisterRequest {
    email = EmailAddresses.normalize(email);
  }
}
