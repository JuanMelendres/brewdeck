package com.brewdeck.brewdeck_api.auth;

import java.time.Instant;

public record UserResponse(
    Long id,
    String email,
    String displayName,
    boolean emailVerified,
    Role role,
    ThemePreference themePreference,
    Language language,
    Instant createdAt) {
  public static UserResponse fromEntity(User user) {
    return new UserResponse(
        user.getId(),
        user.getEmail(),
        user.getDisplayName(),
        user.isEmailVerified(),
        user.getRole(),
        user.getThemePreference(),
        user.getLanguage(),
        user.getCreatedAt());
  }
}
