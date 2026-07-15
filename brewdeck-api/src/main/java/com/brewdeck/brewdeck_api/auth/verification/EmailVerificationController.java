package com.brewdeck.brewdeck_api.auth.verification;

import com.brewdeck.brewdeck_api.common.web.MessageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.security.Principal;
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
  public MessageResponse resendVerification(Principal principal) {
    emailVerificationService.resendFor(principal.getName());
    return new MessageResponse(RESENT_MESSAGE);
  }
}
