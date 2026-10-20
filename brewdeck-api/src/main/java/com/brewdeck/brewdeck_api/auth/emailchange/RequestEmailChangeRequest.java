package com.brewdeck.brewdeck_api.auth.emailchange;

import com.brewdeck.brewdeck_api.auth.EmailAddresses;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RequestEmailChangeRequest(
    @NotBlank @Email @Size(max = 255) String newEmail,
    @NotBlank @Size(max = 128, message = "{validation.password.tooLong}") String currentPassword) {

  public RequestEmailChangeRequest {
    newEmail = EmailAddresses.normalize(newEmail);
  }
}
