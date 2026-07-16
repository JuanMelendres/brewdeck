package com.brewdeck.brewdeck_api.featureflag;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;

/**
 * Short-TTL cache in front of the flag table so the hot {@code isEnabled} path does not hit
 * PostgreSQL on every request. Lives in its own bean so {@link Cacheable}/{@link CacheEvict} go
 * through the Spring proxy (self-invocation would bypass the cache advice).
 *
 * <p>Fails safe: a datastore error yields a {@link FlagSnapshot#datastoreError()} snapshot
 * (disabled) rather than propagating, so a transient DB outage never enables an unfinished feature.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class FeatureFlagCache {

  public static final String CACHE_NAME = "featureFlags";

  private final FeatureFlagRepository repository;

  /** Cache key is {@code featureKey + ':' + environment}, matching the unique DB constraint. */
  @Cacheable(cacheNames = CACHE_NAME, key = "#featureKey + ':' + #environment")
  public FlagSnapshot get(String featureKey, String environment) {
    try {
      return repository
          .findByFeatureKeyAndEnvironment(featureKey, environment)
          .map(FlagSnapshot::of)
          .orElseGet(FlagSnapshot::absent);
    } catch (DataAccessException ex) {
      // No sensitive data in the log: only the key and environment.
      log.error(
          "Feature flag datastore unavailable for key={} environment={}; defaulting to disabled",
          featureKey,
          environment,
          ex);
      return FlagSnapshot.datastoreUnavailable();
    }
  }

  @CacheEvict(cacheNames = CACHE_NAME, key = "#featureKey + ':' + #environment")
  public void evict(String featureKey, String environment) {
    // Advice-only: eviction happens via @CacheEvict.
  }

  @CacheEvict(cacheNames = CACHE_NAME, allEntries = true)
  public void evictAll() {
    // Advice-only: clears every cached flag snapshot.
  }
}
