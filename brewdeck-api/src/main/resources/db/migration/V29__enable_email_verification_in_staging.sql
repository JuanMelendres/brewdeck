-- Enforce email verification in staging (backlog AUTH-02, ADR-012 enabling checklist). SMTP works on
-- staging since #248. The owner's account must be verified on staging before this ships (otherwise
-- see the lock-out recovery in ADR-012). local, dev, test, and prod stay disabled.
--
-- Owner: Juan (product owner). Expires: 2026-12-31 (unchanged from V20). Removal condition:
-- unchanged (enabled in prod and stable for 2 weeks).
UPDATE feature_flags
SET enabled = TRUE
WHERE feature_key = 'auth-require-email-verification'
  AND environment = 'staging';
