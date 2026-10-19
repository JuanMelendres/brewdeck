package com.brewdeck.brewdeck_api.common.config;

import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Argon2id for every new hash, BCrypt still accepted for existing ones (ADR-018). Stored hashes
 * from before have no {@code {id}} prefix, so they match through BCrypt, and {@code
 * upgradeEncoding} reports them as outdated: login re-hashes them with Argon2id.
 */
@Configuration
public class PasswordEncoderConfig {

  static final String ARGON2_ID = "argon2";

  // OWASP's minimum Argon2id profile: m = 19 MiB, t = 2, p = 1.
  private static final int SALT_LENGTH = 16;
  private static final int HASH_LENGTH = 32;
  private static final int PARALLELISM = 1;
  private static final int MEMORY_KIB = 19 * 1024;
  private static final int ITERATIONS = 2;

  // Each Argon2id hash allocates ~19 MiB of heap; at most this many run at once (512 MiB
  // container).
  private static final int MAX_CONCURRENT_HASHES = 4;

  @Bean
  public PasswordEncoder passwordEncoder() {
    PasswordEncoder argon2 =
        new BoundedPasswordEncoder(
            new Argon2PasswordEncoder(
                SALT_LENGTH, HASH_LENGTH, PARALLELISM, MEMORY_KIB, ITERATIONS),
            MAX_CONCURRENT_HASHES);
    PasswordEncoder bcrypt = new LenientBCryptPasswordEncoder();
    DelegatingPasswordEncoder encoder =
        new DelegatingPasswordEncoder(ARGON2_ID, Map.of(ARGON2_ID, argon2, "bcrypt", bcrypt));
    encoder.setDefaultPasswordEncoderForMatches(bcrypt);
    return encoder;
  }

  /**
   * BCrypt that answers "no match" for input it cannot hash (over 72 bytes) instead of throwing.
   * Passwords may now be up to 128 characters, so a long login against an account that still has a
   * BCrypt hash must be a plain failed login, not a 500.
   */
  static final class LenientBCryptPasswordEncoder implements PasswordEncoder {

    private final BCryptPasswordEncoder bcrypt = new BCryptPasswordEncoder();

    @Override
    public String encode(CharSequence rawPassword) {
      return bcrypt.encode(rawPassword);
    }

    @Override
    public boolean matches(CharSequence rawPassword, String encodedPassword) {
      try {
        return bcrypt.matches(rawPassword, encodedPassword);
      } catch (IllegalArgumentException tooLong) {
        return false;
      }
    }

    @Override
    public boolean upgradeEncoding(String encodedPassword) {
      return bcrypt.upgradeEncoding(encodedPassword);
    }
  }
}
