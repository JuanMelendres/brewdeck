package com.brewdeck.brewdeck_api.featureflag;

import lombok.Builder;

/**
 * Evaluation context for targeting. Every field is optional; unknown attributes are {@code null}.
 * Today only {@link #userId()} / {@link #anonymousSessionId()} feed deterministic rollout, but the
 * shape is ready for role/plan/org/country/version/platform targeting without a signature change.
 *
 * <p>A feature flag is never a replacement for authorization — authorization is enforced
 * independently regardless of what any of these attributes say.
 */
@Builder
public record FeatureFlagContext(
    Long userId,
    String role,
    String subscriptionPlan,
    Long organizationId,
    String country,
    String appVersion,
    String platform,
    String anonymousSessionId) {

  private static final FeatureFlagContext EMPTY = FeatureFlagContext.builder().build();

  /** Context with no attributes — server-side checks with no user in scope. */
  public static FeatureFlagContext empty() {
    return EMPTY;
  }

  /** Context for a known authenticated user (drives deterministic rollout bucketing). */
  public static FeatureFlagContext ofUser(Long userId) {
    return FeatureFlagContext.builder().userId(userId).build();
  }

  /**
   * The stable identifier used for rollout bucketing: the user id when present, otherwise the
   * anonymous session id, otherwise {@code null} (no stable subject).
   */
  public String rolloutSubject() {
    if (userId != null) {
      return "user:" + userId;
    }
    if (anonymousSessionId != null && !anonymousSessionId.isBlank()) {
      return "anon:" + anonymousSessionId;
    }
    return null;
  }
}
