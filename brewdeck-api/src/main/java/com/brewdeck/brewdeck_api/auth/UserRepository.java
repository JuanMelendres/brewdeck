package com.brewdeck.brewdeck_api.auth;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {
  Optional<User> findByEmail(String email);

  /** Retention purge (ADR-021): unverified accounts created before the cutoff, not yet reminded. */
  @Query(
      "SELECT u.id FROM User u WHERE u.emailVerified = false "
          + "AND u.retentionReminderSentAt IS NULL AND u.createdAt < :createdBefore "
          + "ORDER BY u.id")
  List<Long> findUnverifiedIdsToRemind(@Param("createdBefore") Instant createdBefore, Limit limit);

  /** Retention purge (ADR-021): unverified accounts past both the age and the reminder cutoffs. */
  @Query(
      "SELECT u.id FROM User u WHERE u.emailVerified = false "
          + "AND u.createdAt < :createdBefore AND u.retentionReminderSentAt < :remindedBefore "
          + "ORDER BY u.id")
  List<Long> findUnverifiedIdsToDelete(
      @Param("createdBefore") Instant createdBefore,
      @Param("remindedBefore") Instant remindedBefore,
      Limit limit);

  /** Locks the row, so verifying the email and the purge cannot interleave. */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT u FROM User u WHERE u.id = :id")
  Optional<User> findByIdForUpdate(@Param("id") Long id);
}
