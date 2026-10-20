package com.brewdeck.brewdeck_api.auth.emailchange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.brewdeck.brewdeck_api.auth.CurrentUserProvider;
import com.brewdeck.brewdeck_api.auth.InvalidCurrentPasswordException;
import com.brewdeck.brewdeck_api.auth.Language;
import com.brewdeck.brewdeck_api.auth.User;
import com.brewdeck.brewdeck_api.auth.UserRepository;
import com.brewdeck.brewdeck_api.auth.refresh.RefreshTokenService;
import com.brewdeck.brewdeck_api.common.ratelimit.RateLimitExceededException;
import com.brewdeck.brewdeck_api.common.ratelimit.RateLimitRule;
import com.brewdeck.brewdeck_api.common.ratelimit.RateLimiter;
import com.brewdeck.brewdeck_api.common.security.SecureTokens;
import com.brewdeck.brewdeck_api.featureflag.FeatureDisabledException;
import com.brewdeck.brewdeck_api.featureflag.FeatureFlagService;
import com.brewdeck.brewdeck_api.featureflag.FeatureKeys;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class EmailChangeServiceTest {

  private static final String OLD_EMAIL = "old@example.com";
  private static final String NEW_EMAIL = "new@example.com";
  private static final String PASSWORD = "brew-test-pass-123";
  private static final Locale SPANISH = Locale.forLanguageTag("es");

  @Mock private EmailChangeRepository emailChangeRepository;
  @Mock private UserRepository userRepository;
  @Mock private CurrentUserProvider currentUserProvider;
  @Mock private PasswordEncoder passwordEncoder;
  @Mock private RefreshTokenService refreshTokenService;
  @Mock private EmailChangeMailPort mailPort;
  @Mock private RateLimiter rateLimiter;
  @Mock private FeatureFlagService featureFlagService;

  @InjectMocks private EmailChangeService service;

  private static User user() {
    return User.builder()
        .id(7L)
        .email(OLD_EMAIL)
        .passwordHash("{argon2}hash")
        .language(Language.ES)
        .createdAt(Instant.now())
        .build();
  }

  private User currentUserWithPassword() {
    User user = user();
    when(currentUserProvider.require()).thenReturn(user);
    when(passwordEncoder.matches(PASSWORD, user.getPasswordHash())).thenReturn(true);
    return user;
  }

  private static EmailChange pending(String rawConfirm, String rawCancel) {
    return EmailChange.builder()
        .id(11L)
        .userId(7L)
        .newEmail(NEW_EMAIL)
        .confirmTokenHash(SecureTokens.sha256Hex(rawConfirm))
        .cancelTokenHash(SecureTokens.sha256Hex(rawCancel))
        .expiresAt(Instant.now().plus(Duration.ofHours(1)))
        .createdAt(Instant.now())
        .build();
  }

  @Test
  void request_storesHashedTokens_andMailsBothAddresses() {
    currentUserWithPassword();
    when(userRepository.findByEmail(NEW_EMAIL)).thenReturn(Optional.empty());
    EmailChange earlier = pending("earlier-confirm", "earlier-cancel");
    when(emailChangeRepository.findByUserIdAndConfirmedAtIsNullAndCancelledAtIsNull(7L))
        .thenReturn(List.of(earlier));

    service.request(new RequestEmailChangeRequest(NEW_EMAIL, PASSWORD));

    assertThat(earlier.getCancelledAt()).as("an earlier pending request is superseded").isNotNull();
    ArgumentCaptor<EmailChange> saved = ArgumentCaptor.forClass(EmailChange.class);
    verify(emailChangeRepository).save(saved.capture());
    ArgumentCaptor<String> confirmToken = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<String> cancelToken = ArgumentCaptor.forClass(String.class);
    verify(mailPort).sendConfirmLink(eq(NEW_EMAIL), confirmToken.capture(), eq(SPANISH));
    verify(mailPort)
        .sendChangeRequestedNotice(
            eq(OLD_EMAIL), eq("n***@example.com"), cancelToken.capture(), eq(SPANISH));

    EmailChange change = saved.getValue();
    assertThat(change.getUserId()).isEqualTo(7L);
    assertThat(change.getNewEmail()).isEqualTo(NEW_EMAIL);
    assertThat(change.getConfirmTokenHash())
        .isEqualTo(SecureTokens.sha256Hex(confirmToken.getValue()));
    assertThat(change.getCancelTokenHash())
        .isEqualTo(SecureTokens.sha256Hex(cancelToken.getValue()));
    assertThat(Duration.between(change.getCreatedAt(), change.getExpiresAt()))
        .isEqualTo(Duration.ofHours(1));
  }

  @Test
  void request_countsTheAttemptBeforeCheckingThePassword() {
    User user = user();
    when(currentUserProvider.require()).thenReturn(user);
    when(passwordEncoder.matches("wrong-password-123", user.getPasswordHash())).thenReturn(false);

    assertThatThrownBy(
            () -> service.request(new RequestEmailChangeRequest(NEW_EMAIL, "wrong-password-123")))
        .isInstanceOf(InvalidCurrentPasswordException.class);

    InOrder order = inOrder(rateLimiter, passwordEncoder);
    order.verify(rateLimiter).requireAllowed(RateLimitRule.EMAIL_CHANGE_USER, "7");
    order.verify(passwordEncoder).matches(anyString(), anyString());
    verifyNoInteractions(emailChangeRepository, mailPort);
  }

  @Test
  void request_overTheLimit_doesNotCheckThePassword() {
    when(currentUserProvider.require()).thenReturn(user());
    doThrow(new RateLimitExceededException(Duration.ofMinutes(30)))
        .when(rateLimiter)
        .requireAllowed(RateLimitRule.EMAIL_CHANGE_USER, "7");

    assertThatThrownBy(() -> service.request(new RequestEmailChangeRequest(NEW_EMAIL, PASSWORD)))
        .isInstanceOf(RateLimitExceededException.class);

    verifyNoInteractions(passwordEncoder, emailChangeRepository, mailPort);
  }

  @Test
  void request_sameEmail_isRejected() {
    currentUserWithPassword();

    assertThatThrownBy(() -> service.request(new RequestEmailChangeRequest(OLD_EMAIL, PASSWORD)))
        .isInstanceOf(SameEmailException.class);

    verifyNoInteractions(emailChangeRepository, mailPort);
  }

  @Test
  void request_takenAddress_storesNothing_andOnlyTellsTheNewAddress() {
    currentUserWithPassword();
    when(userRepository.findByEmail(NEW_EMAIL)).thenReturn(Optional.of(User.builder().build()));

    service.request(new RequestEmailChangeRequest(NEW_EMAIL, PASSWORD));

    verify(mailPort).sendAddressTakenNotice(NEW_EMAIL, SPANISH);
    verify(mailPort, never()).sendConfirmLink(anyString(), anyString(), any());
    verify(mailPort, never())
        .sendChangeRequestedNotice(anyString(), anyString(), anyString(), any());
    verifyNoInteractions(emailChangeRepository);
  }

  @Test
  void request_flagOff_doesNothing() {
    doThrow(new FeatureDisabledException(FeatureKeys.EMAIL_CHANGE, HttpStatus.NOT_FOUND))
        .when(featureFlagService)
        .requireEnabled(FeatureKeys.EMAIL_CHANGE);

    assertThatThrownBy(() -> service.request(new RequestEmailChangeRequest(NEW_EMAIL, PASSWORD)))
        .isInstanceOf(FeatureDisabledException.class);

    verifyNoInteractions(currentUserProvider, emailChangeRepository, mailPort);
  }

  @Test
  void confirm_switchesTheEmail_verifiesIt_andClosesEverySession() {
    User user = user();
    EmailChange change = pending("confirm-raw", "cancel-raw");
    when(emailChangeRepository.findByConfirmTokenHash(SecureTokens.sha256Hex("confirm-raw")))
        .thenReturn(Optional.of(change));
    when(emailChangeRepository.confirmIfPending(eq(11L), any())).thenReturn(1);
    when(userRepository.findById(7L)).thenReturn(Optional.of(user));
    when(userRepository.findByEmail(NEW_EMAIL)).thenReturn(Optional.empty());

    service.confirm("confirm-raw");

    assertThat(user.getEmail()).isEqualTo(NEW_EMAIL);
    assertThat(user.isEmailVerified()).isTrue();
    assertThat(user.getTokenVersion()).isEqualTo(1);
    verify(userRepository).saveAndFlush(user);
    verify(refreshTokenService).revokeAllForUser(7L);
    verify(mailPort).sendEmailChangedNotice(OLD_EMAIL, "n***@example.com", SPANISH);
  }

  @Test
  void confirm_unknownToken_isInvalid() {
    when(emailChangeRepository.findByConfirmTokenHash(anyString())).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.confirm("nope"))
        .isInstanceOf(InvalidEmailChangeTokenException.class);
  }

  @Test
  void confirm_notPendingAnymore_isInvalid() {
    when(emailChangeRepository.findByConfirmTokenHash(anyString()))
        .thenReturn(Optional.of(pending("confirm-raw", "cancel-raw")));
    when(emailChangeRepository.confirmIfPending(eq(11L), any())).thenReturn(0);

    assertThatThrownBy(() -> service.confirm("confirm-raw"))
        .isInstanceOf(InvalidEmailChangeTokenException.class);

    verifyNoInteractions(refreshTokenService, mailPort);
  }

  @Test
  void confirm_addressTakenMeanwhile_isInvalid_andChangesNothing() {
    User user = user();
    when(emailChangeRepository.findByConfirmTokenHash(anyString()))
        .thenReturn(Optional.of(pending("confirm-raw", "cancel-raw")));
    when(emailChangeRepository.confirmIfPending(eq(11L), any())).thenReturn(1);
    when(userRepository.findById(7L)).thenReturn(Optional.of(user));
    when(userRepository.findByEmail(NEW_EMAIL)).thenReturn(Optional.of(User.builder().build()));

    assertThatThrownBy(() -> service.confirm("confirm-raw"))
        .isInstanceOf(InvalidEmailChangeTokenException.class);

    assertThat(user.getEmail()).isEqualTo(OLD_EMAIL);
    verifyNoInteractions(refreshTokenService, mailPort);
  }

  @Test
  void confirm_uniqueViolationOnSave_isInvalid() {
    User user = user();
    when(emailChangeRepository.findByConfirmTokenHash(anyString()))
        .thenReturn(Optional.of(pending("confirm-raw", "cancel-raw")));
    when(emailChangeRepository.confirmIfPending(eq(11L), any())).thenReturn(1);
    when(userRepository.findById(7L)).thenReturn(Optional.of(user));
    when(userRepository.findByEmail(NEW_EMAIL)).thenReturn(Optional.empty());
    when(userRepository.saveAndFlush(user))
        .thenThrow(new DataIntegrityViolationException("users_email_key"));

    assertThatThrownBy(() -> service.confirm("confirm-raw"))
        .isInstanceOf(InvalidEmailChangeTokenException.class);

    verifyNoInteractions(refreshTokenService, mailPort);
  }

  @Test
  void cancel_closesEverySession_evenWithTheFlagOff() {
    User user = user();
    when(emailChangeRepository.findByCancelTokenHash(SecureTokens.sha256Hex("cancel-raw")))
        .thenReturn(Optional.of(pending("confirm-raw", "cancel-raw")));
    when(emailChangeRepository.cancelIfPending(eq(11L), any())).thenReturn(1);
    when(userRepository.findById(7L)).thenReturn(Optional.of(user));

    service.cancel("cancel-raw");

    assertThat(user.getEmail()).isEqualTo(OLD_EMAIL);
    assertThat(user.getTokenVersion()).isEqualTo(1);
    verify(userRepository).save(user);
    verify(refreshTokenService).revokeAllForUser(7L);
    verifyNoInteractions(featureFlagService);
  }

  @Test
  void cancel_notPendingAnymore_isInvalid() {
    when(emailChangeRepository.findByCancelTokenHash(anyString()))
        .thenReturn(Optional.of(pending("confirm-raw", "cancel-raw")));
    when(emailChangeRepository.cancelIfPending(eq(11L), any())).thenReturn(0);

    assertThatThrownBy(() -> service.cancel("cancel-raw"))
        .isInstanceOf(InvalidEmailChangeTokenException.class);

    verifyNoInteractions(refreshTokenService);
  }

  @Test
  void mask_keepsTheFirstCharacterAndTheDomain() {
    assertThat(EmailChangeService.mask("juan@example.com")).isEqualTo("j***@example.com");
    assertThat(EmailChangeService.mask("j@example.com")).isEqualTo("j***@example.com");
    assertThat(EmailChangeService.mask("not-an-email")).isEqualTo("***");
  }
}
