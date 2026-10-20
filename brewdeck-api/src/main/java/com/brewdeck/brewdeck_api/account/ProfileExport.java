package com.brewdeck.brewdeck_api.account;

import com.brewdeck.brewdeck_api.auth.Language;
import com.brewdeck.brewdeck_api.auth.ThemePreference;
import com.brewdeck.brewdeck_api.auth.User;
import java.time.Instant;

/** The account part of the data export (ADR-020). Never includes the password or token hashes. */
public record ProfileExport(
    String email,
    String displayName,
    Language language,
    ThemePreference themePreference,
    boolean emailVerified,
    Instant createdAt,
    String termsVersion,
    Instant termsAcceptedAt) {

  static ProfileExport fromEntity(User user) {
    return new ProfileExport(
        user.getEmail(),
        user.getDisplayName(),
        user.getLanguage(),
        user.getThemePreference(),
        user.isEmailVerified(),
        user.getCreatedAt(),
        user.getTermsVersion(),
        user.getTermsAcceptedAt());
  }
}
