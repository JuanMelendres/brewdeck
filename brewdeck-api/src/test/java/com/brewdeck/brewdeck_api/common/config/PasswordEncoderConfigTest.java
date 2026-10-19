package com.brewdeck.brewdeck_api.common.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

class PasswordEncoderConfigTest {

  private final PasswordEncoder encoder = new PasswordEncoderConfig().passwordEncoder();

  @Test
  void newHashesAreArgon2id() {
    String hash = encoder.encode("correct horse battery staple");

    assertThat(hash).startsWith("{argon2}$argon2id$");
    assertThat(encoder.matches("correct horse battery staple", hash)).isTrue();
    assertThat(encoder.matches("wrong", hash)).isFalse();
    assertThat(encoder.upgradeEncoding(hash)).isFalse();
  }

  @Test
  void existingUnprefixedBcryptHashesStillMatch_andAreMarkedForUpgrade() {
    // Hashes stored before ADR-018: plain BCrypt, no {id} prefix.
    String legacy = new BCryptPasswordEncoder().encode("password1");

    assertThat(encoder.matches("password1", legacy)).isTrue();
    assertThat(encoder.matches("password2", legacy)).isFalse();
    assertThat(encoder.upgradeEncoding(legacy)).isTrue();
  }

  @Test
  void aLongPasswordAgainstALegacyBcryptHash_isANonMatchNotAnError() {
    String legacy = new BCryptPasswordEncoder().encode("password1");
    String longPassphrase = "é".repeat(64); // 128 bytes, over BCrypt's 72.

    assertThatCode(() -> encoder.matches(longPassphrase, legacy)).doesNotThrowAnyException();
    assertThat(encoder.matches(longPassphrase, legacy)).isFalse();
  }

  @Test
  void longMultibytePassphrasesHashAndMatch() {
    String passphrase = "café con leche y pan dulce ".repeat(4).trim(); // > 72 bytes

    String hash = encoder.encode(passphrase);

    assertThat(encoder.matches(passphrase, hash)).isTrue();
  }

  @Test
  void boundedEncoder_neverRunsMoreHashesAtOnceThanItsPermits() throws Exception {
    AtomicInteger running = new AtomicInteger();
    AtomicInteger peak = new AtomicInteger();
    PasswordEncoder slow =
        new PasswordEncoder() {
          @Override
          public String encode(CharSequence raw) {
            int now = running.incrementAndGet();
            peak.accumulateAndGet(now, Math::max);
            try {
              Thread.sleep(20);
            } catch (InterruptedException e) {
              Thread.currentThread().interrupt();
            }
            running.decrementAndGet();
            return "hash";
          }

          @Override
          public boolean matches(CharSequence raw, String encoded) {
            return true;
          }
        };
    PasswordEncoder bounded = new BoundedPasswordEncoder(slow, 2);
    ExecutorService pool = Executors.newFixedThreadPool(8);
    CountDownLatch done = new CountDownLatch(16);
    try {
      for (int i = 0; i < 16; i++) {
        pool.submit(
            () -> {
              bounded.encode("x");
              done.countDown();
            });
      }
      assertThat(done.await(10, TimeUnit.SECONDS)).isTrue();
    } finally {
      pool.shutdownNow();
    }

    assertThat(peak.get()).isLessThanOrEqualTo(2);
  }
}
