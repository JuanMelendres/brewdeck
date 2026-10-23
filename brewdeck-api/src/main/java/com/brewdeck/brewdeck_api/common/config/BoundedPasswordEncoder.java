package com.brewdeck.brewdeck_api.common.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.concurrent.Semaphore;
import java.util.function.Supplier;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Limits how many hashes run at once. Argon2id is memory-hard on purpose (~19 MiB each), so a burst
 * of logins on a small container queues here instead of running out of heap (ADR-018).
 *
 * <p>Metrics (OBS-01): {@code brewdeck.password.hash.wait} is the time spent queued for a permit,
 * {@code brewdeck.password.hash} the time spent hashing, tagged {@code operation=encode|matches}. A
 * growing wait means logins are queueing behind the limit.
 */
final class BoundedPasswordEncoder implements PasswordEncoder {

  static final String HASH_TIMER = "brewdeck.password.hash";
  static final String WAIT_TIMER = "brewdeck.password.hash.wait";

  private final PasswordEncoder delegate;
  private final Semaphore permits;
  private final Timer waitTimer;
  private final Timer encodeTimer;
  private final Timer matchesTimer;

  BoundedPasswordEncoder(PasswordEncoder delegate, int maxConcurrent, MeterRegistry registry) {
    this.delegate = delegate;
    this.permits = new Semaphore(maxConcurrent, true);
    this.waitTimer = Timer.builder(WAIT_TIMER).publishPercentileHistogram().register(registry);
    this.encodeTimer = hashTimer("encode", registry);
    this.matchesTimer = hashTimer("matches", registry);
  }

  private static Timer hashTimer(String operation, MeterRegistry registry) {
    return Timer.builder(HASH_TIMER)
        .tag("operation", operation)
        .publishPercentileHistogram()
        .register(registry);
  }

  @Override
  public String encode(CharSequence rawPassword) {
    return bounded(encodeTimer, () -> delegate.encode(rawPassword));
  }

  @Override
  public boolean matches(CharSequence rawPassword, String encodedPassword) {
    return bounded(matchesTimer, () -> delegate.matches(rawPassword, encodedPassword));
  }

  private <T> T bounded(Timer hashTimer, Supplier<T> hash) {
    waitTimer.record(this::acquire);
    try {
      return hashTimer.record(hash);
    } finally {
      permits.release();
    }
  }

  @Override
  public boolean upgradeEncoding(String encodedPassword) {
    return delegate.upgradeEncoding(encodedPassword);
  }

  private void acquire() {
    try {
      permits.acquire();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Interrupted while waiting to hash a password", e);
    }
  }
}
