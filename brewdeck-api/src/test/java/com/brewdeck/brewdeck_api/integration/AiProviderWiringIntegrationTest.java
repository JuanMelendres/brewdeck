package com.brewdeck.brewdeck_api.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.brewdeck.brewdeck_api.ai.OllamaRecipeSuggestionAdapter;
import com.brewdeck.brewdeck_api.ai.RecipeSuggestionPort;
import com.brewdeck.brewdeck_api.common.PostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/** With provider=ollama the app wires the local-model adapter, and only it (ADR-016). */
@SpringBootTest(properties = {"brewdeck.ai.enabled=true", "brewdeck.ai.provider=ollama"})
@ActiveProfiles("test")
class AiProviderWiringIntegrationTest extends PostgresIntegrationTest {

  @Autowired private RecipeSuggestionPort port;

  @Test
  void ollamaProvider_wiresTheOllamaAdapter() {
    assertThat(port).isInstanceOf(OllamaRecipeSuggestionAdapter.class);
  }
}
