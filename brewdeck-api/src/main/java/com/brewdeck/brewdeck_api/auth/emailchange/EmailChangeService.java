package com.brewdeck.brewdeck_api.auth.emailchange;

import com.brewdeck.brewdeck_api.auth.CurrentUserProvider;
import com.brewdeck.brewdeck_api.auth.InvalidCurrentPasswordException;
import com.brewdeck.brewdeck_api.auth.User;
import com.brewdeck.brewdeck_api.auth.UserLocale;
import com.brewdeck.brewdeck_api.auth.UserRepository;
import com.brewdeck.brewdeck_api.auth.refresh.RefreshTokenService;
import com.brewdeck.brewdeck_api.common.ratelimit.RateLimitRule;
import com.brewdeck.brewdeck_api.common.ratelimit.RateLimiter;
import com.brewdeck.brewdeck_api.common.security.SecureTokens;
import com.brewdeck.brewdeck_api.featureflag.FeatureFlagService;
import com.brewdeck.brewdeck_api.featureflag.FeatureKeys;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Email change with confirmation from the new address and a cancel link to the old one (ADR-019).
 * Neither email address nor any token is logged; only user ids.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class EmailChangeService {

  static final Duration TTL = Duration.ofHours(1);

  private final EmailChangeRepository emailChangeRepository;
  private final UserRepository userRepository;
  private final CurrentUserProvider currentUserProvider;
  private final PasswordEncoder passwordEncoder;
  private final RefreshTokenService refreshTokenService;
  private final EmailChangeMailPort mailPort;
  private final RateLimiter rateLimiter;
  private final FeatureFlagService featureFlagService;

  /**
   * Starts a change for the current user. The caller answers the same whether or not the new
   * address already has an account; only the emails differ.
   */
  @Transactional
  public void request(RequestEmailChangeRequest request) {
    featureFlagService.requireEnabled(FeatureKeys.EMAIL_CHANGE);
    User user = currentUserProvider.require();
    // Before the password check, so wrong passwords count: a stolen access token cannot be used to
    // guess the password here.
    rateLimiter.requireAllowed(RateLimitRule.EMAIL_CHANGE_USER, String.valueOf(user.getId()));
    if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
      throw new InvalidCurrentPasswordException("Current password is incorrect");
    }
    if (request.newEmail().equals(user.getEmail())) {
      throw new SameEmailException();
    }

    Locale locale = UserLocale.of(user);
    if (userRepository.findByEmail(request.newEmail()).isPresent()) {
      mailPort.sendAddressTakenNotice(request.newEmail(), locale);
      log.info(
          "Email change for user id={} asked for an address that has an account", user.getId());
      return;
    }

    Instant now = Instant.now();
    List<EmailChange> pending =
        emailChangeRepository.findByUserIdAndConfirmedAtIsNullAndCancelledAtIsNull(user.getId());
    pending.forEach(change -> change.setCancelledAt(now));
    emailChangeRepository.saveAll(pending);

    String confirmToken = SecureTokens.newToken();
    String cancelToken = SecureTokens.newToken();
    emailChangeRepository.save(
        EmailChange.builder()
            .userId(user.getId())
            .newEmail(request.newEmail())
            .confirmTokenHash(SecureTokens.sha256Hex(confirmToken))
            .cancelTokenHash(SecureTokens.sha256Hex(cancelToken))
            .expiresAt(now.plus(TTL))
            .createdAt(now)
            .build());

    mailPort.sendConfirmLink(request.newEmail(), confirmToken, locale);
    mailPort.sendChangeRequestedNotice(
        user.getEmail(), mask(request.newEmail()), cancelToken, locale);
    log.info("Email change requested for user id={}", user.getId());
  }

  /** Applies a pending change from the new address's link and closes every session. */
  @Transactional
  public void confirm(String rawToken) {
    featureFlagService.requireEnabled(FeatureKeys.EMAIL_CHANGE);
    EmailChange change =
        emailChangeRepository
            .findByConfirmTokenHash(SecureTokens.sha256Hex(rawToken))
            .orElseThrow(() -> new InvalidEmailChangeTokenException("Unknown confirm token"));
    if (emailChangeRepository.confirmIfPending(change.getId(), Instant.now()) == 0) {
      throw new InvalidEmailChangeTokenException("Email change used, cancelled or expired");
    }
    User user =
        userRepository
            .findById(change.getUserId())
            .orElseThrow(() -> new InvalidEmailChangeTokenException("Email change has no user"));
    // Throwing rolls the claim back. The unique constraint below covers a signup that lands
    // between this check and the flush.
    if (userRepository.findByEmail(change.getNewEmail()).isPresent()) {
      log.warn("Email change for user id={} lost its address to another account", user.getId());
      throw new InvalidEmailChangeTokenException("New address already has an account");
    }

    String oldEmail = user.getEmail();
    user.setEmail(change.getNewEmail());
    // Opening the link proved control of the new address.
    user.setEmailVerified(true);
    user.revokeAccessTokens();
    try {
      userRepository.saveAndFlush(user);
    } catch (DataIntegrityViolationException e) {
      log.warn("Email change for user id={} lost its address to another account", user.getId());
      throw new InvalidEmailChangeTokenException("New address already has an account");
    }
    refreshTokenService.revokeAllForUser(user.getId());

    mailPort.sendEmailChangedNotice(oldEmail, mask(change.getNewEmail()), UserLocale.of(user));
    log.info("Email changed for user id={}", user.getId());
  }

  /**
   * Stops a pending change from the old address's link and closes every session: a cancel means the
   * owner did not ask for it. Never guarded by the feature flag.
   */
  @Transactional
  public void cancel(String rawToken) {
    EmailChange change =
        emailChangeRepository
            .findByCancelTokenHash(SecureTokens.sha256Hex(rawToken))
            .orElseThrow(() -> new InvalidEmailChangeTokenException("Unknown cancel token"));
    if (emailChangeRepository.cancelIfPending(change.getId(), Instant.now()) == 0) {
      throw new InvalidEmailChangeTokenException("Email change used, cancelled or expired");
    }
    User user =
        userRepository
            .findById(change.getUserId())
            .orElseThrow(() -> new InvalidEmailChangeTokenException("Email change has no user"));
    user.revokeAccessTokens();
    userRepository.save(user);
    refreshTokenService.revokeAllForUser(user.getId());
    log.info("Email change cancelled for user id={}; sessions closed", user.getId());
  }

  /**
   * Masks an address for the notices: the first character of the local part, then {@code ***}, then
   * the domain ({@code juan@example.com} becomes {@code j***@example.com}).
   */
  static String mask(String email) {
    int at = email.indexOf('@');
    if (at <= 0) {
      return "***";
    }
    return email.charAt(0) + "***" + email.substring(at);
  }
}
