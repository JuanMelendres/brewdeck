package com.brewdeck.brewdeck_api.auth.emailchange;

import com.brewdeck.brewdeck_api.common.web.MessageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Registration, login, and current-user lookup")
public class EmailChangeController {

  static final String REQUEST_MESSAGE =
      "If the address can be used, a confirmation link has been sent to it.";

  private final EmailChangeService emailChangeService;

  @PostMapping("/me/email-change")
  @ResponseStatus(HttpStatus.ACCEPTED)
  @Operation(
      summary = "Request an email change",
      description =
          "Needs the current password. Always 202 with the same body, whether or not the new"
              + " address has an account (no account enumeration). A free address gets a 1-hour"
              + " confirm link and the current address a notice with a cancel link.")
  public MessageResponse request(@Valid @RequestBody RequestEmailChangeRequest request) {
    emailChangeService.request(request);
    return new MessageResponse(REQUEST_MESSAGE);
  }

  @PostMapping("/email-change/confirm")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(
      summary = "Confirm an email change with the token from the new address",
      description = "Public: the token is the credential. Closes every session of the account.")
  public void confirm(@Valid @RequestBody EmailChangeTokenRequest request) {
    emailChangeService.confirm(request.token());
  }

  @PostMapping("/email-change/cancel")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(
      summary = "Cancel a pending email change with the token from the current address",
      description = "Public: the token is the credential. Closes every session of the account.")
  public void cancel(@Valid @RequestBody EmailChangeTokenRequest request) {
    emailChangeService.cancel(request.token());
  }
}
