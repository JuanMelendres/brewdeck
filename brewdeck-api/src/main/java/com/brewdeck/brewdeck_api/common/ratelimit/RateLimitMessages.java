package com.brewdeck.brewdeck_api.common.ratelimit;

import java.util.Locale;
import org.springframework.context.MessageSource;

/** Shared wording for 429 responses from the filter and the exception handler. */
public final class RateLimitMessages {

  private RateLimitMessages() {}

  /** "Too many attempts. Try again in a minute." or "... in 5 minutes.", in {@code locale}. */
  public static String tooManyAttempts(
      MessageSource messageSource, Locale locale, long retryAfterSeconds) {
    long minutes = (retryAfterSeconds + 59) / 60;
    return minutes <= 1
        ? messageSource.getMessage("error.tooManyAttempts.oneMinute", null, locale)
        : messageSource.getMessage("error.tooManyAttempts.minutes", new Object[] {minutes}, locale);
  }
}
