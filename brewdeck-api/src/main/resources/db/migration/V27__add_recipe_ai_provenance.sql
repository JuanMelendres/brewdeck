-- Which AI model and prompt version a recipe came from, when it was saved from an AI suggestion or
-- improvement (ADR-016). NULL for recipes written by hand. Informational only: the web app sends
-- these values with the recipe it saves.
ALTER TABLE recipes ADD COLUMN ai_model VARCHAR(80);
ALTER TABLE recipes ADD COLUMN ai_prompt_version VARCHAR(20);
