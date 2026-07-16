package com.brewdeck.brewdeck_api.featureflag;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeatureFlagRepository extends JpaRepository<FeatureFlag, Long> {

  Optional<FeatureFlag> findByFeatureKeyAndEnvironment(String featureKey, String environment);

  List<FeatureFlag> findByEnvironment(String environment);

  /** Flags whose expiry has passed as of {@code now} — used for stale-flag reporting. */
  List<FeatureFlag> findByExpiresAtBefore(LocalDateTime now);
}
