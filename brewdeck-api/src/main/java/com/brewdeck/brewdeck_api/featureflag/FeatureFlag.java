package com.brewdeck.brewdeck_api.featureflag;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * One feature flag scoped to a single environment. The pair {@code (featureKey, environment)} is
 * unique, so the same capability can be enabled in {@code dev} and disabled in {@code prod}.
 *
 * <p>Never store secrets, tokens, or user PII in {@link #configuration}; it is plain JSONB.
 */
@Entity
@Table(name = "feature_flags")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeatureFlag {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "feature_key", nullable = false, length = 150)
  private String featureKey;

  @Column(name = "display_name", nullable = false, length = 200)
  private String displayName;

  @Column(length = 500)
  private String description;

  @Column(nullable = false, length = 50)
  private String environment;

  @Column(nullable = false)
  private boolean enabled;

  @Enumerated(EnumType.STRING)
  @Column(name = "flag_type", nullable = false, length = 50)
  private FlagType flagType;

  @Column(name = "rollout_percentage", nullable = false)
  private int rolloutPercentage;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(columnDefinition = "jsonb")
  private String configuration;

  @Column(length = 150)
  private String owner;

  @Column(name = "expires_at")
  private LocalDateTime expiresAt;

  @Column(name = "removal_condition", length = 500)
  private String removalCondition;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  @PrePersist
  void onCreate() {
    LocalDateTime now = LocalDateTime.now();
    this.createdAt = now;
    this.updatedAt = now;
    if (this.flagType == null) {
      this.flagType = FlagType.RELEASE;
    }
  }

  @PreUpdate
  void onUpdate() {
    this.updatedAt = LocalDateTime.now();
  }
}
