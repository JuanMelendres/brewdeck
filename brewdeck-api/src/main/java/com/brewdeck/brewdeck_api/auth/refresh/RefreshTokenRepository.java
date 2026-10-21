package com.brewdeck.brewdeck_api.auth.refresh;

import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

  Optional<RefreshToken> findByTokenHash(String tokenHash);

  /**
   * Marks the token used only if it is still unused and not revoked. The row lock makes this the
   * single point where concurrent rotations of one token are decided: exactly one gets 1, the rest
   * get 0.
   */
  @Modifying
  @Query(
      "UPDATE RefreshToken t SET t.usedAt = :now "
          + "WHERE t.id = :id AND t.usedAt IS NULL AND t.revokedAt IS NULL")
  int markUsedIfActive(@Param("id") Long id, @Param("now") Instant now);

  /** Revokes every still-active refresh token for the user. Returns the number of rows updated. */
  @Modifying
  @Query(
      "UPDATE RefreshToken t SET t.revokedAt = :now "
          + "WHERE t.userId = :userId AND t.usedAt IS NULL "
          + "AND t.revokedAt IS NULL AND t.expiresAt > :now")
  int revokeAllActiveForUser(@Param("userId") Long userId, @Param("now") Instant now);

  /** Account deletion (ADR-020). */
  @Modifying
  @Query("DELETE FROM RefreshToken t WHERE t.userId = :userId")
  int deleteAllByUserId(@Param("userId") Long userId);

  /**
   * Retention purge (ADR-021): deletes up to {@code limit} rows that expired before the cutoff.
   * Returns the number deleted; fewer than the limit means none are left.
   */
  @Modifying
  @Query(
      value =
          "DELETE FROM refresh_tokens WHERE id IN "
              + "(SELECT id FROM refresh_tokens WHERE expires_at < :cutoff ORDER BY id LIMIT :limit)",
      nativeQuery = true)
  int deleteExpiredBefore(@Param("cutoff") Instant cutoff, @Param("limit") int limit);
}
