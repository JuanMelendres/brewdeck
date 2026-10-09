package com.brewdeck.brewdeck_api.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * AI recipe assistant settings. {@code enabled=false} wires the disabled adapter; otherwise {@code
 * provider} picks the adapter: {@code claude} (paid API, ADR-006) or {@code ollama} (a local
 * open-weight model, ADR-016). {@code model}, {@code timeoutSeconds} and {@code maxTokens} apply to
 * Claude; {@code ollama} has its own block.
 */
@ConfigurationProperties(prefix = "brewdeck.ai")
public record AiProperties(
    boolean enabled,
    @DefaultValue("claude") String provider,
    String model,
    int timeoutSeconds,
    int maxTokens,
    @DefaultValue Ollama ollama) {

  /** Ollama server and model. Local models are slow (15-25 s on an 8B model) and can run away. */
  public record Ollama(
      @DefaultValue("http://localhost:11434") String baseUrl,
      @DefaultValue("qwen3:8b") String model,
      @DefaultValue("90") int timeoutSeconds,
      @DefaultValue("1024") int maxTokens) {}

  /** The model actually answering, for logs and provenance. */
  public String activeModel() {
    return "ollama".equals(provider) ? ollama.model() : model;
  }
}
