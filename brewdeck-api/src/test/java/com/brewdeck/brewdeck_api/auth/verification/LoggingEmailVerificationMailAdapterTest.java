package com.brewdeck.brewdeck_api.auth.verification;

import static org.assertj.core.api.Assertions.assertThatCode;

import com.brewdeck.brewdeck_api.auth.reset.MailProperties;
import org.junit.jupiter.api.Test;

class LoggingEmailVerificationMailAdapterTest {

  @Test
  void sendVerificationLink_logsWithoutThrowing() {
    LoggingEmailVerificationMailAdapter adapter =
        new LoggingEmailVerificationMailAdapter(new MailProperties(false, "http://localhost:3000"));

    assertThatCode(() -> adapter.sendVerificationLink("brewer@example.com", "raw-token"))
        .doesNotThrowAnyException();
  }
}
