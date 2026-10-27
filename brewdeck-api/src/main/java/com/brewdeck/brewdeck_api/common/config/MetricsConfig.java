package com.brewdeck.brewdeck_api.common.config;

import com.brewdeck.brewdeck_api.featureflag.EnvironmentResolver;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.micrometer.metrics.autoconfigure.MeterRegistryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * Common tags on every metric (OBS-01, ADR-023); {@code application} comes from the YAML. Also
 * checks the OTLP push settings when export is on.
 */
@Configuration
public class MetricsConfig {

  /** The same environment name feature flags use: local, test, staging or prod. */
  @Bean
  MeterRegistryCustomizer<MeterRegistry> environmentTag(EnvironmentResolver environmentResolver) {
    return registry -> registry.config().commonTags("environment", environmentResolver.resolve());
  }

  @Bean
  @ConditionalOnProperty(name = "management.otlp.metrics.export.enabled", havingValue = "true")
  OtlpExportCheck otlpExportCheck(Environment environment) {
    return new OtlpExportCheck(environment);
  }
}
