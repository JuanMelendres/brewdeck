package com.brewdeck.brewdeck_api.auth.reset;

import static org.assertj.core.api.Assertions.assertThatCode;

import org.junit.jupiter.api.Test;

class LoggingPasswordResetMailAdapterTest {

  @Test
  void sendResetLink_logsWithoutThrowing() {
    LoggingPasswordResetMailAdapter adapter =
        new LoggingPasswordResetMailAdapter(new MailProperties(false, "http://localhost:3000"));

    assertThatCode(() -> adapter.sendResetLink("brewer@example.com", "raw-token"))
        .doesNotThrowAnyException();
  }
}
