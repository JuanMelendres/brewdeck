package com.brewdeck.brewdeck_api.featureflag;

import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Internal management operations for feature flags: mutate a flag row and evict its cache entry so
 * the change takes effect immediately. Intentionally <b>not</b> exposed over HTTP — BrewDeck has no
 * role/authorization model yet, and an unauthenticated flag-toggling endpoint would be a security
 * hole. Wire an admin controller on top of this only once RBAC exists (see follow-up task).
 *
 * <p>Every mutation is audit-logged with key, environment, previous/new value and acting user.
 * Sensitive JSON configuration is never logged.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class FeatureFlagAdminService {

  private static final String SYSTEM_ACTOR = "system";

  private final FeatureFlagRepository repository;
  private final FeatureFlagCache cache;

  @Transactional(readOnly = true)
  public List<FeatureFlag> listAll() {
    return repository.findAll();
  }

  /** Flags whose expiry has already passed — surface these so they don't rot as tech debt. */
  @Transactional(readOnly = true)
  public List<FeatureFlag> findExpired() {
    return repository.findByExpiresAtBefore(LocalDateTime.now());
  }

  @Transactional
  public FeatureFlag setEnabled(
      String featureKey, String environment, boolean enabled, String actingUser) {
    FeatureFlag flag = require(featureKey, environment);
    boolean previous = flag.isEnabled();
    flag.setEnabled(enabled);
    FeatureFlag saved = repository.save(flag);
    cache.evict(featureKey, environment);
    logChange("enabled", featureKey, environment, previous, enabled, actingUser);
    return saved;
  }

  @Transactional
  public FeatureFlag setRolloutPercentage(
      String featureKey, String environment, int rolloutPercentage, String actingUser) {
    if (rolloutPercentage < 0 || rolloutPercentage > 100) {
      throw new IllegalArgumentException("rolloutPercentage must be between 0 and 100");
    }
    FeatureFlag flag = require(featureKey, environment);
    int previous = flag.getRolloutPercentage();
    flag.setRolloutPercentage(rolloutPercentage);
    FeatureFlag saved = repository.save(flag);
    cache.evict(featureKey, environment);
    logChange(
        "rolloutPercentage", featureKey, environment, previous, rolloutPercentage, actingUser);
    return saved;
  }

  @Transactional
  public FeatureFlag setExpiration(
      String featureKey, String environment, LocalDateTime expiresAt, String actingUser) {
    FeatureFlag flag = require(featureKey, environment);
    LocalDateTime previous = flag.getExpiresAt();
    flag.setExpiresAt(expiresAt);
    FeatureFlag saved = repository.save(flag);
    cache.evict(featureKey, environment);
    logChange("expiresAt", featureKey, environment, previous, expiresAt, actingUser);
    return saved;
  }

  /** Force-refresh: drop every cached snapshot (e.g. after a bulk DB change). */
  public void invalidateCache() {
    cache.evictAll();
    log.info("Feature flag cache fully invalidated actor={}", SYSTEM_ACTOR);
  }

  private FeatureFlag require(String featureKey, String environment) {
    return repository
        .findByFeatureKeyAndEnvironment(featureKey, environment)
        .orElseThrow(
            () ->
                new EntityNotFoundException(
                    "Feature flag not found: " + featureKey + " [" + environment + "]"));
  }

  private void logChange(
      String field,
      String featureKey,
      String environment,
      Object previous,
      Object next,
      String actingUser) {
    log.info(
        "Feature flag change field={} key={} environment={} previous={} new={} actor={} at={}",
        field,
        featureKey,
        environment,
        previous,
        next,
        actingUser == null ? SYSTEM_ACTOR : actingUser,
        LocalDateTime.now());
  }
}
