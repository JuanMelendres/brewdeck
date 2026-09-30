package com.brewdeck.brewdeck_api.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.brewdeck.brewdeck_api.auth.refresh.RefreshTokenService;
import com.brewdeck.brewdeck_api.common.ratelimit.RateLimitExceededException;
import com.brewdeck.brewdeck_api.common.ratelimit.RateLimitRule;
import com.brewdeck.brewdeck_api.common.ratelimit.RateLimiter;
import jakarta.persistence.EntityNotFoundException;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

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
            new RateLimiter(false));
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
  void register_persistsHashedPasswordAndReturnsToken() {
    when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
    when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
    when(jwtService.generateToken(any(User.class))).thenReturn("jwt-token");

    AuthResponse response =
        authService.register(new RegisterRequest("new@example.com", "password1")).response();

    assertThat(response.token()).isEqualTo("jwt-token");
    assertThat(response.email()).isEqualTo("new@example.com");
    org.mockito.Mockito.verify(emailVerificationService)
        .issueFor(org.mockito.ArgumentMatchers.any(User.class));
  }

  @Test
  void register_throwsWhenEmailAlreadyUsed() {
    when(userRepository.existsByEmail("taken@example.com")).thenReturn(true);

    assertThatThrownBy(
            () -> authService.register(new RegisterRequest("taken@example.com", "password1")))
        .isInstanceOf(EmailAlreadyUsedException.class);
  }

  @Test
  void register_succeedsEvenWhenVerificationIssueThrows() {
    when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
    when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
    when(jwtService.generateToken(any(User.class))).thenReturn("jwt-token");
    org.mockito.Mockito.doThrow(new RuntimeException("verification down"))
        .when(emailVerificationService)
        .issueFor(any(User.class));

    // The account is created and a token returned even if verification issuance fails.
    AuthResponse response =
        authService.register(new RegisterRequest("new@example.com", "password1")).response();

    assertThat(response.token()).isEqualTo("jwt-token");
  }

  @Test
  void login_returnsTokenWhenPasswordMatches() {
    when(userRepository.findByEmail("brewer@example.com"))
        .thenReturn(Optional.of(stored("brewer@example.com", "password1")));
    when(jwtService.generateToken(any(User.class))).thenReturn("jwt-token");

    AuthResponse response =
        authService.login(new LoginRequest("brewer@example.com", "password1")).response();

    assertThat(response.token()).isEqualTo("jwt-token");
  }

  @Test
  void login_throwsWhenPasswordWrong() {
    when(userRepository.findByEmail("brewer@example.com"))
        .thenReturn(Optional.of(stored("brewer@example.com", "password1")));

    assertThatThrownBy(() -> authService.login(new LoginRequest("brewer@example.com", "wrong")))
        .isInstanceOf(org.springframework.security.authentication.BadCredentialsException.class);
  }

  @Test
  void login_throwsWhenEmailUnknown() {
    when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> authService.login(new LoginRequest("ghost@example.com", "password1")))
        .isInstanceOf(org.springframework.security.authentication.BadCredentialsException.class);
  }

  @Test
  void me_returnsUserWhenPresent() {
    when(userRepository.findByEmail("brewer@example.com"))
        .thenReturn(Optional.of(stored("brewer@example.com", "password1")));

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
        .thenReturn(Optional.of(stored("brewer@example.com", "password1")));
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
  void changePassword_reencodesWhenCurrentMatches() {
    User user = stored("brewer@example.com", "password1");
    String originalHash = user.getPasswordHash();
    when(userRepository.findByEmail("brewer@example.com")).thenReturn(Optional.of(user));
    when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

    authService.changePassword(
        "brewer@example.com", new ChangePasswordRequest("password1", "newpassword1"));

    assertThat(user.getPasswordHash()).isNotEqualTo(originalHash);
    assertThat(new BCryptPasswordEncoder().matches("newpassword1", user.getPasswordHash()))
        .isTrue();
  }

  @Test
  void changePassword_revokesAllRefreshTokensForUser() {
    User user = stored("brewer@example.com", "password1");
    when(userRepository.findByEmail("brewer@example.com")).thenReturn(Optional.of(user));
    when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

    authService.changePassword(
        "brewer@example.com", new ChangePasswordRequest("password1", "newpassword1"));

    verify(refreshTokenService).revokeAllForUser(1L);
  }

  @Test
  void changePassword_throwsWhenCurrentWrong() {
    when(userRepository.findByEmail("brewer@example.com"))
        .thenReturn(Optional.of(stored("brewer@example.com", "password1")));

    assertThatThrownBy(
            () ->
                authService.changePassword(
                    "brewer@example.com", new ChangePasswordRequest("wrong", "newpassword1")))
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
            limiter);
    LoginRequest request = new LoginRequest("brewer@example.com", "password1");

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
            new RateLimiter(false));
    when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());
    LoginRequest request = new LoginRequest("ghost@example.com", "password1");

    assertThatThrownBy(() -> service.login(request))
        .isInstanceOf(org.springframework.security.authentication.BadCredentialsException.class)
        .hasMessage("Invalid email or password");

    // Exactly one comparison, against a real BCrypt hash (not a cheap early return).
    org.mockito.Mockito.verify(encoder)
        .matches(
            org.mockito.ArgumentMatchers.eq("password1"),
            org.mockito.ArgumentMatchers.startsWith("$2"));
    org.mockito.Mockito.verifyNoInteractions(refreshTokenService);
  }

  @Test
  void login_unknownEmail_andWrongPassword_failIdentically() {
    when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());
    when(userRepository.findByEmail("brewer@example.com"))
        .thenReturn(Optional.of(stored("brewer@example.com", "password1")));
    LoginRequest unknown = new LoginRequest("ghost@example.com", "password1");
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
        .thenReturn(Optional.of(stored("brewer@example.com", "password1")));

    authService.logout("brewer@example.com", null);

    org.mockito.Mockito.verifyNoInteractions(refreshTokenService);
  }
}
