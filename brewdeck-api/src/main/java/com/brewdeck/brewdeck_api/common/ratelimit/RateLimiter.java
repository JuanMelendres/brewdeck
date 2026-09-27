package com.brewdeck.brewdeck_api.common.ratelimit;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.EnumMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * In-memory fixed-window rate limiter (ADR-011). State lives in this JVM, which is correct for the
 * current single-instance deployment; running several instances would need a shared store (e.g.
 * Redis) instead, or each instance would allow the full limit.
 */
@Component
@Slf4j
public class RateLimiter {

  private static final long MAX_TRACKED_KEYS_PER_RULE = 100_000;

  private final boolean enabled;
  private final Clock clock;
  private final Map<RateLimitRule, Cache<String, Window>> windows =
      new EnumMap<>(RateLimitRule.class);

  @Autowired
  public RateLimiter(@Value("${brewdeck.rate-limit.enabled:true}") boolean enabled) {
    this(enabled, Clock.systemUTC());
  }

  RateLimiter(boolean enabled, Clock clock) {
    this.enabled = enabled;
    this.clock = clock;
    for (RateLimitRule rule : RateLimitRule.values()) {
      // Caffeine only evicts idle keys to bound memory; window boundaries are decided below from
      // the injected clock.
      windows.put(
          rule,
          Caffeine.newBuilder()
              .expireAfterWrite(rule.window())
              .maximumSize(MAX_TRACKED_KEYS_PER_RULE)
              .build());
    }
  }

  /** Counts one attempt for {@code key} under {@code rule}. */
  public Decision tryAcquire(RateLimitRule rule, String key) {
    if (!enabled) {
      return Decision.ALLOWED;
    }
    Instant now = clock.instant();
    Window window =
        windows
            .get(rule)
            .asMap()
            .compute(
                key,
                (k, current) ->
                    current == null || !now.isBefore(current.start().plus(rule.window()))
                        ? new Window(now, 1)
                        : new Window(current.start(), current.count() + 1));

    if (window.count() <= rule.limit()) {
      return Decision.ALLOWED;
    }
    Duration retryAfter = Duration.between(now, window.start().plus(rule.window()));
    log.warn("Rate limit {} exceeded ({} attempts in window)", rule, window.count());
    return new Decision(false, retryAfter);
  }

  /** Like {@link #tryAcquire} but throws {@link RateLimitExceededException} when over the limit. */
  public void requireAllowed(RateLimitRule rule, String key) {
    Decision decision = tryAcquire(rule, key);
    if (!decision.allowed()) {
      throw new RateLimitExceededException(decision.retryAfter());
    }
  }

  private record Window(Instant start, int count) {}

  public record Decision(boolean allowed, Duration retryAfter) {
    static final Decision ALLOWED = new Decision(true, Duration.ZERO);

    /** Whole seconds to wait, rounded up, at least 1. */
    public long retryAfterSeconds() {
      long seconds = retryAfter.toSeconds() + (retryAfter.toNanosPart() > 0 ? 1 : 0);
      return Math.max(1, seconds);
    }
  }
}
