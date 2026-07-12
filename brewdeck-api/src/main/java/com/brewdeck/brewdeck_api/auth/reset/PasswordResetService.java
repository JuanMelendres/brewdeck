package com.brewdeck.brewdeck_api.auth.reset;

import com.brewdeck.brewdeck_api.auth.User;
import com.brewdeck.brewdeck_api.auth.UserRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class PasswordResetService {

  private static final SecureRandom SECURE_RANDOM = new SecureRandom();
  private static final int TOKEN_BYTES = 32;
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

    String rawToken = generateRawToken();
    tokenRepository.save(
        PasswordResetToken.builder()
            .userId(user.getId())
            .tokenHash(hash(rawToken))
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
            .findByTokenHash(hash(request.token()))
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

  private String generateRawToken() {
    byte[] bytes = new byte[TOKEN_BYTES];
    SECURE_RANDOM.nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  private String hash(String rawToken) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hashed = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(hashed);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 unavailable", e);
    }
  }
}
