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
import com.brewdeck.brewdeck_api.common.metrics.BrewdeckMetrics;
import com.brewdeck.brewdeck_api.common.security.SecureTokens;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

  private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
  private final BrewdeckMetrics metrics = new BrewdeckMetrics(meterRegistry);

  @Mock private RefreshTokenRepository tokenRepository;
  @Mock private UserRepository userRepository;

  private RefreshTokenService service;

  private final User user = User.builder().id(7L).email("u@example.com").build();

  @BeforeEach
  void setUp() {
    service = new RefreshTokenService(tokenRepository, userRepository, Duration.ofDays(7), metrics);
    lenient()
        .when(tokenRepository.save(any(RefreshToken.class)))
        .thenAnswer(inv -> inv.getArgument(0));
  }

  private RefreshToken activeToken(String rawToken) {
    return RefreshToken.builder()
        .id(1L)
        .userId(user.getId())
        .tokenHash(SecureTokens.sha256Hex(rawToken))
        .expiresAt(Instant.now().plus(7, ChronoUnit.DAYS))
        .createdAt(Instant.now())
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
    when(tokenRepository.markUsedIfActive(eq(1L), any(Instant.class))).thenReturn(1);

    RefreshTokenService.RotationResult result = service.rotate(raw);

    assertThat(result.user()).isEqualTo(user);
    assertThat(result.rawToken()).isNotBlank().isNotEqualTo(raw);
    // The old token is claimed by the conditional update; only the new one is saved.
    verify(tokenRepository).markUsedIfActive(eq(1L), any(Instant.class));
    verify(tokenRepository, times(1)).save(any(RefreshToken.class));
    assertThat(meterRegistry.counter("brewdeck.auth.refresh.reuse").count()).isZero();
  }

  @Test
  void rotateThatLosesTheRaceIsTreatedAsReuse() {
    String raw = "raw-raced";
    when(tokenRepository.findByTokenHash(SecureTokens.sha256Hex(raw)))
        .thenReturn(Optional.of(activeToken(raw)));
    when(userRepository.findById(7L)).thenReturn(Optional.of(user));
    // A concurrent rotation of the same token committed first.
    when(tokenRepository.markUsedIfActive(eq(1L), any(Instant.class))).thenReturn(0);

    assertThatThrownBy(() -> service.rotate(raw))
        .isInstanceOf(InvalidRefreshTokenException.class)
        .hasMessage("Refresh token already used");
    verify(tokenRepository).revokeAllActiveForUser(eq(7L), any(Instant.class));
    verify(tokenRepository, never()).save(any());
    assertThat(meterRegistry.counter("brewdeck.auth.refresh.reuse").count()).isEqualTo(1);
  }

  @Test
  void rotateOnUsedTokenRevokesAllActiveAndThrows() {
    String raw = "raw-used";
    RefreshToken used = activeToken(raw);
    used.setUsedAt(Instant.now().minus(1, ChronoUnit.MINUTES));
    when(tokenRepository.findByTokenHash(SecureTokens.sha256Hex(raw)))
        .thenReturn(Optional.of(used));

    assertThatThrownBy(() -> service.rotate(raw)).isInstanceOf(InvalidRefreshTokenException.class);
    verify(tokenRepository).revokeAllActiveForUser(eq(7L), any(Instant.class));
    verify(userRepository, never()).findById(any());
    assertThat(meterRegistry.counter("brewdeck.auth.refresh.reuse").count()).isEqualTo(1);
  }

  @Test
  void rotateOnRevokedTokenThrowsWithoutRevokingAll() {
    String raw = "raw-revoked";
    RefreshToken revoked = activeToken(raw);
    revoked.setRevokedAt(Instant.now().minus(1, ChronoUnit.MINUTES));
    when(tokenRepository.findByTokenHash(SecureTokens.sha256Hex(raw)))
        .thenReturn(Optional.of(revoked));

    assertThatThrownBy(() -> service.rotate(raw)).isInstanceOf(InvalidRefreshTokenException.class);
    verify(tokenRepository, never()).revokeAllActiveForUser(any(), any());
    verify(userRepository, never()).findById(any());
  }

  @Test
  void rotateOnExpiredTokenThrowsWithoutRevokingAll() {
    String raw = "raw-expired";
    RefreshToken expired = activeToken(raw);
    expired.setExpiresAt(Instant.now().minus(1, ChronoUnit.MINUTES));
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
    stored.setRevokedAt(Instant.now().minus(1, ChronoUnit.MINUTES));
    when(tokenRepository.findByTokenHash(SecureTokens.sha256Hex(raw)))
        .thenReturn(Optional.of(stored));

    service.revoke(raw, 7L);

    verify(tokenRepository, never()).save(any());
  }

  @Test
  void revokeAllForUserRevokesEveryActiveTokenOfThatUser() {
    service.revokeAllForUser(7L);

    verify(tokenRepository).revokeAllActiveForUser(eq(7L), any(Instant.class));
  }
}
