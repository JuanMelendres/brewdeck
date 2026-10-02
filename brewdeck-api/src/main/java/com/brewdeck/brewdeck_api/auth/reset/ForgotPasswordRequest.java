package com.brewdeck.brewdeck_api.auth.reset;

import com.brewdeck.brewdeck_api.auth.EmailAddresses;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ForgotPasswordRequest(@NotBlank @Email @Size(max = 255) String email) {

  public ForgotPasswordRequest {
    email = EmailAddresses.normalize(email);
  }
}
