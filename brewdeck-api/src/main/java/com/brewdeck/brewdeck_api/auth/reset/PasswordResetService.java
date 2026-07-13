package com.brewdeck.brewdeck_api.auth.reset;

import com.brewdeck.brewdeck_api.auth.User;
import com.brewdeck.brewdeck_api.auth.UserRepository;
import com.brewdeck.brewdeck_api.common.security.SecureTokens;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class PasswordResetService {

  private static final int TTL_MINUTES = 30;

  private final PasswordResetTokenRepository tokenRepository;
  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final PasswordResetMailPort mailPort;

  public PasswordResetService(
      PasswordResetTokenRepository tokenRepository,
      UserRepository userRepository,
      PasswordEncoder passwordEncoder,
      PasswordResetMailPort mailPort) {
    this.tokenRepository = tokenRepository;
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
    this.mailPort = mailPort;
  }

  @Transactional
  public void requestReset(ForgotPasswordRequest request) {
    Optional<User> maybeUser = userRepository.findByEmail(request.email());
    if (maybeUser.isEmpty()) {
      // No user enumeration: silently succeed for unknown emails.
      log.info("Password reset requested for unknown email; no-op");
      return;
    }
    User user = maybeUser.get();

    // Invalidate any outstanding unused tokens for this user.
    List<PasswordResetToken> outstanding =
        tokenRepository.findByUserIdAndUsedAtIsNull(user.getId());
    LocalDateTime now = LocalDateTime.now();
    outstanding.forEach(token -> token.setUsedAt(now));
    tokenRepository.saveAll(outstanding);

    String rawToken = SecureTokens.newToken();
    tokenRepository.save(
        PasswordResetToken.builder()
            .userId(user.getId())
            .tokenHash(SecureTokens.sha256Hex(rawToken))
            .expiresAt(now.plusMinutes(TTL_MINUTES))
            .createdAt(now)
            .build());

    mailPort.sendResetLink(user.getEmail(), rawToken);
    log.info("Password reset link issued for user id={}", user.getId());
  }

  @Transactional
  public void resetPassword(ResetPasswordRequest request) {
    PasswordResetToken token =
        tokenRepository
            .findByTokenHash(SecureTokens.sha256Hex(request.token()))
            .orElseThrow(() -> new InvalidResetTokenException("Unknown reset token"));

    if (token.getUsedAt() != null || token.getExpiresAt().isBefore(LocalDateTime.now())) {
      throw new InvalidResetTokenException("Reset token used or expired");
    }

    User user =
        userRepository
            .findById(token.getUserId())
            .orElseThrow(() -> new InvalidResetTokenException("Reset token has no user"));

    user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
    userRepository.save(user);

    token.setUsedAt(LocalDateTime.now());
    tokenRepository.save(token);
    log.info("Password reset completed for user id={}", user.getId());
  }
}
