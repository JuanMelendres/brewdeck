package com.brewdeck.brewdeck_api.auth;

import java.util.Locale;

/**
 * Canonical form for stored and looked-up email addresses: trimmed and lowercase. The database
 * enforces the same form (V18 {@code chk_users_email_normalized}), so every entry point that
 * receives an email from outside must pass it through {@link #normalize(String)}.
 */
public final class EmailAddresses {

  private EmailAddresses() {}

  /** Returns the canonical form, or {@code null} for {@code null} input. */
  public static String normalize(String email) {
    // Locale.ROOT: a Turkish default locale would turn "I" into a dotless "ı".
    return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
  }
}
