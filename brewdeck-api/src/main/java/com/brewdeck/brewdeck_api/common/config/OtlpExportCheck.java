package com.brewdeck.brewdeck_api.common.config;

import org.springframework.core.env.Environment;

/**
 * Fails startup when OTLP metrics export is on but the endpoint or the credentials are missing
 * (OBS-01 PR 3, ADR-023). Without this, every push would fail later on a background thread, and
 * staging would look healthy while sending nothing.
 */
final class OtlpExportCheck {

  static final String URL = "management.otlp.metrics.export.url";
  static final String AUTHORIZATION = "management.otlp.metrics.export.headers.Authorization";

  OtlpExportCheck(Environment environment) {
    require(environment, URL, "OTLP_METRICS_URL");
    require(environment, AUTHORIZATION, "OTLP_METRICS_AUTHORIZATION");
  }

  private static void require(Environment environment, String property, String variable) {
    String value = environment.getProperty(property);
    if (value == null || value.isBlank()) {
      throw new IllegalStateException(
          "OTLP_METRICS_ENABLED is true but " + variable + " (" + property + ") is not set");
    }
  }
}
