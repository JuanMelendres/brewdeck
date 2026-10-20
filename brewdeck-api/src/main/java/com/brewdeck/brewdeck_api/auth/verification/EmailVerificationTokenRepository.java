package com.brewdeck.brewdeck_api.auth.verification;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EmailVerificationTokenRepository
    extends JpaRepository<EmailVerificationToken, Long> {

  Optional<EmailVerificationToken> findByTokenHash(String tokenHash);

  List<EmailVerificationToken> findByUserIdAndUsedAtIsNull(Long userId);

  /**
   * Marks the token used only if it is still unused. Concurrent requests with the same token
   * serialize on the row lock: exactly one gets 1, the rest get 0.
   */
  @Modifying
  @Query(
      "UPDATE EmailVerificationToken t SET t.usedAt = :now WHERE t.id = :id AND t.usedAt IS NULL")
  int markUsedIfUnused(@Param("id") Long id, @Param("now") Instant now);

  /** Account deletion (ADR-020). */
  @Modifying
  @Query("DELETE FROM EmailVerificationToken t WHERE t.userId = :userId")
  int deleteAllByUserId(@Param("userId") Long userId);
}
