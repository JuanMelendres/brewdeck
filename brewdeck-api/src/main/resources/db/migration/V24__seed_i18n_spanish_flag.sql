-- RELEASE flag: when enabled, users can choose Spanish (PUT /api/auth/me/language) and the web
-- app serves it (ADR-015). It stays off in prod until every screen is translated and reviewed.
--
-- Owner: Juan (product owner). Expires: 2026-12-01. Removal condition: Spanish complete,
-- owner-reviewed, and enabled in prod; then drop the check, this flag, its rows, and the frontend
-- alias.
INSERT INTO feature_flags
    (feature_key, display_name, description, environment, enabled, flag_type, owner, expires_at, removal_condition)
SELECT 'web-i18n-spanish',
       'Spanish UI',
       'Lets users choose Spanish and serves the Spanish web UI.',
       env, env IN ('local', 'dev'), 'RELEASE', 'Juan (product owner)', TIMESTAMP '2026-12-01 00:00:00',
       'Spanish complete, owner-reviewed, and enabled in prod; then remove the flag.'
  FROM (VALUES ('local'), ('dev'), ('test'), ('staging'), ('prod')) AS environments(env);
