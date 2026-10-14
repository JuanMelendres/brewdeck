-- The Spanish UI is complete, owner-reviewed, and enabled in prod (V25), so the RELEASE flag
-- web-i18n-spanish is retired (docs/development/feature-flags.md, "How to remove a flag").
-- The code no longer reads it; English and Spanish are always available (ADR-015).
DELETE FROM feature_flags
WHERE feature_key = 'web-i18n-spanish';
