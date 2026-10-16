package com.brewdeck.brewdeck_api.auth;

import com.brewdeck.brewdeck_api.auth.refresh.InvalidRefreshTokenException;
import com.brewdeck.brewdeck_api.auth.refresh.RefreshTokenService;
import com.brewdeck.brewdeck_api.auth.verification.EmailVerificationService;
import com.brewdeck.brewdeck_api.common.ratelimit.RateLimitRule;
import com.brewdeck.brewdeck_api.common.ratelimit.RateLimiter;
import com.brewdeck.brewdeck_api.common.security.SecureTokens;
import jakarta.persistence.EntityNotFoundException;
import java.time.Instant;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class AuthService {

  private static final String INVALID_CREDENTIALS = "Invalid email or password";

  private final UserRepository userRepository;
  private final JwtService jwtService;
  private final PasswordEncoder passwordEncoder;
  private final EmailVerificationService emailVerificationService;
  private final RefreshTokenService refreshTokenService;
  private final RateLimiter rateLimiter;

  /**
   * A real hash of a random password with the same encoder (and cost) as stored hashes. Login
   * checks against it when the email is unknown, so "no such user" costs the same BCrypt work as
   * "wrong password" and response time does not reveal which emails are registered.
   */
  private final String dummyPasswordHash;

  public AuthService(
      UserRepository userRepository,
      JwtService jwtService,
      PasswordEncoder passwordEncoder,
      EmailVerificationService emailVerificationService,
      RefreshTokenService refreshTokenService,
      RateLimiter rateLimiter) {
    this.userRepository = userRepository;
    this.jwtService = jwtService;
    this.passwordEncoder = passwordEncoder;
    this.emailVerificationService = emailVerificationService;
    this.refreshTokenService = refreshTokenService;
    this.rateLimiter = rateLimiter;
    this.dummyPasswordHash = passwordEncoder.encode(SecureTokens.newToken());
  }

  /**
   * Signs up without revealing whether the email is already registered (AUTH-05.7): the caller
   * always gets the same answer and no session. A new address gets an account and a verification
   * link; an existing one gets a notice to log in or reset the password instead. Both paths run one
   * BCrypt hash so timing does not tell them apart, and the per-email limit applies to both.
   */
  @Transactional
  public void register(RegisterRequest request) {
    rateLimiter.requireAllowed(RateLimitRule.REGISTER_EMAIL, request.email());
    String passwordHash = passwordEncoder.encode(request.password());
    Optional<User> existing = userRepository.findByEmail(request.email());
    if (existing.isPresent()) {
      User user = existing.get();
      try {
        emailVerificationService.sendAccountExistsNotice(user);
      } catch (RuntimeException e) {
        log.warn(
            "Failed to send account-exists notice for user id={}: {}", user.getId(), e.toString());
      }
      log.info("Signup attempted for existing user id={}", user.getId());
      return;
    }
    User saved =
        userRepository.save(
            User.builder()
                .email(request.email())
                .passwordHash(passwordHash)
                .createdAt(Instant.now())
                .build());
    log.info("Registered user id={}", saved.getId());
    try {
      emailVerificationService.issueFor(saved);
    } catch (RuntimeException e) {
      log.warn(
          "Failed to issue verification token for user id={}: {}", saved.getId(), e.toString());
    }
  }

  @Transactional
  public AuthSession login(LoginRequest request) {
    // Per-account limit, checked before the password: rotating IPs does not help an attacker.
    rateLimiter.requireAllowed(RateLimitRule.LOGIN_EMAIL, request.email());
    Optional<User> maybeUser = userRepository.findByEmail(request.email());
    // Always run exactly one BCrypt comparison, against the dummy hash when the user is unknown,
    // so the two failure cases are indistinguishable by timing as well as by response body.
    String hashToCheck = maybeUser.map(User::getPasswordHash).orElse(dummyPasswordHash);
    boolean passwordMatches = passwordEncoder.matches(request.password(), hashToCheck);
    if (maybeUser.isEmpty() || !passwordMatches) {
      throw new BadCredentialsException(INVALID_CREDENTIALS);
    }
    User user = maybeUser.get();
    return tokenResponse(user, refreshTokenService.issue(user));
  }

  @Transactional(readOnly = true)
  public UserResponse me(String email) {
    return userRepository
        .findByEmail(email)
        .map(UserResponse::fromEntity)
        .orElseThrow(() -> new EntityNotFoundException("User not found"));
  }

  @Transactional
  public UserResponse updateProfile(String email, UpdateProfileRequest request) {
    User user = requireByEmail(email);
    user.setDisplayName(request.displayName());
    User saved = userRepository.save(user);
    log.info("Updated profile for user id={}", saved.getId());
    return UserResponse.fromEntity(saved);
  }

  @Transactional
  public UserResponse updateTheme(String email, UpdateThemeRequest request) {
    User user = requireByEmail(email);
    user.setThemePreference(request.themePreference());
    User saved = userRepository.save(user);
    log.info("Updated theme preference for user id={}", saved.getId());
    return UserResponse.fromEntity(saved);
  }

  @Transactional
  public UserResponse updateLanguage(String email, UpdateLanguageRequest request) {
    User user = requireByEmail(email);
    user.setLanguage(request.language());
    User saved = userRepository.save(user);
    log.info("Updated language for user id={}", saved.getId());
    return UserResponse.fromEntity(saved);
  }

  @Transactional
  public void changePassword(String email, ChangePasswordRequest request) {
    User user = requireByEmail(email);
    if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
      throw new InvalidCurrentPasswordException("Current password is incorrect");
    }
    user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
    userRepository.save(user);
    refreshTokenService.revokeAllForUser(user.getId());
    log.info("Changed password for user id={}", user.getId());
  }

  // No @Transactional here on purpose: rotate() must be the outermost transaction boundary so its
  // noRollbackFor governs the reuse-path revocation. A plain @Transactional here would join
  // rotate()'s transaction and roll it back on InvalidRefreshTokenException, undoing the
  // revocation.
  // refresh() has no other DB write of its own (it only rotates, then generates a JWT).
  public AuthSession refresh(String rawRefreshToken) {
    if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
      throw new InvalidRefreshTokenException("Missing refresh token");
    }
    RefreshTokenService.RotationResult result = refreshTokenService.rotate(rawRefreshToken);
    return tokenResponse(result.user(), result.rawToken());
  }

  @Transactional
  public void logout(String email, String rawRefreshToken) {
    User user = requireByEmail(email);
    if (rawRefreshToken != null && !rawRefreshToken.isBlank()) {
      refreshTokenService.revoke(rawRefreshToken, user.getId());
    }
  }

  private User requireByEmail(String email) {
    return userRepository
        .findByEmail(email)
        .orElseThrow(() -> new EntityNotFoundException("User not found"));
  }

  private AuthSession tokenResponse(User user, String refreshToken) {
    String token = jwtService.generateToken(user);
    return new AuthSession(
        new AuthResponse(token, jwtService.expiryFor(Instant.now()), user.getEmail()),
        refreshToken);
  }
}
