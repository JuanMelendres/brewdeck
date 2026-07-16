package com.brewdeck.brewdeck_api.featureflag;

/**
 * Lifecycle/intent classification of a feature flag. The type also drives the HTTP status a
 * disabled feature returns (see {@link FeatureDisabledException}): operational kill switches read
 * as {@code 503}, everything else as {@code 404}.
 */
public enum FlagType {
  /** Temporary flag hiding incomplete/unreleased functionality. Most flags are this. */
  RELEASE,
  /** A/B or trial behaviour being measured. */
  EXPERIMENT,
  /** Operational toggle (throttle, degraded mode) controlled by operators. */
  OPERATIONAL,
  /** Entitlement-style gate. Never a substitute for real authorization. */
  PERMISSION,
  /** Emergency off switch for a live capability. */
  KILL_SWITCH
}
