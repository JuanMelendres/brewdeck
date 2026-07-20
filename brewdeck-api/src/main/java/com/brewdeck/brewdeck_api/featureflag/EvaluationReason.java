package com.brewdeck.brewdeck_api.featureflag;

/** Why an evaluation resolved the way it did. Useful for logging and diagnostics. */
public enum EvaluationReason {
  /** No row for this key in the active environment — treated as disabled (fail safe). */
  FLAG_NOT_FOUND,
  /** Row exists and is disabled. */
  DISABLED,
  /** Row exists, enabled, and the subject falls inside the rollout bucket. */
  ENABLED,
  /** Row exists and enabled, but the subject falls outside the rollout percentage. */
  ROLLOUT_EXCLUDED,
  /** Data access failed and no cached value was available — defaulted to disabled. */
  DATASTORE_UNAVAILABLE
}
