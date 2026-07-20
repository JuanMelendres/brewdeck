package com.brewdeck.brewdeck_api.featureflag;

/**
 * Explicit allow-list of flags exposed to the frontend, mapping each backend {@code feature_key} to
 * its camelCase client alias. Only capabilities the UI needs to hide/show belong here — internal
 * operational toggles and kill switches must NOT be listed, so the public {@code
 * /api/feature-flags} response never leaks operational internals or administrative metadata.
 */
public enum FrontendFeatureFlag {
  AI_RECIPE_ASSISTANT(FeatureKeys.AI_RECIPE_ASSISTANT, "aiRecipeAssistant");

  private final String backendKey;
  private final String frontendAlias;

  FrontendFeatureFlag(String backendKey, String frontendAlias) {
    this.backendKey = backendKey;
    this.frontendAlias = frontendAlias;
  }

  public String backendKey() {
    return backendKey;
  }

  public String frontendAlias() {
    return frontendAlias;
  }
}
