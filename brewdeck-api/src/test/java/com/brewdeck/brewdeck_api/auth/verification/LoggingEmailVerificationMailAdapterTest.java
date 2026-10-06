package com.brewdeck.brewdeck_api.auth.verification;

import static org.assertj.core.api.Assertions.assertThatCode;

import com.brewdeck.brewdeck_api.auth.reset.MailProperties;
import java.util.Locale;
import org.junit.jupiter.api.Test;

class LoggingEmailVerificationMailAdapterTest {

  @Test
  void sendVerificationLink_logsWithoutThrowing() {
    LoggingEmailVerificationMailAdapter adapter =
        new LoggingEmailVerificationMailAdapter(
            new MailProperties(
                false, "http://localhost:3000", "BrewDeck <no-reply@brewdeck.local>"));

    assertThatCode(
            () -> adapter.sendVerificationLink("brewer@example.com", "raw-token", Locale.ENGLISH))
        .doesNotThrowAnyException();
  }
}
