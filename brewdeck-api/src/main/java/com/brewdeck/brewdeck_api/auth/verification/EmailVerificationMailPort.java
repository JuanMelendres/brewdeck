package com.brewdeck.brewdeck_api.auth.verification;

import java.util.Locale;

/** Delivers an email-verification link. Swap adapters via {@code brewdeck.mail.enabled}. */
public interface EmailVerificationMailPort {
  /** Sends the link in {@code locale}'s language (English or Spanish, ADR-015). */
  void sendVerificationLink(String email, String rawToken, Locale locale);
}
