package com.brewdeck.brewdeck_api.auth;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Resolves the {@link User} behind the current request from the security context.
 *
 * <p>The JWT filter authenticates using the user's email as the principal name (see {@code
 * JwtAuthenticationFilter}), so a lookup by email always succeeds for genuinely authenticated
 * requests. A missing user therefore signals a misconfiguration rather than an expected state.
 */
@Component
public class CurrentUserProvider {

  private final UserRepository userRepository;

  public CurrentUserProvider(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  /**
   * Returns the authenticated user.
   *
   * @throws IllegalStateException if there is no authenticated principal or it cannot be resolved
   *     to a persisted user.
   */
  public User require() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !authentication.isAuthenticated()) {
      throw new IllegalStateException("No authenticated user in the security context");
    }

    String email = authentication.getName();
    return userRepository
        .findByEmail(email)
        .orElseThrow(
            () -> new IllegalStateException("Authenticated principal has no user: " + email));
  }
}
