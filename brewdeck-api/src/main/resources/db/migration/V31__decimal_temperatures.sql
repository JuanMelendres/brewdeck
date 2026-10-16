-- Water temperatures keep one decimal (e.g. 92.5 degrees Celsius) instead of being truncated to an
-- integer by the API (backlog DATA-01, docs/architecture/numeric-fields.md, owner decision
-- 2026-10-16). NUMERIC(4,1) holds 0.0 to 100.0 with the same range validation as before. Every
-- existing INTEGER converts exactly (92 -> 92.0), so no USING clause is needed.
ALTER TABLE recipes ALTER COLUMN water_temp TYPE NUMERIC(4,1);
ALTER TABLE brew_sessions ALTER COLUMN actual_temp TYPE NUMERIC(4,1);
