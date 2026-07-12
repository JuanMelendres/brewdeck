package com.brewdeck.brewdeck_api.auth.verification;

import com.brewdeck.brewdeck_api.auth.User;
import com.brewdeck.brewdeck_api.auth.UserRepository;
import com.brewdeck.brewdeck_api.common.security.SecureTokens;
import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class EmailVerificationService {

  private static final int TTL_HOURS = 24;

  private final EmailVerificationTokenRepository tokenRepository;
  private final UserRepository userRepository;
  private final EmailVerificationMailPort mailPort;

  public EmailVerificationService(
      EmailVerificationTokenRepository tokenRepository,
      UserRepository userRepository,
      EmailVerificationMailPort mailPort) {
    this.tokenRepository = tokenRepository;
    this.userRepository = userRepository;
    this.mailPort = mailPort;
  }

  /** Issues a fresh verification token for the user and sends the link best-effort. */
  @Transactional
  public void issueFor(User user) {
    List<EmailVerificationToken> outstanding =
        tokenRepository.findByUserIdAndUsedAtIsNull(user.getId());
    LocalDateTime now = LocalDateTime.now();
    outstanding.forEach(token -> token.setUsedAt(now));
    tokenRepository.saveAll(outstanding);

    String rawToken = SecureTokens.newToken();
    tokenRepository.save(
        EmailVerificationToken.builder()
            .userId(user.getId())
            .tokenHash(SecureTokens.sha256Hex(rawToken))
            .expiresAt(now.plusHours(TTL_HOURS))
            .createdAt(now)
            .build());

    // Best-effort: a mail failure must not fail registration or resend.
    try {
      mailPort.sendVerificationLink(user.getEmail(), rawToken);
    } catch (RuntimeException e) {
      log.warn("Failed to send verification email for user id={}: {}", user.getId(), e.toString());
    }
    log.info("Issued email verification token for user id={}", user.getId());
  }

  @Transactional
  public void verify(String rawToken) {
    EmailVerificationToken token =
        tokenRepository
            .findByTokenHash(SecureTokens.sha256Hex(rawToken))
            .orElseThrow(() -> new InvalidVerificationTokenException("Unknown verification token"));

    if (token.getUsedAt() != null || token.getExpiresAt().isBefore(LocalDateTime.now())) {
      throw new InvalidVerificationTokenException("Verification token used or expired");
    }

    User user =
        userRepository
            .findById(token.getUserId())
            .orElseThrow(
                () -> new InvalidVerificationTokenException("Verification token has no user"));

    user.setEmailVerified(true);
    userRepository.save(user);

    token.setUsedAt(LocalDateTime.now());
    tokenRepository.save(token);
    log.info("Email verified for user id={}", user.getId());
  }

  @Transactional
  public void resendFor(String email) {
    User user =
        userRepository
            .findByEmail(email)
            .orElseThrow(() -> new EntityNotFoundException("User not found"));
    if (user.isEmailVerified()) {
      log.info("Resend requested for already-verified user id={}; no-op", user.getId());
      return;
    }
    issueFor(user);
  }
}
