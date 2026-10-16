package com.brewdeck.brewdeck_api.auth.reset;

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
class PasswordResetTokenRepositoryTest extends PostgresIntegrationTest {

  @Autowired private PasswordResetTokenRepository tokenRepository;
  @Autowired private UserRepository userRepository;

  @Test
  void findByTokenHash_returnsToken() {
    User user =
        userRepository.save(
            User.builder()
                .email("reset-" + System.nanoTime() + "@example.com")
                .passwordHash("hash")
                .createdAt(Instant.now())
                .build());
    tokenRepository.save(
        PasswordResetToken.builder()
            .userId(user.getId())
            .tokenHash("abc123")
            .expiresAt(Instant.now().plus(30, ChronoUnit.MINUTES))
            .createdAt(Instant.now())
            .build());

    assertThat(tokenRepository.findByTokenHash("abc123")).isPresent();
    assertThat(tokenRepository.findByTokenHash("missing")).isEmpty();
  }

  @Test
  void findByUserIdAndUsedAtIsNull_excludesUsedTokens() {
    User user =
        userRepository.save(
            User.builder()
                .email("reset-" + System.nanoTime() + "@example.com")
                .passwordHash("hash")
                .createdAt(Instant.now())
                .build());
    tokenRepository.save(
        PasswordResetToken.builder()
            .userId(user.getId())
            .tokenHash("unused-" + System.nanoTime())
            .expiresAt(Instant.now().plus(30, ChronoUnit.MINUTES))
            .createdAt(Instant.now())
            .build());
    tokenRepository.save(
        PasswordResetToken.builder()
            .userId(user.getId())
            .tokenHash("used-" + System.nanoTime())
            .expiresAt(Instant.now().plus(30, ChronoUnit.MINUTES))
            .usedAt(Instant.now())
            .createdAt(Instant.now())
            .build());

    assertThat(tokenRepository.findByUserIdAndUsedAtIsNull(user.getId())).hasSize(1);
  }

  @Test
  void markUsedIfUnused_claimsTheTokenOnlyOnce() {
    User user =
        userRepository.save(
            User.builder()
                .email("reset-" + System.nanoTime() + "@example.com")
                .passwordHash("hash")
                .createdAt(Instant.now())
                .build());
    PasswordResetToken token =
        tokenRepository.save(
            PasswordResetToken.builder()
                .userId(user.getId())
                .tokenHash("claim-" + System.nanoTime())
                .expiresAt(Instant.now().plus(30, ChronoUnit.MINUTES))
                .createdAt(Instant.now())
                .build());

    assertThat(tokenRepository.markUsedIfUnused(token.getId(), Instant.now())).isEqualTo(1);
    assertThat(tokenRepository.markUsedIfUnused(token.getId(), Instant.now())).isZero();
  }
}
