package com.brewdeck.brewdeck_api.auth;

import com.brewdeck.brewdeck_api.common.i18n.RequestLocale;
import java.util.Locale;

/** The language to write to a user in, e.g. for emails (ADR-015). */
public final class UserLocale {

  private UserLocale() {}

  /** The user's saved language, else the language of the current request. */
  public static Locale of(User user) {
    return user.getLanguage() != null ? user.getLanguage().locale() : RequestLocale.current();
  }
}
