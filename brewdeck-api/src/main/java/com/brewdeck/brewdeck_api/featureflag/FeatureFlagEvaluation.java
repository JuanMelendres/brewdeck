package com.brewdeck.brewdeck_api.featureflag;

/** Immutable result of evaluating one flag in one environment. */
public record FeatureFlagEvaluation(
    String featureKey, String environment, boolean enabled, EvaluationReason reason) {

  static FeatureFlagEvaluation notFound(String featureKey, String environment) {
    return new FeatureFlagEvaluation(
        featureKey, environment, false, EvaluationReason.FLAG_NOT_FOUND);
  }

  static FeatureFlagEvaluation datastoreUnavailable(String featureKey, String environment) {
    return new FeatureFlagEvaluation(
        featureKey, environment, false, EvaluationReason.DATASTORE_UNAVAILABLE);
  }
}
