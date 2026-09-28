package com.brewdeck.brewdeck_api.common.config;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * The single CORS policy for the API. Spring Security applies it (see {@code SecurityConfig}) in
 * its filter chain, which every request passes through, so a separate Spring MVC {@code
 * addCorsMappings} would only duplicate it and could drift out of sync.
 */
@Configuration
public class WebConfig {

  private final List<String> allowedOrigins;

  public WebConfig(@Value("${app.cors.allowed-origins}") List<String> allowedOrigins) {
    this.allowedOrigins = allowedOrigins;
  }

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowedOrigins(allowedOrigins);
    config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    // Exactly what the web client sends; anything else is refused at preflight.
    config.setAllowedHeaders(List.of(HttpHeaders.AUTHORIZATION, HttpHeaders.CONTENT_TYPE));
    // Let browser code read the rate-limit wait and the URL of a created resource.
    config.setExposedHeaders(List.of(HttpHeaders.RETRY_AFTER, HttpHeaders.LOCATION));
    // Auth is a bearer token in a header, never a cookie, so browsers need not send credentials.
    config.setAllowCredentials(false);
    config.setMaxAge(3600L);
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/api/**", config);
    return source;
  }
}
