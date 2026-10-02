package com.brewdeck.brewdeck_api.auth.verification;

import static org.assertj.core.api.Assertions.assertThat;

import com.brewdeck.brewdeck_api.auth.User;
import com.brewdeck.brewdeck_api.auth.UserRepository;
import com.brewdeck.brewdeck_api.common.PostgresIntegrationTest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
class EmailVerificationTokenRepositoryTest extends PostgresIntegrationTest {

  @Autowired private EmailVerificationTokenRepository tokenRepository;
  @Autowired private UserRepository userRepository;

  private User persistUser() {
    return userRepository.save(
        User.builder()
            .email("verify-" + System.nanoTime() + "@example.com")
            .passwordHash("hash")
            .createdAt(Instant.now())
            .build());
  }

  @Test
  void findByTokenHash_returnsToken() {
    User user = persistUser();
    tokenRepository.save(
        EmailVerificationToken.builder()
            .userId(user.getId())
            .tokenHash("hash-" + System.nanoTime())
            .expiresAt(Instant.now().plus(24, ChronoUnit.HOURS))
            .createdAt(Instant.now())
            .build());
    String hash = tokenRepository.findAll().get(0).getTokenHash();

    assertThat(tokenRepository.findByTokenHash(hash)).isPresent();
    assertThat(tokenRepository.findByTokenHash("missing")).isEmpty();
  }

  @Test
  void findByUserIdAndUsedAtIsNull_excludesUsedTokens() {
    User user = persistUser();
    tokenRepository.save(
        EmailVerificationToken.builder()
            .userId(user.getId())
            .tokenHash("unused-" + System.nanoTime())
            .expiresAt(Instant.now().plus(24, ChronoUnit.HOURS))
            .createdAt(Instant.now())
            .build());
    tokenRepository.save(
        EmailVerificationToken.builder()
            .userId(user.getId())
            .tokenHash("used-" + System.nanoTime())
            .expiresAt(Instant.now().plus(24, ChronoUnit.HOURS))
            .usedAt(Instant.now())
            .createdAt(Instant.now())
            .build());

    assertThat(tokenRepository.findByUserIdAndUsedAtIsNull(user.getId())).hasSize(1);
  }
}
