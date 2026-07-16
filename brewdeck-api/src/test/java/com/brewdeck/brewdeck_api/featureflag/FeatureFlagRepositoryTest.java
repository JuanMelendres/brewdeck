package com.brewdeck.brewdeck_api.featureflag;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.brewdeck.brewdeck_api.common.PostgresIntegrationTest;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
class FeatureFlagRepositoryTest extends PostgresIntegrationTest {

  @Autowired private FeatureFlagRepository repository;

  private FeatureFlag flag(String key, String environment, boolean enabled) {
    return FeatureFlag.builder()
        .featureKey(key)
        .displayName("Test flag")
        .environment(environment)
        .enabled(enabled)
        .flagType(FlagType.RELEASE)
        .rolloutPercentage(100)
        .build();
  }

  @Test
  void findByFeatureKeyAndEnvironment_returnsMatchOrEmpty() {
    String key = "repo-flag-" + System.nanoTime();
    repository.save(flag(key, "repo-env", true));

    assertThat(repository.findByFeatureKeyAndEnvironment(key, "repo-env")).isPresent();
    assertThat(repository.findByFeatureKeyAndEnvironment(key, "other-env")).isEmpty();
    assertThat(repository.findByFeatureKeyAndEnvironment("missing", "repo-env")).isEmpty();
  }

  @Test
  void sameKeyAllowedInDifferentEnvironments_butDuplicatePairRejected() {
    String key = "repo-dup-" + System.nanoTime();
    repository.saveAndFlush(flag(key, "env-a", true));
    // Same key, different environment: allowed.
    repository.saveAndFlush(flag(key, "env-b", false));

    // Same key + environment again: violates uk_feature_flags_key_environment.
    assertThatThrownBy(() -> repository.saveAndFlush(flag(key, "env-a", false)))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void findByExpiresAtBefore_returnsOnlyPastExpiries() {
    String key = "repo-exp-" + System.nanoTime();
    FeatureFlag expired = flag(key, "exp-env", true);
    expired.setExpiresAt(LocalDateTime.now().minusDays(1));
    repository.save(expired);

    assertThat(repository.findByExpiresAtBefore(LocalDateTime.now()))
        .anyMatch(f -> f.getFeatureKey().equals(key));
  }
}
