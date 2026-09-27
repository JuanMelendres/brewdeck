-- PostgreSQL does not index foreign keys automatically. Without these, deleting a coffee, brew
-- method, or recipe scans the whole child table to check references, and the per-coffee /
-- per-method / per-recipe lookups scan every row.
--
-- Plain CREATE INDEX (not CONCURRENTLY): Flyway runs each migration in a transaction, and at the
-- current data size the brief write lock is negligible.
CREATE INDEX IF NOT EXISTS idx_recipes_coffee_id ON recipes (coffee_id);
CREATE INDEX IF NOT EXISTS idx_recipes_method_id ON recipes (method_id);

-- Leading recipe_id serves the FK check and "sessions of a recipe"; brewed_at DESC serves their
-- newest-first ordering (history, AI improve's latest rated sessions).
CREATE INDEX IF NOT EXISTS idx_brew_sessions_recipe_id_brewed_at
    ON brew_sessions (recipe_id, brewed_at DESC);
