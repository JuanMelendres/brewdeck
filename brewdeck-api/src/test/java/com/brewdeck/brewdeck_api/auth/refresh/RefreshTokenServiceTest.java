package com.brewdeck.brewdeck_api.auth.refresh;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.brewdeck.brewdeck_api.auth.User;
import com.brewdeck.brewdeck_api.auth.UserRepository;
import com.brewdeck.brewdeck_api.common.security.SecureTokens;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

  @Mock private RefreshTokenRepository tokenRepository;
  @Mock private UserRepository userRepository;

  private RefreshTokenService service;

  private final User user = User.builder().id(7L).email("u@example.com").build();

  @BeforeEach
  void setUp() {
    service = new RefreshTokenService(tokenRepository, userRepository, Duration.ofDays(7));
    lenient()
        .when(tokenRepository.save(any(RefreshToken.class)))
        .thenAnswer(inv -> inv.getArgument(0));
  }

  private RefreshToken activeToken(String rawToken) {
    return RefreshToken.builder()
        .id(1L)
        .userId(user.getId())
        .tokenHash(SecureTokens.sha256Hex(rawToken))
        .expiresAt(LocalDateTime.now().plusDays(7))
        .createdAt(LocalDateTime.now())
        .build();
  }

  @Test
  void issuePersistsHashedTokenAndReturnsRawValue() {
    String raw = service.issue(user);

    ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
    verify(tokenRepository).save(captor.capture());
    assertThat(captor.getValue().getTokenHash()).isEqualTo(SecureTokens.sha256Hex(raw));
    assertThat(captor.getValue().getUserId()).isEqualTo(7L);
    assertThat(raw).isNotBlank();
  }

  @Test
  void rotateMarksOldUsedAndIssuesNewToken() {
    String raw = "raw-valid";
    RefreshToken stored = activeToken(raw);
    when(tokenRepository.findByTokenHash(SecureTokens.sha256Hex(raw)))
        .thenReturn(Optional.of(stored));
    when(userRepository.findById(7L)).thenReturn(Optional.of(user));

    RefreshTokenService.RotationResult result = service.rotate(raw);

    assertThat(stored.getUsedAt()).isNotNull();
    assertThat(result.user()).isEqualTo(user);
    assertThat(result.rawToken()).isNotBlank().isNotEqualTo(raw);
    // saved twice: the rotated (used) token + the new one.
    verify(tokenRepository, times(2)).save(any(RefreshToken.class));
  }

  @Test
  void rotateOnUsedTokenRevokesAllActiveAndThrows() {
    String raw = "raw-used";
    RefreshToken used = activeToken(raw);
    used.setUsedAt(LocalDateTime.now().minusMinutes(1));
    when(tokenRepository.findByTokenHash(SecureTokens.sha256Hex(raw)))
        .thenReturn(Optional.of(used));

    assertThatThrownBy(() -> service.rotate(raw)).isInstanceOf(InvalidRefreshTokenException.class);
    verify(tokenRepository).revokeAllActiveForUser(eq(7L), any(LocalDateTime.class));
    verify(userRepository, never()).findById(any());
  }

  @Test
  void rotateOnExpiredTokenThrowsWithoutRevokingAll() {
    String raw = "raw-expired";
    RefreshToken expired = activeToken(raw);
    expired.setExpiresAt(LocalDateTime.now().minusMinutes(1));
    when(tokenRepository.findByTokenHash(SecureTokens.sha256Hex(raw)))
        .thenReturn(Optional.of(expired));

    assertThatThrownBy(() -> service.rotate(raw)).isInstanceOf(InvalidRefreshTokenException.class);
    verify(tokenRepository, never()).revokeAllActiveForUser(any(), any());
  }

  @Test
  void rotateOnUnknownTokenThrows() {
    when(tokenRepository.findByTokenHash(any())).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.rotate("nope"))
        .isInstanceOf(InvalidRefreshTokenException.class);
    verify(tokenRepository, never()).revokeAllActiveForUser(any(), any());
  }

  @Test
  void revokeSetsRevokedAtWhenOwnedAndActive() {
    String raw = "raw-logout";
    RefreshToken stored = activeToken(raw);
    when(tokenRepository.findByTokenHash(SecureTokens.sha256Hex(raw)))
        .thenReturn(Optional.of(stored));

    service.revoke(raw, 7L);

    assertThat(stored.getRevokedAt()).isNotNull();
    verify(tokenRepository).save(stored);
  }

  @Test
  void revokeIsNoOpWhenTokenBelongsToAnotherUser() {
    String raw = "raw-other";
    RefreshToken stored = activeToken(raw);
    when(tokenRepository.findByTokenHash(SecureTokens.sha256Hex(raw)))
        .thenReturn(Optional.of(stored));

    service.revoke(raw, 999L);

    assertThat(stored.getRevokedAt()).isNull();
    verify(tokenRepository, never()).save(any());
  }

  @Test
  void revokeIsNoOpWhenTokenUnknown() {
    when(tokenRepository.findByTokenHash(any())).thenReturn(Optional.empty());

    service.revoke("missing", 7L);

    verify(tokenRepository, never()).save(any());
  }

  @Test
  void revokeIsNoOpWhenTokenAlreadyRevoked() {
    String raw = "raw-already-revoked";
    RefreshToken stored = activeToken(raw);
    stored.setRevokedAt(LocalDateTime.now().minusMinutes(1));
    when(tokenRepository.findByTokenHash(SecureTokens.sha256Hex(raw)))
        .thenReturn(Optional.of(stored));

    service.revoke(raw, 7L);

    verify(tokenRepository, never()).save(any());
  }
}
