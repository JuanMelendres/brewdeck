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

  @Column(name = "email_verified", nullable = false)
  private boolean emailVerified;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;
}
