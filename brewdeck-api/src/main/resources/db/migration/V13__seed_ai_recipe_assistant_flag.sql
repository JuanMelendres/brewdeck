-- RELEASE flag for the AI recipe assistant (POST /api/recipes/suggest and
-- POST /api/recipes/{id}/improve). The feature calls an external LLM whose output still
-- needs validation, so production and staging default to disabled. It is enabled in the
-- developer-facing environments (local, dev) so the flow can be exercised there.
--
-- Owner: Backend Team. Expires: 2026-12-01. Removal condition: AI suggestion/improvement
-- output validated across brew methods and the frontend UX is polished; then drop the flag
-- and its conditional checks.
INSERT INTO feature_flags
    (feature_key, display_name, description, environment, enabled, flag_type, owner, expires_at, removal_condition)
VALUES
    ('brew-recipe-ai-assistant', 'AI Recipe Assistant',
     'AI-generated brewing parameters (suggest and improve).', 'local',   TRUE,  'RELEASE',
     'Backend Team', TIMESTAMP '2026-12-01 00:00:00',
     'AI output validated across brew methods and frontend UX polished.'),
    ('brew-recipe-ai-assistant', 'AI Recipe Assistant',
     'AI-generated brewing parameters (suggest and improve).', 'dev',     TRUE,  'RELEASE',
     'Backend Team', TIMESTAMP '2026-12-01 00:00:00',
     'AI output validated across brew methods and frontend UX polished.'),
    ('brew-recipe-ai-assistant', 'AI Recipe Assistant',
     'AI-generated brewing parameters (suggest and improve).', 'test',    FALSE, 'RELEASE',
     'Backend Team', TIMESTAMP '2026-12-01 00:00:00',
     'AI output validated across brew methods and frontend UX polished.'),
    ('brew-recipe-ai-assistant', 'AI Recipe Assistant',
     'AI-generated brewing parameters (suggest and improve).', 'staging', FALSE, 'RELEASE',
     'Backend Team', TIMESTAMP '2026-12-01 00:00:00',
     'AI output validated across brew methods and frontend UX polished.'),
    ('brew-recipe-ai-assistant', 'AI Recipe Assistant',
     'AI-generated brewing parameters (suggest and improve).', 'prod',    FALSE, 'RELEASE',
     'Backend Team', TIMESTAMP '2026-12-01 00:00:00',
     'AI output validated across brew methods and frontend UX polished.');
