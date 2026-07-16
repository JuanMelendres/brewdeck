package com.brewdeck.brewdeck_api.featureflag;

import java.nio.charset.StandardCharsets;
import java.util.zip.CRC32;
import org.springframework.stereotype.Component;

/**
 * Deterministic percentage-based rollout. The same subject always lands in the same bucket for the
 * same flag, so a user's experience is stable across requests (no per-request randomness). The
 * bucket is {@code hash(featureKey:subject) mod 100}; the subject is inside the rollout when its
 * bucket is strictly below the rollout percentage.
 */
@Component
public class RolloutEvaluator {

  /**
   * @param featureKey the flag key (salts the hash so buckets differ per flag)
   * @param rolloutPercentage 0..100
   * @param subject stable subject id (user/anon), or {@code null} when none is available
   * @return whether the subject is included in the rollout
   */
  public boolean isIncluded(String featureKey, int rolloutPercentage, String subject) {
    if (rolloutPercentage >= 100) {
      return true;
    }
    if (rolloutPercentage <= 0) {
      return false;
    }
    // No stable subject and a partial rollout: exclude to fail safe rather than flip per request.
    if (subject == null) {
      return false;
    }
    return bucket(featureKey, subject) < rolloutPercentage;
  }

  private int bucket(String featureKey, String subject) {
    CRC32 crc = new CRC32();
    crc.update((featureKey + ':' + subject).getBytes(StandardCharsets.UTF_8));
    return (int) (crc.getValue() % 100);
  }
}
