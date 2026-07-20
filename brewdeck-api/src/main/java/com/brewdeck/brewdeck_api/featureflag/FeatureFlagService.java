package com.brewdeck.brewdeck_api.featureflag;

/**
 * Reads feature flag state for the active environment. The backend is the source of truth: business
 * operations must consult this before running flag-protected side effects, not rely on the frontend
 * hiding UI.
 *
 * <p>Fails safe — an unknown flag, a disabled flag, or a datastore error all resolve to {@code
 * false}. An unknown feature is never enabled automatically.
 */
public interface FeatureFlagService {

  /** Whether the flag is enabled with no targeting context (server-side, no user in scope). */
  boolean isEnabled(String featureKey);

  /** Whether the flag is enabled for the given targeting context. */
  boolean isEnabled(String featureKey, FeatureFlagContext context);

  /** Full evaluation (enabled + reason) for diagnostics and the frontend endpoint. */
  FeatureFlagEvaluation evaluate(String featureKey, FeatureFlagContext context);

  /**
   * Throws {@link FeatureDisabledException} when the flag is not enabled. Call this at the business
   * boundary before any protected side effect (persistence, events, external calls, notifications).
   */
  default void requireEnabled(String featureKey) {
    requireEnabled(featureKey, FeatureFlagContext.empty());
  }

  /** Context-aware variant of {@link #requireEnabled(String)}. */
  void requireEnabled(String featureKey, FeatureFlagContext context);
}
