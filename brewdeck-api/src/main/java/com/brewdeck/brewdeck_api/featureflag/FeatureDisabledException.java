package com.brewdeck.brewdeck_api.featureflag;

import org.springframework.http.HttpStatus;

/**
 * Thrown when a flag-protected operation is invoked while its feature is disabled. The HTTP status
 * depends on flag type: operational kill switches surface as {@code 503 Service Unavailable}
 * (temporarily off), everything else as {@code 404 Not Found} (the feature is not discoverable).
 * Never {@code 500}.
 */
public class FeatureDisabledException extends RuntimeException {

  private final transient String featureKey;
  private final transient HttpStatus status;

  public FeatureDisabledException(String featureKey, HttpStatus status) {
    super("Feature '" + featureKey + "' is disabled");
    this.featureKey = featureKey;
    this.status = status;
  }

  public String getFeatureKey() {
    return featureKey;
  }

  public HttpStatus getStatus() {
    return status;
  }

  /** Maps a flag type to the status a disabled feature of that type should return. */
  public static HttpStatus statusFor(FlagType flagType) {
    if (flagType == FlagType.OPERATIONAL || flagType == FlagType.KILL_SWITCH) {
      return HttpStatus.SERVICE_UNAVAILABLE;
    }
    return HttpStatus.NOT_FOUND;
  }
}
