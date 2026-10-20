package com.brewdeck.brewdeck_api.auth.emailchange;

import java.util.Locale;

/**
 * Delivers the email change messages (ADR-019). Swap adapters via {@code brewdeck.mail.enabled}.
 * Every message is in {@code locale}'s language (English or Spanish, ADR-015).
 */
public interface EmailChangeMailPort {

  /** To the new address: the link that confirms the change. */
  void sendConfirmLink(String newEmail, String rawToken, Locale locale);

  /** To the old address: a change was requested to {@code maskedNewEmail}, with a cancel link. */
  void sendChangeRequestedNotice(
      String oldEmail, String maskedNewEmail, String rawCancelToken, Locale locale);

  /**
   * To a new address that already has an account: someone asked to move another account here and
   * nothing changed. The API answers the same either way, so this email is the only place the
   * difference shows (no account enumeration).
   */
  void sendAddressTakenNotice(String newEmail, Locale locale);

  /** To the old address after the change is confirmed. It has no link. */
  void sendEmailChangedNotice(String oldEmail, String maskedNewEmail, Locale locale);
}
