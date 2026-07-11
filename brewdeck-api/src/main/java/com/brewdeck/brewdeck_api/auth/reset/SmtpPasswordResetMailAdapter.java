package com.brewdeck.brewdeck_api.auth.reset;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Activated by {@code brewdeck.mail.enabled=true}. Placeholder for a real JavaMailSender
 * integration; wiring an actual SMTP provider is a documented follow-up to Slice C.2.
 */
@Component
@Slf4j
@ConditionalOnProperty(prefix = "brewdeck.mail", name = "enabled", havingValue = "true")
public class SmtpPasswordResetMailAdapter implements PasswordResetMailPort {

  @Override
  public void sendResetLink(String email, String rawToken) {
    // TODO(C.2 follow-up): send a real transactional email via JavaMailSender.
    log.warn("SMTP mail adapter enabled but not implemented; reset link for {} not sent", email);
    throw new UnsupportedOperationException("SMTP password-reset delivery is not implemented yet");
  }
}
