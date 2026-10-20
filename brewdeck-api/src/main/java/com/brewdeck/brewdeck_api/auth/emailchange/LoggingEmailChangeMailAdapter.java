package com.brewdeck.brewdeck_api.auth.emailchange;

import com.brewdeck.brewdeck_api.auth.reset.MailProperties;
import java.util.Locale;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Default adapter used whenever {@code brewdeck.mail.enabled} is false or absent. Logs the links
 * instead of sending email, so the flow works with no SMTP dependency.
 */
@Component
@Slf4j
@ConditionalOnProperty(
    prefix = "brewdeck.mail",
    name = "enabled",
    havingValue = "false",
    matchIfMissing = true)
public class LoggingEmailChangeMailAdapter implements EmailChangeMailPort {

  private final MailProperties properties;

  public LoggingEmailChangeMailAdapter(MailProperties properties) {
    this.properties = properties;
  }

  @Override
  public void sendConfirmLink(String newEmail, String rawToken, Locale locale) {
    log.info(
        "Email change confirm link for {}: {}/email-change/confirm?token={}",
        newEmail,
        properties.frontendBaseUrl(),
        rawToken);
  }

  @Override
  public void sendChangeRequestedNotice(
      String oldEmail, String maskedNewEmail, String rawCancelToken, Locale locale) {
    log.info(
        "Email change to {} requested for {}; cancel at {}/email-change/cancel?token={}",
        maskedNewEmail,
        oldEmail,
        properties.frontendBaseUrl(),
        rawCancelToken);
  }

  @Override
  public void sendAddressTakenNotice(String newEmail, Locale locale) {
    log.info("Email change address-taken notice for {}", newEmail);
  }

  @Override
  public void sendEmailChangedNotice(String oldEmail, String maskedNewEmail, Locale locale) {
    log.info("Email changed notice for {}: the account now uses {}", oldEmail, maskedNewEmail);
  }
}
