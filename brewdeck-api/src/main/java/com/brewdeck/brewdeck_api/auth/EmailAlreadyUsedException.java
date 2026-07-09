package com.brewdeck.brewdeck_api.auth;

public class EmailAlreadyUsedException extends RuntimeException {
  public EmailAlreadyUsedException(String message) {
    super(message);
  }
}
