package com.brewdeck.brewdeck_api.auth.verification;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Activated by {@code brewdeck.mail.enabled=true}. Placeholder for a real JavaMailSender
 * integration; wiring an actual SMTP provider is a documented follow-up.
 */
@Component
@Slf4j
@ConditionalOnProperty(prefix = "brewdeck.mail", name = "enabled", havingValue = "true")
public class SmtpEmailVerificationMailAdapter implements EmailVerificationMailPort {

  @Override
  public void sendVerificationLink(String email, String rawToken) {
    // TODO(C.3 follow-up): send a real transactional email via JavaMailSender.
    log.warn(
        "SMTP mail adapter enabled but not implemented; verification link for {} not sent", email);
    throw new UnsupportedOperationException("SMTP verification delivery is not implemented yet");
  }
}
