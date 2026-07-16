package com.brewdeck.brewdeck_api.featureflag;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/**
 * PostgreSQL-backed {@link FeatureFlagService}. Resolves the active environment, reads the cached
 * flag snapshot, and applies deterministic rollout. Every failure mode (missing flag, disabled
 * flag, datastore error, rollout exclusion) resolves to disabled.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DatabaseFeatureFlagService implements FeatureFlagService {

  private final FeatureFlagCache cache;
  private final EnvironmentResolver environmentResolver;
  private final RolloutEvaluator rolloutEvaluator;

  @Override
  public boolean isEnabled(String featureKey) {
    return isEnabled(featureKey, FeatureFlagContext.empty());
  }

  @Override
  public boolean isEnabled(String featureKey, FeatureFlagContext context) {
    return evaluate(featureKey, context).enabled();
  }

  @Override
  public FeatureFlagEvaluation evaluate(String featureKey, FeatureFlagContext context) {
    String environment = environmentResolver.resolve();
    FlagSnapshot snapshot = cache.get(featureKey, environment);
    return evaluateSnapshot(featureKey, environment, snapshot, context);
  }

  @Override
  public void requireEnabled(String featureKey, FeatureFlagContext context) {
    String environment = environmentResolver.resolve();
    FlagSnapshot snapshot = cache.get(featureKey, environment);
    FeatureFlagEvaluation evaluation = evaluateSnapshot(featureKey, environment, snapshot, context);
    if (evaluation.enabled()) {
      return;
    }
    HttpStatus status =
        snapshot.present()
            ? FeatureDisabledException.statusFor(snapshot.flagType())
            : HttpStatus.NOT_FOUND;
    log.debug(
        "Blocked disabled feature key={} environment={} reason={}",
        featureKey,
        environment,
        evaluation.reason());
    throw new FeatureDisabledException(featureKey, status);
  }

  private FeatureFlagEvaluation evaluateSnapshot(
      String featureKey, String environment, FlagSnapshot snapshot, FeatureFlagContext context) {
    if (snapshot.datastoreError()) {
      return FeatureFlagEvaluation.datastoreUnavailable(featureKey, environment);
    }
    if (!snapshot.present()) {
      // A missing flag is never enabled. Warn in non-prod so typos surface during development.
      if (EnvironmentResolver.DEFAULT_ENVIRONMENT.equals(environment)) {
        log.debug("Unknown feature flag key={} environment={}", featureKey, environment);
      } else {
        log.warn("Unknown feature flag key={} environment={}", featureKey, environment);
      }
      return FeatureFlagEvaluation.notFound(featureKey, environment);
    }
    if (!snapshot.enabled()) {
      return new FeatureFlagEvaluation(featureKey, environment, false, EvaluationReason.DISABLED);
    }
    boolean included =
        rolloutEvaluator.isIncluded(
            featureKey, snapshot.rolloutPercentage(), context.rolloutSubject());
    EvaluationReason reason =
        included ? EvaluationReason.ENABLED : EvaluationReason.ROLLOUT_EXCLUDED;
    return new FeatureFlagEvaluation(featureKey, environment, included, reason);
  }
}
