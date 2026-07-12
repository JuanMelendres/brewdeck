package com.brewdeck.brewdeck_api.auth.reset;

/** Raised when a reset token is unknown, already used, or expired. */
public class InvalidResetTokenException extends RuntimeException {
  public InvalidResetTokenException(String message) {
    super(message);
  }
}
