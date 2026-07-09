package com.brewdeck.brewdeck_api.ai;

public class InsufficientBrewHistoryException extends RuntimeException {

  public InsufficientBrewHistoryException(String message) {
    super(message);
  }
}
