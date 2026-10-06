package com.brewdeck.brewdeck_api.auth.reset;

import java.util.Locale;

/** Delivers a password-reset link to the user. Swap adapters via {@code brewdeck.mail.enabled}. */
public interface PasswordResetMailPort {
  /** Sends the link in {@code locale}'s language (English or Spanish, ADR-015). */
  void sendResetLink(String email, String rawToken, Locale locale);
}
