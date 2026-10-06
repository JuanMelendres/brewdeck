package com.brewdeck.brewdeck_api.auth;

import java.util.Locale;

/**
 * The web app's language chosen by a {@link User}; {@code null} on the user means not chosen, so
 * the app follows the browser language (ADR-015).
 */
public enum Language {
  EN(Locale.ENGLISH),
  ES(Locale.forLanguageTag("es"));

  private final Locale locale;

  Language(Locale locale) {
    this.locale = locale;
  }

  public Locale locale() {
    return locale;
  }
}
