package com.brewdeck.brewdeck_api.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.brewdeck.brewdeck_api.auth.refresh.RefreshTokenService;
import com.brewdeck.brewdeck_api.common.config.PasswordEncoderConfig;
import com.brewdeck.brewdeck_api.common.ratelimit.RateLimitExceededException;
import com.brewdeck.brewdeck_api.common.ratelimit.RateLimitRule;
import com.brewdeck.brewdeck_api.common.ratelimit.RateLimiter;
import com.brewdeck.brewdeck_api.featureflag.FeatureFlagService;
import com.brewdeck.brewdeck_api.featureflag.FeatureKeys;
import com.brewdeck.brewdeck_api.legal.LegalTerms;
import com.brewdeck.brewdeck_api.legal.TermsNotAcceptedException;
import jakarta.persistence.EntityNotFoundException;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

  @Mock private FeatureFlagService featureFlagService;

  @Mock private UserRepository userRepository;
  @Mock private JwtService jwtService;

  @Mock
  private com.brewdeck.brewdeck_api.auth.verification.EmailVerificationService
      emailVerificationService;

  @Mock private RefreshTokenService refreshTokenService;

  private AuthService authService;

  @BeforeEach
  void setUp() {
    PasswordEncoder encoder = new BCryptPasswordEncoder();
    authService =
        new AuthService(
            userRepository,
            jwtService,
            encoder,
            emailVerificationService,
            refreshTokenService,
            new RateLimiter(false),
            featureFlagService);
  }

  private User stored(String email, String rawPassword) {
    return User.builder()
        .id(1L)
        .email(email)
        .passwordHash(new BCryptPasswordEncoder().encode(rawPassword))
        .createdAt(Instant.now())
        .build();
  }

  @Test
  void register_newEmail_createsTheAccountWithAHashedPassword_andSendsVerification() {
    when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());
    when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

    authService.register(new RegisterRequest("new@example.com", "brew-test-pass-001"));

    org.mockito.ArgumentCaptor<User> saved = org.mockito.ArgumentCaptor.forClass(User.class);
    verify(userRepository).save(saved.capture());
    assertThat(saved.getValue().getEmail()).isEqualTo("new@example.com");
    assertThat(
            new BCryptPasswordEncoder()
                .matches("brew-test-pass-001", saved.getValue().getPasswordHash()))
        .isTrue();
    verify(emailVerificationService).issueFor(saved.getValue());
    // No session at signup (AUTH-05.7).
    org.mockito.Mockito.verifyNoInteractions(refreshTokenService, jwtService);
  }

  @Test
  void register_existingEmail_notifiesTheOwner_andChangesNothing() {
    User existing = stored("taken@example.com", "brew-test-pass-001");
    when(userRepository.findByEmail("taken@example.com")).thenReturn(Optional.of(existing));

    authService.register(new RegisterRequest("taken@example.com", "other-password"));

    verify(emailVerificationService).sendAccountExistsNotice(existing);
    verify(emailVerificationService, never()).issueFor(any(User.class));
    verify(userRepository, never()).save(any(User.class));
    org.mockito.Mockito.verifyNoInteractions(refreshTokenService, jwtService);
  }

  @Test
  void register_appliesThePerEmailLimit_beforeLookingTheAccountUp() {
    RateLimiter limiter = org.mockito.Mockito.mock(RateLimiter.class);
    org.mockito.Mockito.doThrow(new RateLimitExceededException(Duration.ofMinutes(30)))
        .when(limiter)
        .requireAllowed(RateLimitRule.REGISTER_EMAIL, "flood@example.com");
    AuthService limited =
        new AuthService(
            userRepository,
            jwtService,
            new BCryptPasswordEncoder(),
            emailVerificationService,
            refreshTokenService,
            limiter,
            featureFlagService);

    assertThatThrownBy(
            () -> limited.register(new RegisterRequest("flood@example.com", "brew-test-pass-001")))
        .isInstanceOf(RateLimitExceededException.class);
    org.mockito.Mockito.verifyNoInteractions(userRepository, emailVerificationService);
  }

  @Test
  void register_succeedsEvenWhenVerificationIssueThrows() {
    when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());
    when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
    org.mockito.Mockito.doThrow(new RuntimeException("verification down"))
        .when(emailVerificationService)
        .issueFor(any(User.class));

    // The account is still created if verification issuance fails.
    authService.register(new RegisterRequest("new@example.com", "brew-test-pass-001"));

    verify(userRepository).save(any(User.class));
  }

  @Test
  void register_existingEmail_swallowsANoticeFailure() {
    User existing = stored("taken@example.com", "brew-test-pass-001");
    when(userRepository.findByEmail("taken@example.com")).thenReturn(Optional.of(existing));
    org.mockito.Mockito.doThrow(new RuntimeException("mail down"))
        .when(emailVerificationService)
        .sendAccountExistsNotice(existing);

    // Must not surface: an error here would tell the caller the email exists.
    authService.register(new RegisterRequest("taken@example.com", "brew-test-pass-001"));
  }

  @Test
  void login_returnsTokenWhenPasswordMatches() {
    when(userRepository.findByEmail("brewer@example.com"))
        .thenReturn(Optional.of(stored("brewer@example.com", "brew-test-pass-001")));
    when(jwtService.generateToken(any(User.class))).thenReturn("jwt-token");

    AuthResponse response =
        authService.login(new LoginRequest("brewer@example.com", "brew-test-pass-001")).response();

    assertThat(response.token()).isEqualTo("jwt-token");
  }

  @Test
  void login_upgradesALegacyBcryptHashToArgon2id() {
    User legacy =
        stored("brewer@example.com", "brew-test-pass-001"); // plain BCrypt, as before ADR-018
    when(userRepository.findByEmail("brewer@example.com")).thenReturn(Optional.of(legacy));
    when(jwtService.generateToken(any(User.class))).thenReturn("jwt-token");
    PasswordEncoder configured = new PasswordEncoderConfig().passwordEncoder();
    AuthService withArgon2 =
        new AuthService(
            userRepository,
            jwtService,
            configured,
            emailVerificationService,
            refreshTokenService,
            new RateLimiter(false),
            featureFlagService);

    withArgon2.login(new LoginRequest("brewer@example.com", "brew-test-pass-001"));

    verify(userRepository).save(legacy);
    assertThat(legacy.getPasswordHash()).startsWith("{argon2}");
    assertThat(configured.matches("brew-test-pass-001", legacy.getPasswordHash())).isTrue();
  }

  @Test
  void login_throwsWhenPasswordWrong() {
    when(userRepository.findByEmail("brewer@example.com"))
        .thenReturn(Optional.of(stored("brewer@example.com", "brew-test-pass-001")));

    assertThatThrownBy(() -> authService.login(new LoginRequest("brewer@example.com", "wrong")))
        .isInstanceOf(org.springframework.security.authentication.BadCredentialsException.class);
  }

  @Test
  void login_throwsWhenEmailUnknown() {
    when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

    assertThatThrownBy(
            () -> authService.login(new LoginRequest("ghost@example.com", "brew-test-pass-001")))
        .isInstanceOf(org.springframework.security.authentication.BadCredentialsException.class);
  }

  @Test
  void me_returnsUserWhenPresent() {
    when(userRepository.findByEmail("brewer@example.com"))
        .thenReturn(Optional.of(stored("brewer@example.com", "brew-test-pass-001")));

    UserResponse response = authService.me("brewer@example.com");

    assertThat(response.email()).isEqualTo("brewer@example.com");
  }

  @Test
  void me_throwsWhenMissing() {
    when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> authService.me("ghost@example.com"))
        .isInstanceOf(EntityNotFoundException.class);
  }

  @Test
  void updateProfile_setsDisplayNameAndReturnsUser() {
    when(userRepository.findByEmail("brewer@example.com"))
        .thenReturn(Optional.of(stored("brewer@example.com", "brew-test-pass-001")));
    when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

    UserResponse response =
        authService.updateProfile("brewer@example.com", new UpdateProfileRequest("Barista Bob"));

    assertThat(response.displayName()).isEqualTo("Barista Bob");
  }

  @Test
  void updateProfile_throwsWhenUserMissing() {
    when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

    assertThatThrownBy(
            () -> authService.updateProfile("ghost@example.com", new UpdateProfileRequest("X")))
        .isInstanceOf(EntityNotFoundException.class);
  }

  @Test
  void updateTheme_setsPreferenceAndKeepsDisplayName() {
    User user = stored("brewer@example.com", "brew-test-pass-001");
    user.setDisplayName("Barista Bob");
    when(userRepository.findByEmail("brewer@example.com")).thenReturn(Optional.of(user));
    when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

    UserResponse response =
        authService.updateTheme("brewer@example.com", new UpdateThemeRequest(ThemePreference.DARK));

    assertThat(response.themePreference()).isEqualTo(ThemePreference.DARK);
    assertThat(response.displayName()).isEqualTo("Barista Bob");
  }

  @Test
  void updateTheme_throwsWhenUserMissing() {
    when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

    assertThatThrownBy(
            () ->
                authService.updateTheme(
                    "ghost@example.com", new UpdateThemeRequest(ThemePreference.LIGHT)))
        .isInstanceOf(EntityNotFoundException.class);
  }

  @Test
  void updateLanguage_setsLanguage() {
    User user = stored("brewer@example.com", "brew-test-pass-001");
    when(userRepository.findByEmail("brewer@example.com")).thenReturn(Optional.of(user));
    when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

    UserResponse response =
        authService.updateLanguage("brewer@example.com", new UpdateLanguageRequest(Language.ES));

    assertThat(response.language()).isEqualTo(Language.ES);
  }

  @Test
  void changePassword_reencodesWhenCurrentMatches() {
    User user = stored("brewer@example.com", "brew-test-pass-001");
    String originalHash = user.getPasswordHash();
    when(userRepository.findByEmail("brewer@example.com")).thenReturn(Optional.of(user));
    when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

    authService.changePassword(
        "brewer@example.com",
        new ChangePasswordRequest("brew-test-pass-001", "brew-new-pass-0001"));

    assertThat(user.getPasswordHash()).isNotEqualTo(originalHash);
    assertThat(new BCryptPasswordEncoder().matches("brew-new-pass-0001", user.getPasswordHash()))
        .isTrue();
  }

  @Test
  void changePassword_revokesAllRefreshTokensForUser() {
    User user = stored("brewer@example.com", "brew-test-pass-001");
    when(userRepository.findByEmail("brewer@example.com")).thenReturn(Optional.of(user));
    when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

    authService.changePassword(
        "brewer@example.com",
        new ChangePasswordRequest("brew-test-pass-001", "brew-new-pass-0001"));

    verify(refreshTokenService).revokeAllForUser(1L);
  }

  @Test
  void changePassword_invalidatesOutstandingAccessTokens_andReturnsAFreshSession() {
    User user = stored("brewer@example.com", "brew-test-pass-001");
    when(userRepository.findByEmail("brewer@example.com")).thenReturn(Optional.of(user));
    when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
    when(refreshTokenService.issue(user)).thenReturn("new-refresh");
    when(jwtService.generateToken(user)).thenReturn("new-jwt");

    AuthSession session =
        authService.changePassword(
            "brewer@example.com",
            new ChangePasswordRequest("brew-test-pass-001", "brew-new-pass-0001"));

    assertThat(user.getTokenVersion()).isEqualTo(1);
    assertThat(session.response().token()).isEqualTo("new-jwt");
    assertThat(session.refreshToken()).isEqualTo("new-refresh");
    // The fresh refresh token is issued after the old ones are revoked, so it survives.
    org.mockito.InOrder order = org.mockito.Mockito.inOrder(refreshTokenService);
    order.verify(refreshTokenService).revokeAllForUser(1L);
    order.verify(refreshTokenService).issue(user);
  }

  @Test
  void changePassword_throwsWhenCurrentWrong() {
    when(userRepository.findByEmail("brewer@example.com"))
        .thenReturn(Optional.of(stored("brewer@example.com", "brew-test-pass-001")));

    assertThatThrownBy(
            () ->
                authService.changePassword(
                    "brewer@example.com", new ChangePasswordRequest("wrong", "brew-new-pass-0001")))
        .isInstanceOf(InvalidCurrentPasswordException.class);
    verify(refreshTokenService, never()).revokeAllForUser(anyLong());
  }

  @Test
  void login_throwsBeforeCheckingThePassword_whenTheAccountIsRateLimited() {
    RateLimiter limiter = org.mockito.Mockito.mock(RateLimiter.class);
    org.mockito.Mockito.doThrow(new RateLimitExceededException(Duration.ofMinutes(5)))
        .when(limiter)
        .requireAllowed(RateLimitRule.LOGIN_EMAIL, "brewer@example.com");
    AuthService limited =
        new AuthService(
            userRepository,
            jwtService,
            new BCryptPasswordEncoder(),
            emailVerificationService,
            refreshTokenService,
            limiter,
            featureFlagService);
    LoginRequest request = new LoginRequest("brewer@example.com", "brew-test-pass-001");

    assertThatThrownBy(() -> limited.login(request)).isInstanceOf(RateLimitExceededException.class);
    org.mockito.Mockito.verifyNoInteractions(userRepository, refreshTokenService);
  }

  @Test
  void login_unknownEmail_stillRunsOneBcryptComparison_soTimingMatchesAWrongPassword() {
    PasswordEncoder encoder = org.mockito.Mockito.spy(new BCryptPasswordEncoder());
    AuthService service =
        new AuthService(
            userRepository,
            jwtService,
            encoder,
            emailVerificationService,
            refreshTokenService,
            new RateLimiter(false),
            featureFlagService);
    when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());
    LoginRequest request = new LoginRequest("ghost@example.com", "brew-test-pass-001");

    assertThatThrownBy(() -> service.login(request))
        .isInstanceOf(org.springframework.security.authentication.BadCredentialsException.class)
        .hasMessage("Invalid email or password");

    // Exactly one comparison, against a real BCrypt hash (not a cheap early return).
    org.mockito.Mockito.verify(encoder)
        .matches(
            org.mockito.ArgumentMatchers.eq("brew-test-pass-001"),
            org.mockito.ArgumentMatchers.startsWith("$2"));
    org.mockito.Mockito.verifyNoInteractions(refreshTokenService);
  }

  @Test
  void login_unknownEmail_andWrongPassword_failIdentically() {
    when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());
    when(userRepository.findByEmail("brewer@example.com"))
        .thenReturn(Optional.of(stored("brewer@example.com", "brew-test-pass-001")));
    LoginRequest unknown = new LoginRequest("ghost@example.com", "brew-test-pass-001");
    LoginRequest wrong = new LoginRequest("brewer@example.com", "not-the-password");

    Throwable unknownError =
        org.assertj.core.api.Assertions.catchThrowable(() -> authService.login(unknown));
    Throwable wrongError =
        org.assertj.core.api.Assertions.catchThrowable(() -> authService.login(wrong));

    assertThat(unknownError).isExactlyInstanceOf(wrongError.getClass());
    assertThat(unknownError).hasMessage(wrongError.getMessage());
  }

  @Test
  void refresh_withoutAnyToken_isRejected() {
    assertThatThrownBy(() -> authService.refresh(null))
        .isInstanceOf(com.brewdeck.brewdeck_api.auth.refresh.InvalidRefreshTokenException.class);
    assertThatThrownBy(() -> authService.refresh("  "))
        .isInstanceOf(com.brewdeck.brewdeck_api.auth.refresh.InvalidRefreshTokenException.class);
    org.mockito.Mockito.verifyNoInteractions(refreshTokenService);
  }

  @Test
  void logout_withoutAToken_revokesNothing() {
    when(userRepository.findByEmail("brewer@example.com"))
        .thenReturn(Optional.of(stored("brewer@example.com", "brew-test-pass-001")));

    authService.logout("brewer@example.com", null);

    org.mockito.Mockito.verifyNoInteractions(refreshTokenService);
  }

  @Test
  void logout_invalidatesOutstandingAccessTokens() {
    User user = stored("brewer@example.com", "brew-test-pass-001");
    when(userRepository.findByEmail("brewer@example.com")).thenReturn(Optional.of(user));

    authService.logout("brewer@example.com", "refresh");

    assertThat(user.getTokenVersion()).isEqualTo(1);
    verify(refreshTokenService).revoke("refresh", 1L);
    verify(userRepository).save(user);
  }

  @Test
  void register_whileTermsArePublished_requiresTheCurrentVersion() {
    when(featureFlagService.isEnabled(FeatureKeys.LEGAL_TERMS)).thenReturn(true);

    assertThatThrownBy(
            () ->
                authService.register(
                    new RegisterRequest("terms@example.com", "brew-test-pass-001")))
        .isInstanceOf(TermsNotAcceptedException.class);
    assertThatThrownBy(
            () ->
                authService.register(
                    new RegisterRequest("terms@example.com", "brew-test-pass-001", "1999-01-01")))
        .isInstanceOf(TermsNotAcceptedException.class);
    org.mockito.Mockito.verifyNoInteractions(userRepository, emailVerificationService);
  }

  @Test
  void register_recordsTheAcceptedVersion() {
    when(userRepository.findByEmail("terms@example.com")).thenReturn(Optional.empty());
    when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

    authService.register(
        new RegisterRequest("terms@example.com", "brew-test-pass-001", LegalTerms.CURRENT_VERSION));

    ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
    verify(userRepository).save(saved.capture());
    assertThat(saved.getValue().getTermsVersion()).isEqualTo(LegalTerms.CURRENT_VERSION);
    assertThat(saved.getValue().getTermsAcceptedAt()).isNotNull();
  }

  @Test
  void register_whileTermsAreNotPublished_needsNoAcceptance() {
    when(userRepository.findByEmail("plain@example.com")).thenReturn(Optional.empty());
    when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

    authService.register(new RegisterRequest("plain@example.com", "brew-test-pass-001"));

    ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
    verify(userRepository).save(saved.capture());
    assertThat(saved.getValue().getTermsVersion()).isNull();
  }
}
