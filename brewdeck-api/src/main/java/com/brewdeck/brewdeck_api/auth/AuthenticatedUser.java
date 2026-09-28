package com.brewdeck.brewdeck_api.auth;

import java.security.Principal;

/**
 * The authenticated principal the JWT filter stores in the security context. It carries what the
 * filter already loaded (id, email, role), so the rest of the request can identify the user without
 * querying the database again. {@link #getName()} is the email, so {@code Principal.getName()} and
 * {@code Authentication.getName()} keep returning the email as before.
 */
public record AuthenticatedUser(Long id, String email, Role role) implements Principal {

  @Override
  public String getName() {
    return email;
  }
}
