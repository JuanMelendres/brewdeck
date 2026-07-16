package com.brewdeck.brewdeck_api.featureflag;

import java.util.Map;

/**
 * Frontend-facing flag payload: {@code { "features": { "aiRecipeAssistant": false } }}. Carries
 * only boolean availability keyed by client alias — no owner, environment, rollout, configuration,
 * or any other administrative metadata.
 */
public record FeatureFlagsResponse(Map<String, Boolean> features) {}
