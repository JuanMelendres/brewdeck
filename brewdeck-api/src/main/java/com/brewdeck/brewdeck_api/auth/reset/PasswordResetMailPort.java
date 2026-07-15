package com.brewdeck.brewdeck_api.auth.reset;

/** Delivers a password-reset link to the user. Swap adapters via {@code brewdeck.mail.enabled}. */
public interface PasswordResetMailPort {
  void sendResetLink(String email, String rawToken);
}
