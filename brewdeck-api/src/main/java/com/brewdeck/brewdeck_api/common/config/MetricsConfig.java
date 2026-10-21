package com.brewdeck.brewdeck_api.common.config;

import com.brewdeck.brewdeck_api.featureflag.EnvironmentResolver;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.micrometer.metrics.autoconfigure.MeterRegistryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Common tags on every metric (OBS-01, ADR-023); {@code application} comes from the YAML. */
@Configuration
public class MetricsConfig {

  /** The same environment name feature flags use: local, test, staging or prod. */
  @Bean
  MeterRegistryCustomizer<MeterRegistry> environmentTag(EnvironmentResolver environmentResolver) {
    return registry -> registry.config().commonTags("environment", environmentResolver.resolve());
  }
}
