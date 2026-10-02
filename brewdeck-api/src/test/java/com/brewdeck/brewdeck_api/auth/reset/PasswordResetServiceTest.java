package com.brewdeck.brewdeck_api.auth.reset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.brewdeck.brewdeck_api.auth.User;
import com.brewdeck.brewdeck_api.auth.UserRepository;
import com.brewdeck.brewdeck_api.auth.refresh.RefreshTokenService;
import com.brewdeck.brewdeck_api.common.ratelimit.RateLimitExceededException;
import com.brewdeck.brewdeck_api.common.ratelimit.RateLimitRule;
import com.brewdeck.brewdeck_api.common.ratelimit.RateLimiter;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
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
class PasswordResetServiceTest {

  @Mock private PasswordResetTokenRepository tokenRepository;
  @Mock private UserRepository userRepository;
  @Mock private PasswordResetMailPort mailPort;
  @Mock private RefreshTokenService refreshTokenService;

  private PasswordResetService service;

  @BeforeEach
  void setUp() {
    PasswordEncoder encoder = new BCryptPasswordEncoder();
    service =
        new PasswordResetService(
            tokenRepository,
            userRepository,
            encoder,
            mailPort,
            refreshTokenService,
            new RateLimiter(false));
  }

  private User user() {
    return User.builder()
        .id(1L)
        .email("brewer@example.com")
        .passwordHash(new BCryptPasswordEncoder().encode("password1"))
        .createdAt(Instant.now())
        .build();
  }

  @Test
  void requestReset_unknownEmail_isSilentNoOp() {
    when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

    service.requestReset(new ForgotPasswordRequest("ghost@example.com"));

    verify(tokenRepository, never()).save(any());
    verify(mailPort, never()).sendResetLink(anyString(), anyString());
  }

  @Test
  void requestReset_knownEmail_persistsHashedTokenAndSendsLink() {
    when(userRepository.findByEmail("brewer@example.com")).thenReturn(Optional.of(user()));
    when(tokenRepository.findByUserIdAndUsedAtIsNull(1L)).thenReturn(List.of());

    service.requestReset(new ForgotPasswordRequest("brewer@example.com"));

    ArgumentCaptor<PasswordResetToken> tokenCaptor =
        ArgumentCaptor.forClass(PasswordResetToken.class);
    verify(tokenRepository).save(tokenCaptor.capture());
    ArgumentCaptor<String> rawCaptor = ArgumentCaptor.forClass(String.class);
    verify(mailPort).sendResetLink(eq("brewer@example.com"), rawCaptor.capture());

    PasswordResetToken saved = tokenCaptor.getValue();
    // Stored value is a 64-char hex hash, never the raw token.
    assertThat(saved.getTokenHash()).hasSize(64).isNotEqualTo(rawCaptor.getValue());
    assertThat(saved.getExpiresAt()).isAfter(Instant.now());
  }

  @Test
  void requestReset_invalidatesOutstandingUnusedTokens() {
    when(userRepository.findByEmail("brewer@example.com")).thenReturn(Optional.of(user()));
    PasswordResetToken outstanding =
        PasswordResetToken.builder()
            .id(9L)
            .userId(1L)
            .tokenHash("prior-hash")
            .expiresAt(Instant.now().plus(10, ChronoUnit.MINUTES))
            .createdAt(Instant.now())
            .build();
    when(tokenRepository.findByUserIdAndUsedAtIsNull(1L)).thenReturn(List.of(outstanding));

    service.requestReset(new ForgotPasswordRequest("brewer@example.com"));

    // The prior unused token is stamped used and persisted before a new one is issued.
    assertThat(outstanding.getUsedAt()).isNotNull();
    verify(tokenRepository).saveAll(List.of(outstanding));
  }

  @Test
  void resetPassword_validToken_reencodesAndStampsUsed() {
    PasswordResetToken token =
        PasswordResetToken.builder()
            .id(5L)
            .userId(1L)
            .tokenHash("9d0e410f5e6a3f0e0c3e8f6d6f2b4a0c9d0e410f5e6a3f0e0c3e8f6d6f2b4a0c")
            .expiresAt(Instant.now().plus(10, ChronoUnit.MINUTES))
            .createdAt(Instant.now())
            .build();
    // Match the service's hash of the supplied raw token.
    when(tokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(token));
    User user = user();
    when(userRepository.findById(1L)).thenReturn(Optional.of(user));
    String originalHash = user.getPasswordHash();

    service.resetPassword(new ResetPasswordRequest("any-raw-token", "newpassword1"));

    assertThat(user.getPasswordHash()).isNotEqualTo(originalHash);
    assertThat(new BCryptPasswordEncoder().matches("newpassword1", user.getPasswordHash()))
        .isTrue();
    assertThat(token.getUsedAt()).isNotNull();
    verify(refreshTokenService).revokeAllForUser(1L);
  }

  @Test
  void resetPassword_unknownToken_throws() {
    when(tokenRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());

    assertThatThrownBy(
            () -> service.resetPassword(new ResetPasswordRequest("nope", "newpassword1")))
        .isInstanceOf(InvalidResetTokenException.class);
    verify(refreshTokenService, never()).revokeAllForUser(anyLong());
  }

  @Test
  void resetPassword_expiredToken_throws() {
    PasswordResetToken token =
        PasswordResetToken.builder()
            .id(6L)
            .userId(1L)
            .tokenHash("hash")
            .expiresAt(Instant.now().minus(1, ChronoUnit.MINUTES))
            .createdAt(Instant.now().minus(31, ChronoUnit.MINUTES))
            .build();
    when(tokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(token));

    assertThatThrownBy(() -> service.resetPassword(new ResetPasswordRequest("raw", "newpassword1")))
        .isInstanceOf(InvalidResetTokenException.class);
    verify(refreshTokenService, never()).revokeAllForUser(anyLong());
  }

  @Test
  void resetPassword_usedToken_throws() {
    PasswordResetToken token =
        PasswordResetToken.builder()
            .id(7L)
            .userId(1L)
            .tokenHash("hash")
            .expiresAt(Instant.now().plus(10, ChronoUnit.MINUTES))
            .usedAt(Instant.now().minus(1, ChronoUnit.MINUTES))
            .createdAt(Instant.now())
            .build();
    when(tokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(token));

    assertThatThrownBy(() -> service.resetPassword(new ResetPasswordRequest("raw", "newpassword1")))
        .isInstanceOf(InvalidResetTokenException.class);
    verify(refreshTokenService, never()).revokeAllForUser(anyLong());
  }

  @Test
  void requestReset_throttledEmail_sendsNothingAndDoesNotLookUpTheUser() {
    RateLimiter limiter = org.mockito.Mockito.mock(RateLimiter.class);
    org.mockito.Mockito.doThrow(new RateLimitExceededException(Duration.ofMinutes(30)))
        .when(limiter)
        .requireAllowed(RateLimitRule.FORGOT_PASSWORD_EMAIL, "brewer@example.com");
    PasswordResetService limited =
        new PasswordResetService(
            tokenRepository,
            userRepository,
            new BCryptPasswordEncoder(),
            mailPort,
            refreshTokenService,
            limiter);
    ForgotPasswordRequest request = new ForgotPasswordRequest("brewer@example.com");

    assertThatThrownBy(() -> limited.requestReset(request))
        .isInstanceOf(RateLimitExceededException.class);
    org.mockito.Mockito.verifyNoInteractions(userRepository, mailPort);
  }
}
