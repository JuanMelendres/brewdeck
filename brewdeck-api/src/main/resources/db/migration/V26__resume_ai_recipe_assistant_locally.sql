-- Resume the AI recipe assistant in local development only, now on a free local model served by
-- Ollama (ADR-016, docs/product/spikes/ai-local-llm-spike.md). The `local` profile wires
-- brewdeck.ai.provider=ollama; dev, test, staging, and prod stay disabled until the owner decides.
--
-- Owner: Backend Team. Expires: 2026-12-01 (unchanged from V13). Removal condition: unchanged
-- (AI output validated across brew methods and the frontend UX polished).
UPDATE feature_flags
SET enabled = TRUE
WHERE feature_key = 'brew-recipe-ai-assistant'
  AND environment = 'local';
