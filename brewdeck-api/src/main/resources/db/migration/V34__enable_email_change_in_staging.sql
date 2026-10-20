-- Turn on the email change flow (backlog AUTH-04, ADR-019) in local and staging now that the API
-- (#294) and the web (#295) are both in. dev, test and prod stay disabled; prod is enabled with the
-- go-live release once the staging manual checks pass.
--
-- Owner: Juan (product owner). Expires: 2027-01-31 (unchanged from V33). Removal condition:
-- unchanged (enabled in staging and prod with no issues for 2 weeks).
UPDATE feature_flags
SET enabled = TRUE
WHERE feature_key = 'auth-email-change'
  AND environment IN ('local', 'staging');
