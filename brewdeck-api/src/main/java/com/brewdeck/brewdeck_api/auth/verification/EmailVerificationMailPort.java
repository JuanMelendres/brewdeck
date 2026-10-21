package com.brewdeck.brewdeck_api.auth.verification;

import java.time.Instant;
import java.util.Locale;

/** Delivers an email-verification link. Swap adapters via {@code brewdeck.mail.enabled}. */
public interface EmailVerificationMailPort {
  /** Sends the link in {@code locale}'s language (English or Spanish, ADR-015). */
  void sendVerificationLink(String email, String rawToken, Locale locale);

  /**
   * Tells the owner of an existing account that someone tried to sign up with their address, and
   * points them to log in or reset their password. Signup answers the same way either way, so this
   * email is the only place the difference shows (no account enumeration, AUTH-05.7).
   */
  void sendAccountExistsNotice(String email, Locale locale);

  /**
   * Warns an unverified account that the retention purge deletes it on or after {@code deleteAfter}
   * unless the email is verified with the fresh link (AUTH-06, ADR-021).
   */
  void sendRetentionReminder(String email, String rawToken, Instant deleteAfter, Locale locale);
}
