package com.brewdeck.brewdeck_api.auth.refresh;

import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

  Optional<RefreshToken> findByTokenHash(String tokenHash);

  /** Revokes every still-active refresh token for the user. Returns the number of rows updated. */
  @Modifying
  @Query(
      "UPDATE RefreshToken t SET t.revokedAt = :now "
          + "WHERE t.userId = :userId AND t.usedAt IS NULL "
          + "AND t.revokedAt IS NULL AND t.expiresAt > :now")
  int revokeAllActiveForUser(@Param("userId") Long userId, @Param("now") LocalDateTime now);
}
