package com.brewdeck.brewdeck_api.account;

import java.util.Locale;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Default adapter whenever {@code brewdeck.mail.enabled} is false or absent: logs instead. */
@Component
@Slf4j
@ConditionalOnProperty(
    prefix = "brewdeck.mail",
    name = "enabled",
    havingValue = "false",
    matchIfMissing = true)
public class LoggingAccountMailAdapter implements AccountMailPort {

  @Override
  public void sendAccountDeletedNotice(String email, Locale locale) {
    log.info("Account-deleted notice for {}", email);
  }
}
