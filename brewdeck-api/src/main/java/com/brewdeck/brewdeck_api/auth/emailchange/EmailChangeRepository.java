package com.brewdeck.brewdeck_api.auth.emailchange;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EmailChangeRepository extends JpaRepository<EmailChange, Long> {

  Optional<EmailChange> findByConfirmTokenHash(String confirmTokenHash);

  Optional<EmailChange> findByCancelTokenHash(String cancelTokenHash);

  List<EmailChange> findByUserIdAndConfirmedAtIsNullAndCancelledAtIsNull(Long userId);

  /**
   * Confirms the request only if it is still pending and unexpired. Concurrent confirms (or a
   * confirm racing a cancel) serialize on the row lock: exactly one gets 1, the rest get 0.
   */
  @Modifying
  @Query(
      "UPDATE EmailChange c SET c.confirmedAt = :now WHERE c.id = :id AND c.confirmedAt IS NULL"
          + " AND c.cancelledAt IS NULL AND c.expiresAt > :now")
  int confirmIfPending(@Param("id") Long id, @Param("now") Instant now);

  /** Cancels the request only if it is still pending and unexpired; same locking as confirm. */
  @Modifying
  @Query(
      "UPDATE EmailChange c SET c.cancelledAt = :now WHERE c.id = :id AND c.confirmedAt IS NULL"
          + " AND c.cancelledAt IS NULL AND c.expiresAt > :now")
  int cancelIfPending(@Param("id") Long id, @Param("now") Instant now);
}
