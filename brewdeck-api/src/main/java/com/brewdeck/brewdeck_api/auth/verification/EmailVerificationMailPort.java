package com.brewdeck.brewdeck_api.auth.verification;

/** Delivers an email-verification link. Swap adapters via {@code brewdeck.mail.enabled}. */
public interface EmailVerificationMailPort {
  void sendVerificationLink(String email, String rawToken);
}
