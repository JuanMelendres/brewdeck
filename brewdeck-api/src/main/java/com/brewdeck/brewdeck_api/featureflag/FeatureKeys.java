package com.brewdeck.brewdeck_api.featureflag;

/**
 * Centralized, typed feature keys. Never scatter raw flag-key string literals through the codebase
 * — reference a constant here so renames and removals are mechanical and greppable. Keys are
 * stable, descriptive, kebab-case, and match the {@code feature_key} column seeded via Flyway.
 */
public final class FeatureKeys {

  /** AI recipe suggestion + improvement (POST /api/recipes/suggest, /api/recipes/{id}/improve). */
  public static final String AI_RECIPE_ASSISTANT = "brew-recipe-ai-assistant";

  /**
   * Blocks accounts with an unverified email from everything except verifying (ADR-012). RELEASE
   * flag, off by default until real email delivery is confirmed.
   */
  public static final String REQUIRE_EMAIL_VERIFICATION = "auth-require-email-verification";

  /**
   * Lets users choose Spanish and serves the Spanish web UI (ADR-015). RELEASE flag, on in local
   * and dev only until every screen is translated and reviewed.
   */
  public static final String I18N_SPANISH = "web-i18n-spanish";

  private FeatureKeys() {}
}
