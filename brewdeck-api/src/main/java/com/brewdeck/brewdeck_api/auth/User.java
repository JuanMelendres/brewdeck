package com.brewdeck.brewdeck_api.auth;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.*;

@Entity
@Table(name = "users")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true)
  private String email;

  @Column(name = "password_hash", nullable = false)
  private String passwordHash;

  @Column(name = "display_name", length = 100)
  private String displayName;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  @Builder.Default
  private Role role = Role.USER;

  @Enumerated(EnumType.STRING)
  @Column(name = "theme_preference", length = 10)
  private ThemePreference themePreference;

  @Enumerated(EnumType.STRING)
  @Column(length = 5)
  private Language language;

  @Column(name = "email_verified", nullable = false)
  private boolean emailVerified;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  /** Embedded in every access JWT; bumping it invalidates all access tokens issued before. */
  @Column(name = "token_version", nullable = false)
  private int tokenVersion;

  /** The terms version the user accepted (LEGAL-01); null before the texts were published. */
  @Column(name = "terms_version", length = 20)
  private String termsVersion;

  @Column(name = "terms_accepted_at")
  private Instant termsAcceptedAt;

  public void revokeAccessTokens() {
    tokenVersion++;
  }
}
