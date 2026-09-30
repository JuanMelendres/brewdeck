-- RELEASE flag: when enabled, accounts with an unverified email are blocked (403
-- EMAIL_NOT_VERIFIED) from everything except verifying themselves (ADR-012).
--
-- Disabled EVERYWHERE, including local: turning it on before real email delivery works would lock
-- users (including the owner) out, because they could never receive the verification link.
-- Enable it per environment once SMTP is configured and a verification email is confirmed to
-- arrive.
--
-- Owner: Juan (product owner). Expires: 2026-12-31. Removal condition: enabled in prod and stable
-- for 2 weeks; then make the check unconditional, drop this flag, its rows, and the frontend alias.
INSERT INTO feature_flags
    (feature_key, display_name, description, environment, enabled, flag_type, owner, expires_at, removal_condition)
SELECT 'auth-require-email-verification',
       'Require email verification',
       'Blocks accounts with an unverified email from all endpoints except verification.',
       env, FALSE, 'RELEASE', 'Juan (product owner)', TIMESTAMP '2026-12-31 00:00:00',
       'Enabled in prod and stable for 2 weeks; then make the check unconditional and remove the flag.'
  FROM (VALUES ('local'), ('dev'), ('test'), ('staging'), ('prod')) AS environments(env);
