package com.brewdeck.brewdeck_api.auth.reset;

import static org.assertj.core.api.Assertions.assertThat;

import com.brewdeck.brewdeck_api.auth.User;
import com.brewdeck.brewdeck_api.auth.UserRepository;
import com.brewdeck.brewdeck_api.common.PostgresIntegrationTest;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
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
                .createdAt(LocalDateTime.now())
                .build());
    tokenRepository.save(
        PasswordResetToken.builder()
            .userId(user.getId())
            .tokenHash("abc123")
            .expiresAt(LocalDateTime.now().plusMinutes(30))
            .createdAt(LocalDateTime.now())
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
                .createdAt(LocalDateTime.now())
                .build());
    tokenRepository.save(
        PasswordResetToken.builder()
            .userId(user.getId())
            .tokenHash("unused-" + System.nanoTime())
            .expiresAt(LocalDateTime.now().plusMinutes(30))
            .createdAt(LocalDateTime.now())
            .build());
    tokenRepository.save(
        PasswordResetToken.builder()
            .userId(user.getId())
            .tokenHash("used-" + System.nanoTime())
            .expiresAt(LocalDateTime.now().plusMinutes(30))
            .usedAt(LocalDateTime.now())
            .createdAt(LocalDateTime.now())
            .build());

    assertThat(tokenRepository.findByUserIdAndUsedAtIsNull(user.getId())).hasSize(1);
  }
}
