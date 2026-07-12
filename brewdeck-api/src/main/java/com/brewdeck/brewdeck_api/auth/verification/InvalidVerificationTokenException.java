package com.brewdeck.brewdeck_api.auth.verification;

/** Raised when a verification token is unknown, already used, or expired. */
public class InvalidVerificationTokenException extends RuntimeException {
  public InvalidVerificationTokenException(String message) {
    super(message);
  }
}
