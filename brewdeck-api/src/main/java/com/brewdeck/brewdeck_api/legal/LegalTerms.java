package com.brewdeck.brewdeck_api.legal;

/**
 * The published version of the privacy notice and terms (LEGAL-01). It must match the version date
 * at the top of the texts in {@code brewdeck-web/content/legal/}. Bump both together; users then
 * accept the new version (docs/legal/README.md, "Versioning").
 */
public final class LegalTerms {

  public static final String CURRENT_VERSION = "2026-10-20";

  private LegalTerms() {}
}
