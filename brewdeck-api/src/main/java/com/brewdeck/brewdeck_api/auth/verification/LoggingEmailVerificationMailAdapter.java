package com.brewdeck.brewdeck_api.auth.verification;

import com.brewdeck.brewdeck_api.auth.reset.MailProperties;
import java.time.Instant;
import java.util.Locale;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Default adapter used whenever {@code brewdeck.mail.enabled} is false or absent. Logs the
 * verification link instead of sending an email, so the flow works with no SMTP dependency.
 */
@Component
@Slf4j
@ConditionalOnProperty(
    prefix = "brewdeck.mail",
    name = "enabled",
    havingValue = "false",
    matchIfMissing = true)
public class LoggingEmailVerificationMailAdapter implements EmailVerificationMailPort {

  private final MailProperties properties;

  public LoggingEmailVerificationMailAdapter(MailProperties properties) {
    this.properties = properties;
  }

  @Override
  public void sendVerificationLink(String email, String rawToken, Locale locale) {
    log.info(
        "Email verification link for {}: {}/verify-email?token={}",
        email,
        properties.frontendBaseUrl(),
        rawToken);
  }

  @Override
  public void sendAccountExistsNotice(String email, Locale locale) {
    log.info(
        "Account-exists notice for {}: log in at {}/login or reset the password at"
            + " {}/forgot-password",
        email,
        properties.frontendBaseUrl(),
        properties.frontendBaseUrl());
  }

  @Override
  public void sendRetentionReminder(
      String email, String rawToken, Instant deleteAfter, Locale locale) {
    log.info(
        "Retention reminder for {} (deleted on or after {}): {}/verify-email?token={}",
        email,
        deleteAfter,
        properties.frontendBaseUrl(),
        rawToken);
  }
}
