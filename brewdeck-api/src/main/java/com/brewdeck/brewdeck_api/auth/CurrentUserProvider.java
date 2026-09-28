package com.brewdeck.brewdeck_api.auth;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Resolves the {@link User} behind the current request from the security context.
 *
 * <p>For JWT-authenticated requests the filter has already loaded the user and stored an {@link
 * AuthenticatedUser} principal, so this returns a JPA <b>reference</b> built from its id: no query.
 * Callers here only need the id ({@code require().getId()}) or an owner to link ({@code
 * setOwner(require())}), and a reference serves both. Do not read other fields (email, role, ...)
 * from the result outside a transaction; use {@link UserRepository} if you need the full row.
 *
 * <p>Any other principal (e.g. {@code @WithMockUser} in tests) falls back to a lookup by name.
 */
@Component
public class CurrentUserProvider {

  private final UserRepository userRepository;

  public CurrentUserProvider(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  /**
   * Returns the authenticated user (a reference when the JWT filter resolved it).
   *
   * @throws IllegalStateException if there is no authenticated principal or it cannot be resolved
   *     to a persisted user.
   */
  public User require() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !authentication.isAuthenticated()) {
      throw new IllegalStateException("No authenticated user in the security context");
    }

    if (authentication.getPrincipal() instanceof AuthenticatedUser principal) {
      return userRepository.getReferenceById(principal.id());
    }

    String email = authentication.getName();
    return userRepository
        .findByEmail(email)
        .orElseThrow(
            () -> new IllegalStateException("Authenticated principal has no user: " + email));
  }
}
