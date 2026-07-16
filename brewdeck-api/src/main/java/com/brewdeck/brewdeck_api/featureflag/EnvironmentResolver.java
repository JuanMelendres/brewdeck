package com.brewdeck.brewdeck_api.featureflag;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Resolves the environment name feature flags are scoped to. Derived from the active Spring profile
 * (local/dev/test/staging/prod), with an optional explicit override for deployments whose profile
 * name differs. Falls back to {@code prod} — the most restrictive environment — when nothing
 * matches, so an unrecognized deployment defaults incomplete features to disabled rather than
 * exposing them.
 */
@Component
public class EnvironmentResolver {

  static final String DEFAULT_ENVIRONMENT = "prod";

  private static final Set<String> KNOWN_ENVIRONMENTS =
      Set.of("local", "dev", "test", "staging", "prod");

  private final Environment springEnvironment;
  private final String override;

  public EnvironmentResolver(
      Environment springEnvironment,
      @Value("${brewdeck.feature-flags.environment:}") String override) {
    this.springEnvironment = springEnvironment;
    this.override = override;
  }

  /** The resolved environment name, lowercased. */
  public String resolve() {
    if (override != null && !override.isBlank()) {
      return normalize(override);
    }
    List<String> active = Arrays.asList(springEnvironment.getActiveProfiles());
    return active.stream()
        .map(this::normalize)
        .filter(KNOWN_ENVIRONMENTS::contains)
        .findFirst()
        .orElse(DEFAULT_ENVIRONMENT);
  }

  private String normalize(String value) {
    return value.trim().toLowerCase();
  }
}
