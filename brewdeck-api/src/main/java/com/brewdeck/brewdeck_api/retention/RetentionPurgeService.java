package com.brewdeck.brewdeck_api.retention;

import com.brewdeck.brewdeck_api.account.AccountEraser;
import com.brewdeck.brewdeck_api.auth.User;
import com.brewdeck.brewdeck_api.auth.UserRepository;
import com.brewdeck.brewdeck_api.auth.emailchange.EmailChangeRepository;
import com.brewdeck.brewdeck_api.auth.refresh.RefreshTokenRepository;
import com.brewdeck.brewdeck_api.auth.reset.PasswordResetTokenRepository;
import com.brewdeck.brewdeck_api.auth.verification.EmailVerificationService;
import com.brewdeck.brewdeck_api.auth.verification.EmailVerificationTokenRepository;
import com.brewdeck.brewdeck_api.featureflag.FeatureFlagService;
import com.brewdeck.brewdeck_api.featureflag.FeatureKeys;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Daily retention purge (AUTH-06, ADR-021), run by the Cloud Run Job through {@link
 * RetentionPurgeRunner}.
 *
 * <ul>
 *   <li>Where email verification is enforced: reminds unverified accounts on day 5 and deletes them
 *       after day 7, at least 2 days after the reminder.
 *   <li>Everywhere: deletes token rows 30 days after they expired.
 * </ul>
 *
 * Every step is idempotent and bounded, and each account gets its own transaction, so one bad row
 * cannot block the rest and a rerun picks up where a failed one stopped.
 */
@Service
@Slf4j
public class RetentionPurgeService {

  static final Duration REMIND_AFTER = Duration.ofDays(5);
  static final Duration DELETE_AFTER = Duration.ofDays(7);
  static final Duration MIN_NOTICE = Duration.ofDays(2);
  static final Duration TOKEN_RETENTION = Duration.ofDays(30);
  static final int MAX_ACCOUNTS = 500;
  static final int TOKEN_BATCH = 1000;

  private final UserRepository userRepository;
  private final AccountEraser accountEraser;
  private final EmailVerificationService emailVerificationService;
  private final FeatureFlagService featureFlagService;
  private final RefreshTokenRepository refreshTokenRepository;
  private final PasswordResetTokenRepository passwordResetTokenRepository;
  private final EmailVerificationTokenRepository emailVerificationTokenRepository;
  private final EmailChangeRepository emailChangeRepository;
  private final TransactionTemplate transaction;
  private final Clock clock;

  @SuppressWarnings("PMD.ExcessiveParameterList") // one repository per token table
  public RetentionPurgeService(
      UserRepository userRepository,
      AccountEraser accountEraser,
      EmailVerificationService emailVerificationService,
      FeatureFlagService featureFlagService,
      RefreshTokenRepository refreshTokenRepository,
      PasswordResetTokenRepository passwordResetTokenRepository,
      EmailVerificationTokenRepository emailVerificationTokenRepository,
      EmailChangeRepository emailChangeRepository,
      PlatformTransactionManager transactionManager,
      Clock retentionClock) {
    this.userRepository = userRepository;
    this.accountEraser = accountEraser;
    this.emailVerificationService = emailVerificationService;
    this.featureFlagService = featureFlagService;
    this.refreshTokenRepository = refreshTokenRepository;
    this.passwordResetTokenRepository = passwordResetTokenRepository;
    this.emailVerificationTokenRepository = emailVerificationTokenRepository;
    this.emailChangeRepository = emailChangeRepository;
    this.transaction = new TransactionTemplate(transactionManager);
    this.clock = retentionClock;
  }

  public RetentionPurgeResult run() {
    return run(clock.instant());
  }

  public RetentionPurgeResult run(Instant now) {
    boolean accountsChecked = featureFlagService.isEnabled(FeatureKeys.REQUIRE_EMAIL_VERIFICATION);
    Tally deleted = Tally.NONE;
    Tally reminded = Tally.NONE;
    if (accountsChecked) {
      deleted = forEachAccount(deleteCandidates(now), this::delete);
      reminded = forEachAccount(remindCandidates(now), user -> remind(user, now));
    } else {
      log.info("Email verification is not enforced here; unverified accounts are kept");
    }
    long tokens = purgeTokens(now.minus(TOKEN_RETENTION));
    RetentionPurgeResult result =
        new RetentionPurgeResult(
            accountsChecked,
            reminded.done(),
            deleted.done(),
            reminded.failures() + deleted.failures(),
            tokens);
    log.info(
        "Retention purge done: reminded={}, deleted={}, failures={}, tokensDeleted={}",
        result.reminded(),
        result.deleted(),
        result.failures(),
        result.tokensDeleted());
    return result;
  }

  private List<Long> deleteCandidates(Instant now) {
    return userRepository.findUnverifiedIdsToDelete(
        now.minus(DELETE_AFTER), now.minus(MIN_NOTICE), Limit.of(MAX_ACCOUNTS));
  }

  private List<Long> remindCandidates(Instant now) {
    return userRepository.findUnverifiedIdsToRemind(
        now.minus(REMIND_AFTER), Limit.of(MAX_ACCOUNTS));
  }

  /** Deletes the account unless it was verified since it was selected. */
  private boolean delete(User user) {
    if (user.isEmailVerified()) {
      return false;
    }
    accountEraser.erase(user);
    log.info("Deleted unverified account id={}", user.getId());
    return true;
  }

  /** Reminds the account unless it was verified or reminded since it was selected. */
  private boolean remind(User user, Instant now) {
    if (user.isEmailVerified() || user.getRetentionReminderSentAt() != null) {
      return false;
    }
    // Reminded accounts are at least 5 days old, so day 7 always comes before the 2-day notice
    // ends.
    emailVerificationService.issueRetentionReminder(user, now, now.plus(MIN_NOTICE));
    return true;
  }

  /**
   * Runs {@code action} on each account in its own transaction, with the user row locked so a
   * concurrent email verification waits.
   */
  @SuppressWarnings("PMD.AvoidCatchingGenericException") // one bad account must not stop the run
  private Tally forEachAccount(List<Long> ids, Function<User, Boolean> action) {
    int done = 0;
    int failures = 0;
    for (Long id : ids) {
      try {
        Boolean acted =
            transaction.execute(
                status -> {
                  Optional<User> user = userRepository.findByIdForUpdate(id);
                  return user.isPresent() && action.apply(user.get());
                });
        if (Boolean.TRUE.equals(acted)) {
          done++;
        }
      } catch (RuntimeException e) {
        failures++;
        log.error("Retention purge failed for user id={}", id, e);
      }
    }
    return new Tally(done, failures);
  }

  private long purgeTokens(Instant cutoff) {
    return purgeTable("refresh_tokens", refreshTokenRepository::deleteExpiredBefore, cutoff)
        + purgeTable(
            "password_reset_tokens", passwordResetTokenRepository::deleteExpiredBefore, cutoff)
        + purgeTable(
            "email_verification_tokens",
            emailVerificationTokenRepository::deleteExpiredBefore,
            cutoff)
        + purgeTable("email_change_requests", emailChangeRepository::deleteExpiredBefore, cutoff);
  }

  /** Deletes in batches, one transaction each, until a batch comes back short. */
  private long purgeTable(
      String table, BiFunction<Instant, Integer, Integer> deleteBatch, Instant cutoff) {
    long total = 0;
    int batch;
    do {
      Integer deleted = transaction.execute(status -> deleteBatch.apply(cutoff, TOKEN_BATCH));
      batch = deleted == null ? 0 : deleted;
      total += batch;
    } while (batch == TOKEN_BATCH);
    if (total > 0) {
      log.info("Deleted {} expired rows from {}", total, table);
    }
    return total;
  }

  private record Tally(int done, int failures) {
    static final Tally NONE = new Tally(0, 0);
  }
}
