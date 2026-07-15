package com.brewdeck.brewdeck_api.auth.refresh;

import static org.assertj.core.api.Assertions.assertThat;

import com.brewdeck.brewdeck_api.auth.User;
import com.brewdeck.brewdeck_api.auth.UserRepository;
import com.brewdeck.brewdeck_api.common.PostgresIntegrationTest;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
class RefreshTokenRepositoryTest extends PostgresIntegrationTest {

  @Autowired private RefreshTokenRepository refreshTokenRepository;
  @Autowired private UserRepository userRepository;
  @Autowired private TestEntityManager entityManager;

  private Long persistUser() {
    User user =
        User.builder()
            .email("refresh-repo-" + System.nanoTime() + "@example.com")
            .passwordHash("x")
            .createdAt(LocalDateTime.now())
            .build();
    return userRepository.save(user).getId();
  }

  private RefreshToken persistToken(Long userId, String hash, LocalDateTime expiresAt) {
    return refreshTokenRepository.save(
        RefreshToken.builder()
            .userId(userId)
            .tokenHash(hash)
            .expiresAt(expiresAt)
            .createdAt(LocalDateTime.now())
            .build());
  }

  @Test
  void findByTokenHashReturnsTheStoredToken() {
    Long userId = persistUser();
    persistToken(userId, "hash-a", LocalDateTime.now().plusDays(7));

    assertThat(refreshTokenRepository.findByTokenHash("hash-a")).isPresent();
    assertThat(refreshTokenRepository.findByTokenHash("missing")).isEmpty();
  }

  @Test
  void revokeAllActiveForUserRevokesOnlyActiveRowsOfThatUser() {
    Long userId = persistUser();
    Long otherUserId = persistUser();
    LocalDateTime now = LocalDateTime.now();

    persistToken(userId, "active", now.plusDays(7));
    persistToken(otherUserId, "other-active", now.plusDays(7));

    int updated = refreshTokenRepository.revokeAllActiveForUser(userId, now);
    // The @Modifying bulk update runs a direct SQL UPDATE that bypasses the persistence
    // context, so already-loaded managed entities go stale. Clear it before re-reading.
    entityManager.clear();

    assertThat(updated).isEqualTo(1);
    // Re-read to see the flushed update.
    assertThat(refreshTokenRepository.findByTokenHash("active").orElseThrow().getRevokedAt())
        .isNotNull();
    assertThat(refreshTokenRepository.findByTokenHash("other-active").orElseThrow().getRevokedAt())
        .isNull();
  }
}
