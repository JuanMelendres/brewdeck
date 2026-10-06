package com.brewdeck.brewdeck_api.common.i18n;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import org.junit.jupiter.api.Test;

/** Every API message exists in English and Spanish (ADR-015). */
class MessagesParityTest {

  private static Properties load(String name) throws IOException {
    Properties properties = new Properties();
    try (InputStream in = MessagesParityTest.class.getResourceAsStream("/" + name)) {
      assertThat(in).as(name).isNotNull();
      properties.load(new InputStreamReader(in, StandardCharsets.UTF_8));
    }
    return properties;
  }

  @Test
  void spanishHasExactlyTheEnglishKeys() throws IOException {
    assertThat(load("messages_es.properties").stringPropertyNames())
        .containsExactlyInAnyOrderElementsOf(load("messages.properties").stringPropertyNames());
  }
}
