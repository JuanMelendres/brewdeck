package com.brewdeck.brewdeck_api.auth.verification;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Registration, login, and account management")
public class EmailVerificationController {

  private static final String RESENT_MESSAGE = "Verification email sent.";

  private final EmailVerificationService emailVerificationService;

  @PostMapping("/verify-email")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(summary = "Verify an email address using a verification token")
  public void verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
    emailVerificationService.verify(request.token());
  }

  @PostMapping("/resend-verification")
  @Operation(summary = "Resend the verification email for the current user")
  public Map<String, String> resendVerification(Principal principal) {
    emailVerificationService.resendFor(principal.getName());
    return Map.of("message", RESENT_MESSAGE);
  }
}
