package com.brewdeck.brewdeck_api.common.ratelimit;

/** Shared wording for 429 responses from the filter and the exception handler. */
public final class RateLimitMessages {

  private RateLimitMessages() {}

  public static String tooManyAttempts(long retryAfterSeconds) {
    long minutes = (retryAfterSeconds + 59) / 60;
    return minutes <= 1
        ? "Too many attempts. Try again in a minute."
        : "Too many attempts. Try again in " + minutes + " minutes.";
  }
}
