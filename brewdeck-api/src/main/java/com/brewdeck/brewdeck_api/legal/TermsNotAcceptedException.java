package com.brewdeck.brewdeck_api.legal;

/**
 * Sign-up without accepting the current terms while they are published. Answered as a field error.
 */
public class TermsNotAcceptedException extends RuntimeException {
  public TermsNotAcceptedException() {
    super("The current terms were not accepted");
  }
}
