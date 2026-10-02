-- Suspend the AI recipe assistant in local/dev too. Owner has decided not to pay for an
-- LLM API subscription for now, so the feature stays fully built (code, port, adapter,
-- frontend buttons) but disabled everywhere until integration resumes.
--
-- Owner: Backend Team. Expires: 2026-12-01 (unchanged from V13). Removal condition:
-- unchanged from V13 (AI output validated + frontend UX polished) — this migration only
-- flips enabled=false, it does not change ownership/expiration/removal-condition metadata.
UPDATE feature_flags
SET enabled = FALSE
WHERE feature_key = 'brew-recipe-ai-assistant'
  AND environment IN ('local', 'dev');
