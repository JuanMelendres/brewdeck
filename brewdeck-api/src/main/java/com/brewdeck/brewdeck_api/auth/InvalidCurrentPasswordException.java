package com.brewdeck.brewdeck_api.auth;

/** Raised when a password-change request supplies the wrong current password. */
public class InvalidCurrentPasswordException extends RuntimeException {
  public InvalidCurrentPasswordException(String message) {
    super(message);
  }
}
