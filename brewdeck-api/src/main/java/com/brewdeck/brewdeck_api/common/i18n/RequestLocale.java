package com.brewdeck.brewdeck_api.common.i18n;

import java.util.Locale;
import org.springframework.context.i18n.LocaleContextHolder;

/** The language of the current request, as resolved by {@link LocaleConfig} (ADR-015). */
public final class RequestLocale {

  private RequestLocale() {}

  /**
   * The locale the DispatcherServlet resolved for this request. Outside a request there is none,
   * and {@link LocaleContextHolder#getLocale()} would fall back to the JVM's locale, so use
   * English.
   */
  public static Locale current() {
    return LocaleContextHolder.getLocaleContext() == null
        ? LocaleConfig.DEFAULT_LOCALE
        : LocaleContextHolder.getLocale();
  }
}
