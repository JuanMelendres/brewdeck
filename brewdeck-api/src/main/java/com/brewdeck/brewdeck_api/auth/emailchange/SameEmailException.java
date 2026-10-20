package com.brewdeck.brewdeck_api.auth.emailchange;

/**
 * Raised when the requested new email is the account's current email. Answered as a field error.
 */
public class SameEmailException extends RuntimeException {
  public SameEmailException() {
    super("New email is the current email");
  }
}
