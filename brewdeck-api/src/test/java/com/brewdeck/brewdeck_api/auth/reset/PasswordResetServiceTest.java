package com.brewdeck.brewdeck_api.auth.reset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.brewdeck.brewdeck_api.auth.User;
import com.brewdeck.brewdeck_api.auth.UserRepository;
import java.time.LocalDateTime;
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

  private PasswordResetService service;

  @BeforeEach
  void setUp() {
    PasswordEncoder encoder = new BCryptPasswordEncoder();
    service = new PasswordResetService(tokenRepository, userRepository, encoder, mailPort);
  }

  private User user() {
    return User.builder()
        .id(1L)
        .email("brewer@example.com")
        .passwordHash(new BCryptPasswordEncoder().encode("password1"))
        .createdAt(LocalDateTime.now())
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
    assertThat(saved.getExpiresAt()).isAfter(LocalDateTime.now());
  }

  @Test
  void resetPassword_validToken_reencodesAndStampsUsed() {
    PasswordResetToken token =
        PasswordResetToken.builder()
            .id(5L)
            .userId(1L)
            .tokenHash("9d0e410f5e6a3f0e0c3e8f6d6f2b4a0c9d0e410f5e6a3f0e0c3e8f6d6f2b4a0c")
            .expiresAt(LocalDateTime.now().plusMinutes(10))
            .createdAt(LocalDateTime.now())
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
  }

  @Test
  void resetPassword_unknownToken_throws() {
    when(tokenRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());

    assertThatThrownBy(
            () -> service.resetPassword(new ResetPasswordRequest("nope", "newpassword1")))
        .isInstanceOf(InvalidResetTokenException.class);
  }

  @Test
  void resetPassword_expiredToken_throws() {
    PasswordResetToken token =
        PasswordResetToken.builder()
            .id(6L)
            .userId(1L)
            .tokenHash("hash")
            .expiresAt(LocalDateTime.now().minusMinutes(1))
            .createdAt(LocalDateTime.now().minusMinutes(31))
            .build();
    when(tokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(token));

    assertThatThrownBy(() -> service.resetPassword(new ResetPasswordRequest("raw", "newpassword1")))
        .isInstanceOf(InvalidResetTokenException.class);
  }

  @Test
  void resetPassword_usedToken_throws() {
    PasswordResetToken token =
        PasswordResetToken.builder()
            .id(7L)
            .userId(1L)
            .tokenHash("hash")
            .expiresAt(LocalDateTime.now().plusMinutes(10))
            .usedAt(LocalDateTime.now().minusMinutes(1))
            .createdAt(LocalDateTime.now())
            .build();
    when(tokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(token));

    assertThatThrownBy(() -> service.resetPassword(new ResetPasswordRequest("raw", "newpassword1")))
        .isInstanceOf(InvalidResetTokenException.class);
  }
}
