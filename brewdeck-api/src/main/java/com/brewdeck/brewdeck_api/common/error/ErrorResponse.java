package com.brewdeck.brewdeck_api.common.error;

import java.time.Instant;
import java.util.Map;

/**
 * Standard error body. {@code code} is an optional machine-readable reason for clients that must
 * react to one specific error (e.g. {@code EMAIL_NOT_VERIFIED}); it is {@code null} otherwise.
 */
public record ErrorResponse(
    Instant timestamp,
    int status,
    String error,
    String message,
    String path,
    Map<String, String> validationErrors,
    String code) {

  public ErrorResponse(
      Instant timestamp,
      int status,
      String error,
      String message,
      String path,
      Map<String, String> validationErrors) {
    this(timestamp, status, error, message, path, validationErrors, null);
  }
}
