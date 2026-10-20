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
        String password,
    // The terms version shown at sign-up (LEGAL-01). Required only while the legal-terms flag is
    // on.
    @Size(max = 20) String acceptedTermsVersion) {

  public RegisterRequest {
    email = EmailAddresses.normalize(email);
  }

  public RegisterRequest(String email, String password) {
    this(email, password, null);
  }
}
