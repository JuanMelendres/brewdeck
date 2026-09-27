package com.brewdeck.brewdeck_api.common.ratelimit;

import java.time.Duration;
import lombok.Getter;

@Getter
public class RateLimitExceededException extends RuntimeException {

  private final RateLimiter.Decision decision;

  public RateLimitExceededException(Duration retryAfter) {
    super("Rate limit exceeded");
    this.decision = new RateLimiter.Decision(false, retryAfter);
  }
}
