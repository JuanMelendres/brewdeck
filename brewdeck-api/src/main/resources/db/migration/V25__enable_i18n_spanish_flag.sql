-- Turn on the Spanish UI in staging and prod (ADR-015). The owner reviewed every Spanish string
-- (2026-10-06, PR #210). The test environment stays off: its integration tests set the flag
-- explicitly and expect "off" as the seeded default.
--
-- Owner: Juan (product owner). Expires: 2026-12-01 (unchanged from V24). Removal condition:
-- unchanged from V24 (stable in prod; then remove the flag, its rows, and the gating code).
UPDATE feature_flags
SET enabled = TRUE
WHERE feature_key = 'web-i18n-spanish'
  AND environment IN ('staging', 'prod');
