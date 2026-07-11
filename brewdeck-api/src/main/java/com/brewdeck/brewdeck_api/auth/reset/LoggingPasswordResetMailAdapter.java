package com.brewdeck.brewdeck_api.auth.reset;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Default adapter used whenever {@code brewdeck.mail.enabled} is false or absent. Logs the reset
 * link instead of sending an email, so the full flow works with no SMTP dependency.
 */
@Component
@Slf4j
@ConditionalOnProperty(
    prefix = "brewdeck.mail",
    name = "enabled",
    havingValue = "false",
    matchIfMissing = true)
public class LoggingPasswordResetMailAdapter implements PasswordResetMailPort {

  private final MailProperties properties;

  public LoggingPasswordResetMailAdapter(MailProperties properties) {
    this.properties = properties;
  }

  @Override
  public void sendResetLink(String email, String rawToken) {
    log.info(
        "Password reset link for {}: {}/reset-password?token={}",
        email,
        properties.frontendBaseUrl(),
        rawToken);
  }
}
