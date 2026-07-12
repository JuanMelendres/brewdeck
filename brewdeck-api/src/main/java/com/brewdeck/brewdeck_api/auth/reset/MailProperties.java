package com.brewdeck.brewdeck_api.auth.reset;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "brewdeck.mail")
public record MailProperties(boolean enabled, String frontendBaseUrl) {}
