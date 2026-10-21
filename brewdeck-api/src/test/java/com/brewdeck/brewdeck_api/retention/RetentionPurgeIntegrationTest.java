package com.brewdeck.brewdeck_api.retention;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.brewdeck.brewdeck_api.auth.User;
import com.brewdeck.brewdeck_api.auth.UserRepository;
import com.brewdeck.brewdeck_api.auth.refresh.RefreshToken;
import com.brewdeck.brewdeck_api.auth.refresh.RefreshTokenRepository;
import com.brewdeck.brewdeck_api.auth.verification.EmailVerificationMailPort;
import com.brewdeck.brewdeck_api.auth.verification.EmailVerificationTokenRepository;
import com.brewdeck.brewdeck_api.coffee.Coffee;
import com.brewdeck.brewdeck_api.coffee.CoffeeRepository;
import com.brewdeck.brewdeck_api.common.PostgresIntegrationTest;
import com.brewdeck.brewdeck_api.common.security.SecureTokens;
import com.brewdeck.brewdeck_api.featureflag.FeatureFlagAdminService;
import com.brewdeck.brewdeck_api.featureflag.FeatureKeys;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

/**
 * The retention purge against Postgres (ADR-021). Accounts are backdated rather than the clock
 * moved forward, so accounts other test classes create minutes earlier are never candidates.
 */
@SpringBootTest
@ActiveProfiles("test")
class RetentionPurgeIntegrationTest extends PostgresIntegrationTest {

  @Autowired private RetentionPurgeService purgeService;
  @Autowired private UserRepository userRepository;
  @Autowired private CoffeeRepository coffeeRepository;
  @Autowired private RefreshTokenRepository refreshTokenRepository;
  @Autowired private EmailVerificationTokenRepository emailVerificationTokenRepository;
  @Autowired private FeatureFlagAdminService featureFlagAdminService;
  @MockitoSpyBean private EmailVerificationMailPort mailPort;

  // Microseconds, as Postgres stores them, so timestamps read back compare equal.
  private final Instant now = Instant.now().truncatedTo(ChronoUnit.MICROS);

  @BeforeEach
  void enforceVerification() {
    setVerificationRequired(true);
  }

  @AfterEach
  void restoreFlag() {
    setVerificationRequired(false);
  }

  private void setVerificationRequired(boolean enabled) {
    featureFlagAdminService.setEnabled(
        FeatureKeys.REQUIRE_EMAIL_VERIFICATION, "test", enabled, "integration-test");
  }

  private User account(String prefix, boolean verified, Duration age, Duration remindedAgo) {
    return userRepository.save(
        User.builder()
            .email(prefix + "-" + UUID.randomUUID() + "@example.com")
            .passwordHash("{argon2}not-used")
            .emailVerified(verified)
            .createdAt(now.minus(age))
            .retentionReminderSentAt(remindedAgo == null ? null : now.minus(remindedAgo))
            .build());
  }

  private RefreshToken refreshToken(User user, Duration expiredAgo) {
    return refreshTokenRepository.save(
        RefreshToken.builder()
            .userId(user.getId())
            .tokenHash(SecureTokens.sha256Hex(UUID.randomUUID().toString()))
            .expiresAt(now.minus(expiredAgo))
            .createdAt(now.minus(expiredAgo).minus(Duration.ofDays(7)))
            .build());
  }

  @Test
  void dayFive_remindsWithAFreshLink_andTheDeletionDate() {
    User fresh = account("fresh", false, Duration.ofDays(1), null);
    User dayFive = account("day-five", false, Duration.ofDays(5).plusHours(1), null);

    purgeService.run(now);

    User reminded = userRepository.findById(dayFive.getId()).orElseThrow();
    assertThat(reminded.getRetentionReminderSentAt()).isEqualTo(now);
    assertThat(emailVerificationTokenRepository.findByUserIdAndUsedAtIsNull(dayFive.getId()))
        .hasSize(1);
    // The reminder always promises at least 2 more days.
    verify(mailPort)
        .sendRetentionReminder(
            eq(dayFive.getEmail()), anyString(), eq(now.plus(Duration.ofDays(2))), any());
    assertThat(userRepository.findById(fresh.getId()).orElseThrow().getRetentionReminderSentAt())
        .isNull();
    verify(mailPort, never()).sendRetentionReminder(eq(fresh.getEmail()), any(), any(), any());
  }

  @Test
  void afterDaySeven_deletesRemindedAccountsWithTheirData_only_withTwoDaysNotice() {
    User due = account("due", false, Duration.ofDays(8), Duration.ofDays(3));
    coffeeRepository.save(Coffee.builder().name("Purged Coffee").owner(due).build());
    User recentlyReminded = account("recent", false, Duration.ofDays(8), Duration.ofDays(1));
    User neverReminded = account("never", false, Duration.ofDays(30), null);
    User verified = account("verified", true, Duration.ofDays(30), null);

    RetentionPurgeResult result = purgeService.run(now);

    assertThat(result.accountsChecked()).isTrue();
    assertThat(result.failures()).isZero();
    assertThat(userRepository.findById(due.getId())).isEmpty();
    assertThat(coffeeRepository.countByOwnerId(due.getId())).isZero();
    assertThat(userRepository.findById(recentlyReminded.getId())).isPresent();
    // An old account the purge never warned gets the reminder first, not the deletion.
    assertThat(
            userRepository
                .findById(neverReminded.getId())
                .orElseThrow()
                .getRetentionReminderSentAt())
        .isEqualTo(now);
    assertThat(userRepository.findById(verified.getId()).orElseThrow().getRetentionReminderSentAt())
        .isNull();
  }

  @Test
  void secondRun_isANoOp_forTheSameAccounts() {
    User dayFive = account("twice", false, Duration.ofDays(6), null);

    purgeService.run(now);
    purgeService.run(now);

    verify(mailPort, times(1)).sendRetentionReminder(eq(dayFive.getEmail()), any(), any(), any());
  }

  @Test
  void verificationNotEnforced_keepsUnverifiedAccounts_butStillPurgesTokens() {
    setVerificationRequired(false);
    User due = account("kept", false, Duration.ofDays(30), Duration.ofDays(10));
    User owner = account("tokens", true, Duration.ofDays(60), null);
    RefreshToken old = refreshToken(owner, Duration.ofDays(31));
    RefreshToken recent = refreshToken(owner, Duration.ofDays(29));

    RetentionPurgeResult result = purgeService.run(now);

    assertThat(result.accountsChecked()).isFalse();
    assertThat(userRepository.findById(due.getId())).isPresent();
    assertThat(refreshTokenRepository.findById(old.getId())).isEmpty();
    assertThat(refreshTokenRepository.findById(recent.getId())).isPresent();
    assertThat(result.tokensDeleted()).isGreaterThanOrEqualTo(1);
  }
}
