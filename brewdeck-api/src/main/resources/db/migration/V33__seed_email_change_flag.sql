-- RELEASE flag for the email change flow (backlog AUTH-04, ADR-019). Guards requesting and
-- confirming a change; cancelling is never guarded, so a pending request can always be stopped.
--
-- Disabled EVERYWHERE until the web flow ships (AUTH-04 PR 2); PR 3 enables it in staging.
--
-- Owner: Juan (product owner). Expires: 2027-01-31. Removal condition: enabled in staging and prod
-- with no issues for 2 weeks; then drop the requireEnabled checks, this flag's rows, and the
-- frontend alias.
INSERT INTO feature_flags
    (feature_key, display_name, description, environment, enabled, flag_type, owner, expires_at, removal_condition)
SELECT 'auth-email-change',
       'Email change',
       'Lets users change their account email with confirmation from the new address.',
       env, FALSE, 'RELEASE', 'Juan (product owner)', TIMESTAMP '2027-01-31 00:00:00',
       'Enabled in staging and prod with no issues for 2 weeks; then remove the flag.'
  FROM (VALUES ('local'), ('dev'), ('test'), ('staging'), ('prod')) AS environments(env);
