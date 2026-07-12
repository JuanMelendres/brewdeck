package com.brewdeck.brewdeck_api.auth.verification;

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

@ExtendWith(MockitoExtension.class)
class EmailVerificationServiceTest {

  @Mock private EmailVerificationTokenRepository tokenRepository;
  @Mock private UserRepository userRepository;
  @Mock private EmailVerificationMailPort mailPort;

  private EmailVerificationService service;

  @BeforeEach
  void setUp() {
    service = new EmailVerificationService(tokenRepository, userRepository, mailPort);
  }

  private User user(boolean verified) {
    return User.builder()
        .id(1L)
        .email("brewer@example.com")
        .passwordHash("hash")
        .emailVerified(verified)
        .createdAt(LocalDateTime.now())
        .build();
  }

  @Test
  void issueFor_persistsHashedTokenAndSendsLink() {
    when(tokenRepository.findByUserIdAndUsedAtIsNull(1L)).thenReturn(List.of());

    service.issueFor(user(false));

    ArgumentCaptor<EmailVerificationToken> tokenCaptor =
        ArgumentCaptor.forClass(EmailVerificationToken.class);
    verify(tokenRepository).save(tokenCaptor.capture());
    ArgumentCaptor<String> rawCaptor = ArgumentCaptor.forClass(String.class);
    verify(mailPort).sendVerificationLink(eq("brewer@example.com"), rawCaptor.capture());

    EmailVerificationToken saved = tokenCaptor.getValue();
    assertThat(saved.getTokenHash()).hasSize(64).isNotEqualTo(rawCaptor.getValue());
    assertThat(saved.getExpiresAt()).isAfter(LocalDateTime.now().plusHours(23));
  }

  @Test
  void issueFor_stillPersistsTokenWhenMailThrows() {
    when(tokenRepository.findByUserIdAndUsedAtIsNull(1L)).thenReturn(List.of());
    org.mockito.Mockito.doThrow(new RuntimeException("smtp down"))
        .when(mailPort)
        .sendVerificationLink(anyString(), anyString());

    // Must not propagate — registration/resend rely on this.
    service.issueFor(user(false));

    verify(tokenRepository).save(any(EmailVerificationToken.class));
  }

  @Test
  void verify_validToken_setsEmailVerifiedAndStampsUsed() {
    EmailVerificationToken token =
        EmailVerificationToken.builder()
            .id(5L)
            .userId(1L)
            .tokenHash("hash")
            .expiresAt(LocalDateTime.now().plusHours(1))
            .createdAt(LocalDateTime.now())
            .build();
    when(tokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(token));
    User user = user(false);
    when(userRepository.findById(1L)).thenReturn(Optional.of(user));

    service.verify("any-raw-token");

    assertThat(user.isEmailVerified()).isTrue();
    assertThat(token.getUsedAt()).isNotNull();
  }

  @Test
  void verify_unknownToken_throws() {
    when(tokenRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.verify("nope"))
        .isInstanceOf(InvalidVerificationTokenException.class);
  }

  @Test
  void verify_expiredToken_throws() {
    EmailVerificationToken token =
        EmailVerificationToken.builder()
            .id(6L)
            .userId(1L)
            .tokenHash("hash")
            .expiresAt(LocalDateTime.now().minusMinutes(1))
            .createdAt(LocalDateTime.now().minusHours(25))
            .build();
    when(tokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(token));

    assertThatThrownBy(() -> service.verify("raw"))
        .isInstanceOf(InvalidVerificationTokenException.class);
  }

  @Test
  void verify_usedToken_throws() {
    EmailVerificationToken token =
        EmailVerificationToken.builder()
            .id(7L)
            .userId(1L)
            .tokenHash("hash")
            .expiresAt(LocalDateTime.now().plusHours(1))
            .usedAt(LocalDateTime.now().minusMinutes(1))
            .createdAt(LocalDateTime.now())
            .build();
    when(tokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(token));

    assertThatThrownBy(() -> service.verify("raw"))
        .isInstanceOf(InvalidVerificationTokenException.class);
  }

  @Test
  void resendFor_alreadyVerified_isNoOp() {
    when(userRepository.findByEmail("brewer@example.com")).thenReturn(Optional.of(user(true)));

    service.resendFor("brewer@example.com");

    verify(tokenRepository, never()).save(any());
    verify(mailPort, never()).sendVerificationLink(anyString(), anyString());
  }

  @Test
  void resendFor_unverified_issuesToken() {
    when(userRepository.findByEmail("brewer@example.com")).thenReturn(Optional.of(user(false)));
    when(tokenRepository.findByUserIdAndUsedAtIsNull(1L)).thenReturn(List.of());

    service.resendFor("brewer@example.com");

    verify(tokenRepository).save(any(EmailVerificationToken.class));
    verify(mailPort).sendVerificationLink(eq("brewer@example.com"), anyString());
  }
}
