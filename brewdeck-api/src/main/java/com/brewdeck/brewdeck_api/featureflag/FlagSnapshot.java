package com.brewdeck.brewdeck_api.featureflag;

/**
 * Immutable, cache-friendly projection of a flag row (or its absence). Kept minimal and detached so
 * it is safe to hold in a Caffeine cache without dragging a JPA entity out of its session.
 */
record FlagSnapshot(
    boolean present,
    boolean enabled,
    int rolloutPercentage,
    FlagType flagType,
    boolean datastoreError) {

  private static final FlagSnapshot ABSENT = new FlagSnapshot(false, false, 0, null, false);
  private static final FlagSnapshot DATASTORE_ERROR = new FlagSnapshot(false, false, 0, null, true);

  static FlagSnapshot absent() {
    return ABSENT;
  }

  static FlagSnapshot datastoreUnavailable() {
    return DATASTORE_ERROR;
  }

  static FlagSnapshot of(FeatureFlag flag) {
    return new FlagSnapshot(
        true, flag.isEnabled(), flag.getRolloutPercentage(), flag.getFlagType(), false);
  }
}
