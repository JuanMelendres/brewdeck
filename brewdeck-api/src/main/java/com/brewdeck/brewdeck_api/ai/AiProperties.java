package com.brewdeck.brewdeck_api.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "brewdeck.ai")
public record AiProperties(boolean enabled, String model, int timeoutSeconds, int maxTokens) {}
