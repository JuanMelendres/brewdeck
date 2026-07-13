package com.brewdeck.brewdeck_api.auth;

import com.brewdeck.brewdeck_api.auth.verification.EmailVerificationService;
import jakarta.persistence.EntityNotFoundException;
import java.time.Instant;
import java.time.LocalDateTime;
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

  public AuthService(
      UserRepository userRepository,
      JwtService jwtService,
      PasswordEncoder passwordEncoder,
      EmailVerificationService emailVerificationService) {
    this.userRepository = userRepository;
    this.jwtService = jwtService;
    this.passwordEncoder = passwordEncoder;
    this.emailVerificationService = emailVerificationService;
  }

  @Transactional
  public AuthResponse register(RegisterRequest request) {
    if (userRepository.existsByEmail(request.email())) {
      throw new EmailAlreadyUsedException("Email is already registered");
    }
    User user =
        User.builder()
            .email(request.email())
            .passwordHash(passwordEncoder.encode(request.password()))
            .createdAt(LocalDateTime.now())
            .build();
    User saved = userRepository.save(user);
    log.info("Registered user id={}", saved.getId());
    try {
      emailVerificationService.issueFor(saved);
    } catch (RuntimeException e) {
      log.warn(
          "Failed to issue verification token for user id={}: {}", saved.getId(), e.toString());
    }
    return tokenResponse(saved);
  }

  @Transactional(readOnly = true)
  public AuthResponse login(LoginRequest request) {
    User user =
        userRepository
            .findByEmail(request.email())
            .orElseThrow(() -> new BadCredentialsException(INVALID_CREDENTIALS));
    if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
      throw new BadCredentialsException(INVALID_CREDENTIALS);
    }
    return tokenResponse(user);
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
  public void changePassword(String email, ChangePasswordRequest request) {
    User user = requireByEmail(email);
    if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
      throw new InvalidCurrentPasswordException("Current password is incorrect");
    }
    user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
    userRepository.save(user);
    log.info("Changed password for user id={}", user.getId());
  }

  private User requireByEmail(String email) {
    return userRepository
        .findByEmail(email)
        .orElseThrow(() -> new EntityNotFoundException("User not found"));
  }

  private AuthResponse tokenResponse(User user) {
    String token = jwtService.generateToken(user);
    return new AuthResponse(token, jwtService.expiryFor(Instant.now()), user.getEmail());
  }
}
