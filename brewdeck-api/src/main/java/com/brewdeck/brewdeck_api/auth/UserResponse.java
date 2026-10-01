package com.brewdeck.brewdeck_api.auth;

import java.time.Instant;

public record UserResponse(
    Long id,
    String email,
    String displayName,
    boolean emailVerified,
    Role role,
    ThemePreference themePreference,
    Instant createdAt) {
  public static UserResponse fromEntity(User user) {
    return new UserResponse(
        user.getId(),
        user.getEmail(),
        user.getDisplayName(),
        user.isEmailVerified(),
        user.getRole(),
        user.getThemePreference(),
        user.getCreatedAt());
  }
}
