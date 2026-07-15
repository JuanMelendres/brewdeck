package com.brewdeck.brewdeck_api.auth.reset;

import com.brewdeck.brewdeck_api.common.web.MessageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Registration, login, and current-user lookup")
public class PasswordResetController {

  private static final String GENERIC_MESSAGE = "If that email exists, a reset link has been sent.";

  private final PasswordResetService passwordResetService;

  @PostMapping("/forgot-password")
  @Operation(summary = "Request a password reset link")
  public MessageResponse forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
    passwordResetService.requestReset(request);
    return new MessageResponse(GENERIC_MESSAGE);
  }

  @PostMapping("/reset-password")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(summary = "Reset a password using a reset token")
  public void resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
    passwordResetService.resetPassword(request);
  }
}
