package com.brewdeck.brewdeck_api.auth.emailchange;

/**
 * Raised when an email change link is unknown, expired, already used or cancelled, or when the new
 * address was taken before the change was confirmed. One error for all cases, so the answer does
 * not reveal which.
 */
public class InvalidEmailChangeTokenException extends RuntimeException {
  public InvalidEmailChangeTokenException(String message) {
    super(message);
  }
}
