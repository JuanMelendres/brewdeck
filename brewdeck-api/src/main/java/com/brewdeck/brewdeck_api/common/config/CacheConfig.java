package com.brewdeck.brewdeck_api.common.config;

import com.brewdeck.brewdeck_api.featureflag.FeatureFlagCache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Local in-process caching via Caffeine. Introduced for feature flags so the hot {@code isEnabled}
 * path avoids a PostgreSQL round-trip per request. A short 45s TTL keeps flag changes propagating
 * quickly while still absorbing bursts; admin updates evict entries immediately regardless of TTL.
 *
 * <p>No Redis: this API is a single modular monolith and does not otherwise need a distributed
 * cache. Each instance holds its own copy; the short TTL bounds cross-instance staleness.
 */
@Configuration
@EnableCaching
public class CacheConfig {

  @Bean
  public CacheManager cacheManager() {
    CaffeineCacheManager manager = new CaffeineCacheManager(FeatureFlagCache.CACHE_NAME);
    manager.setCaffeine(
        Caffeine.newBuilder().expireAfterWrite(Duration.ofSeconds(45)).maximumSize(1_000));
    return manager;
  }
}
