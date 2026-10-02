package com.brewdeck.brewdeck_api.auth.reset;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * {@code brewdeck.mail.*}: whether real SMTP delivery is on, the frontend base URL used to build
 * links, and the sender address. SMTP connection settings are Spring Boot's own {@code
 * spring.mail.*}.
 */
@ConfigurationProperties(prefix = "brewdeck.mail")
public record MailProperties(boolean enabled, String frontendBaseUrl, String from) {}
