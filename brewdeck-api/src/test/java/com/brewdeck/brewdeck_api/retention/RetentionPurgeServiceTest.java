package com.brewdeck.brewdeck_api.retention;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.transaction.PlatformTransactionManager;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RetentionPurgeServiceTest {

  private static final Instant NOW = Instant.parse("2026-10-20T12:00:00Z");

  @Mock private UserRepository userRepository;
  @Mock private AccountEraser accountEraser;
  @Mock private EmailVerificationService emailVerificationService;
  @Mock private FeatureFlagService featureFlagService;
  @Mock private RefreshTokenRepository refreshTokenRepository;
  @Mock private PasswordResetTokenRepository passwordResetTokenRepository;
  @Mock private EmailVerificationTokenRepository emailVerificationTokenRepository;
  @Mock private EmailChangeRepository emailChangeRepository;
  @Mock private PlatformTransactionManager transactionManager;

  private RetentionPurgeService service;

  @BeforeEach
  void setUp() {
    service =
        new RetentionPurgeService(
            userRepository,
            accountEraser,
            emailVerificationService,
            featureFlagService,
            refreshTokenRepository,
            passwordResetTokenRepository,
            emailVerificationTokenRepository,
            emailChangeRepository,
            transactionManager,
            Clock.fixed(NOW, ZoneOffset.UTC));
    when(featureFlagService.isEnabled(FeatureKeys.REQUIRE_EMAIL_VERIFICATION)).thenReturn(true);
  }

  private User unverified(long id, Duration age) {
    User user = User.builder().id(id).email(id + "@example.com").createdAt(NOW.minus(age)).build();
    when(userRepository.findByIdForUpdate(id)).thenReturn(Optional.of(user));
    return user;
  }

  @Test
  void run_usesTheClock_forEveryCutoff() {
    service.run();

    verify(userRepository)
        .findUnverifiedIdsToDelete(
            eq(NOW.minus(Duration.ofDays(7))), eq(NOW.minus(Duration.ofDays(2))), any());
    verify(userRepository).findUnverifiedIdsToRemind(eq(NOW.minus(Duration.ofDays(5))), any());
    verify(refreshTokenRepository).deleteExpiredBefore(NOW.minus(Duration.ofDays(30)), 1000);
  }

  @Test
  void verificationNotEnforced_touchesNoAccount() {
    when(featureFlagService.isEnabled(FeatureKeys.REQUIRE_EMAIL_VERIFICATION)).thenReturn(false);

    RetentionPurgeResult result = service.run(NOW);

    assertThat(result.accountsChecked()).isFalse();
    verify(userRepository, never()).findUnverifiedIdsToDelete(any(), any(), any());
    verify(userRepository, never()).findUnverifiedIdsToRemind(any(), any());
    verify(emailChangeRepository).deleteExpiredBefore(NOW.minus(Duration.ofDays(30)), 1000);
  }

  @Test
  void reminder_announcesDeletionTwoDaysOut_evenForOldAccounts() {
    User old = unverified(1L, Duration.ofDays(40));
    User dayFive = unverified(2L, Duration.ofDays(5).plusHours(1));
    when(userRepository.findUnverifiedIdsToRemind(any(), any())).thenReturn(List.of(1L, 2L));

    RetentionPurgeResult result = service.run(NOW);

    assertThat(result.reminded()).isEqualTo(2);
    verify(emailVerificationService).issueRetentionReminder(old, NOW, NOW.plus(Duration.ofDays(2)));
    verify(emailVerificationService)
        .issueRetentionReminder(dayFive, NOW, NOW.plus(Duration.ofDays(2)));
  }

  @Test
  void accountVerifiedSinceSelection_isNotDeleted() {
    User user = unverified(3L, Duration.ofDays(9));
    user.setEmailVerified(true);
    when(userRepository.findUnverifiedIdsToDelete(any(), any(), any())).thenReturn(List.of(3L));

    RetentionPurgeResult result = service.run(NOW);

    assertThat(result.deleted()).isZero();
    verify(accountEraser, never()).erase(any());
  }

  @Test
  void oneFailingAccount_isCounted_andTheRestStillRun() {
    User broken = unverified(4L, Duration.ofDays(9));
    User fine = unverified(5L, Duration.ofDays(9));
    when(userRepository.findUnverifiedIdsToDelete(any(), any(), any())).thenReturn(List.of(4L, 5L));
    doThrow(new IllegalStateException("constraint")).when(accountEraser).erase(broken);

    RetentionPurgeResult result = service.run(NOW);

    assertThat(result.failures()).isEqualTo(1);
    assertThat(result.deleted()).isEqualTo(1);
    verify(accountEraser).erase(fine);
  }

  @Test
  void tokens_areDeletedInBatches_untilOneComesBackShort() {
    when(refreshTokenRepository.deleteExpiredBefore(any(), anyInt())).thenReturn(1000, 1000, 7);

    RetentionPurgeResult result = service.run(NOW);

    assertThat(result.tokensDeleted()).isEqualTo(2007);
  }
}
