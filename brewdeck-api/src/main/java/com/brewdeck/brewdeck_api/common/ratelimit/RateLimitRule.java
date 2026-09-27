package com.brewdeck.brewdeck_api.common.ratelimit;

import java.time.Duration;

/**
 * Fixed-window limits for abuse-prone endpoints. {@code *_IP} rules are keyed by client IP (applied
 * by {@link AuthRateLimitFilter}); {@code *_EMAIL} rules are keyed by the target account's email
 * (applied in the services) so rotating IPs does not help against one account.
 */
public enum RateLimitRule {
  LOGIN_IP(20, Duration.ofMinutes(1)),
  LOGIN_EMAIL(10, Duration.ofMinutes(15)),
  REGISTER_IP(5, Duration.ofHours(1)),
  REFRESH_IP(30, Duration.ofMinutes(1)),
  FORGOT_PASSWORD_IP(5, Duration.ofMinutes(15)),
  // Stops anyone from flooding a mailbox with reset emails.
  FORGOT_PASSWORD_EMAIL(3, Duration.ofHours(1)),
  RESET_PASSWORD_IP(10, Duration.ofMinutes(15)),
  VERIFY_EMAIL_IP(10, Duration.ofMinutes(15));

  private final int limit;
  private final Duration window;

  RateLimitRule(int limit, Duration window) {
    this.limit = limit;
    this.window = window;
  }

  public int limit() {
    return limit;
  }

  public Duration window() {
    return window;
  }
}
