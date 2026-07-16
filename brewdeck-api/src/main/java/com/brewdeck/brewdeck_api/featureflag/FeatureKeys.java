package com.brewdeck.brewdeck_api.featureflag;

/**
 * Centralized, typed feature keys. Never scatter raw flag-key string literals through the codebase
 * — reference a constant here so renames and removals are mechanical and greppable. Keys are
 * stable, descriptive, kebab-case, and match the {@code feature_key} column seeded via Flyway.
 */
public final class FeatureKeys {

  /** AI recipe suggestion + improvement (POST /api/recipes/suggest, /api/recipes/{id}/improve). */
  public static final String AI_RECIPE_ASSISTANT = "brew-recipe-ai-assistant";

  private FeatureKeys() {}
}
