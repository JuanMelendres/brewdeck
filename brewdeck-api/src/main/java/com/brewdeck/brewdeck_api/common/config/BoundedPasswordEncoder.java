package com.brewdeck.brewdeck_api.common.config;

import java.util.concurrent.Semaphore;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Limits how many hashes run at once. Argon2id is memory-hard on purpose (~19 MiB each), so a burst
 * of logins on a small container queues here instead of running out of heap (ADR-018).
 */
final class BoundedPasswordEncoder implements PasswordEncoder {

  private final PasswordEncoder delegate;
  private final Semaphore permits;

  BoundedPasswordEncoder(PasswordEncoder delegate, int maxConcurrent) {
    this.delegate = delegate;
    this.permits = new Semaphore(maxConcurrent, true);
  }

  @Override
  public String encode(CharSequence rawPassword) {
    acquire();
    try {
      return delegate.encode(rawPassword);
    } finally {
      permits.release();
    }
  }

  @Override
  public boolean matches(CharSequence rawPassword, String encodedPassword) {
    acquire();
    try {
      return delegate.matches(rawPassword, encodedPassword);
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
