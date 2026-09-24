package com.brewdeck.brewdeck_api.auth.reset;

import com.brewdeck.brewdeck_api.auth.EmailAddresses;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ForgotPasswordRequest(@NotBlank @Email String email) {

  public ForgotPasswordRequest {
    email = EmailAddresses.normalize(email);
  }
}
