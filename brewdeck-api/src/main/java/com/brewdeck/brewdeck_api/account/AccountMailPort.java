package com.brewdeck.brewdeck_api.account;

import java.util.Locale;

/** Account lifecycle emails (ADR-020). Swap adapters via {@code brewdeck.mail.enabled}. */
public interface AccountMailPort {

  /** Confirms to the deleted address that the account and its data are gone. It has no links. */
  void sendAccountDeletedNotice(String email, Locale locale);
}
