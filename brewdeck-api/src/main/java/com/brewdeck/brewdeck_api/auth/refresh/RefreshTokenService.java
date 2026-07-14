package com.brewdeck.brewdeck_api.auth.refresh;

import com.brewdeck.brewdeck_api.auth.User;
import com.brewdeck.brewdeck_api.auth.UserRepository;
import com.brewdeck.brewdeck_api.common.security.SecureTokens;
import java.time.Duration;
import java.time.LocalDateTime;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class RefreshTokenService {

  private final RefreshTokenRepository tokenRepository;
  private final UserRepository userRepository;
  private final Duration refreshTtl;

  public RefreshTokenService(
      RefreshTokenRepository tokenRepository,
      UserRepository userRepository,
      @Value("${brewdeck.auth.refresh-ttl}") Duration refreshTtl) {
    this.tokenRepository = tokenRepository;
    this.userRepository = userRepository;
    this.refreshTtl = refreshTtl;
  }

  /** Issues a fresh refresh token for the user and returns the raw (unhashed) value. */
  @Transactional
  public String issue(User user) {
    return issueInternal(user, LocalDateTime.now());
  }

  /**
   * Rotates a refresh token: validates it, marks it used, and issues a replacement. Presenting an
   * already-used or revoked token is treated as theft and revokes every active token for that user.
   *
   * <p>{@code noRollbackFor} is essential: the reuse path revokes the user's active tokens and then
   * throws. Without it the throw would roll back the very revocation we rely on for containment.
   * This method must remain the OUTERMOST transaction boundary for that revocation to persist — no
   * caller may wrap it in a plain {@code @Transactional}, or that outer boundary's default
   * rollback-on-RuntimeException rule would join and roll back this transaction, undoing the
   * revocation (see {@code AuthService.refresh()}, deliberately non-transactional for this reason).
   */
  @Transactional(noRollbackFor = InvalidRefreshTokenException.class)
  public RotationResult rotate(String rawToken) {
    RefreshToken token =
        tokenRepository
            .findByTokenHash(SecureTokens.sha256Hex(rawToken))
            .orElseThrow(() -> new InvalidRefreshTokenException("Unknown refresh token"));

    LocalDateTime now = LocalDateTime.now();

    if (token.getUsedAt() != null || token.getRevokedAt() != null) {
      tokenRepository.revokeAllActiveForUser(token.getUserId(), now);
      log.warn(
          "Refresh token reuse detected for user id={}; revoked all active tokens",
          token.getUserId());
      throw new InvalidRefreshTokenException("Refresh token already used");
    }

    if (token.getExpiresAt().isBefore(now)) {
      throw new InvalidRefreshTokenException("Refresh token expired");
    }

    User user =
        userRepository
            .findById(token.getUserId())
            .orElseThrow(() -> new InvalidRefreshTokenException("Refresh token has no user"));

    token.setUsedAt(now);
    tokenRepository.save(token);

    String newRawToken = issueInternal(user, now);
    return new RotationResult(user, newRawToken);
  }

  /** Revokes the presented token if it belongs to the user and is still active. Idempotent. */
  @Transactional
  public void revoke(String rawToken, Long userId) {
    tokenRepository
        .findByTokenHash(SecureTokens.sha256Hex(rawToken))
        .filter(
            t -> t.getUserId().equals(userId) && t.getUsedAt() == null && t.getRevokedAt() == null)
        .ifPresent(
            t -> {
              t.setRevokedAt(LocalDateTime.now());
              tokenRepository.save(t);
              log.info("Refresh token revoked for user id={}", userId);
            });
  }

  private String issueInternal(User user, LocalDateTime now) {
    String rawToken = SecureTokens.newToken();
    tokenRepository.save(
        RefreshToken.builder()
            .userId(user.getId())
            .tokenHash(SecureTokens.sha256Hex(rawToken))
            .expiresAt(now.plus(refreshTtl))
            .createdAt(now)
            .build());
    return rawToken;
  }

  public record RotationResult(User user, String rawToken) {}
}
