package com.brewdeck.brewdeck_api.auth;

/**
 * Authorization role of a {@link User}. Exposed to Spring Security as a {@code ROLE_}-prefixed
 * authority by {@code JwtAuthenticationFilter}.
 */
public enum Role {
  USER,
  ADMIN;

  public String authority() {
    return "ROLE_" + name();
  }
}
