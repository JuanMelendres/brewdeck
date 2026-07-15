-- Slice B.1: introduce per-user ownership (write path).
-- Nullable owner_id FK now; NOT NULL enforcement deferred to Slice B.2 once
-- every row is guaranteed to have an owner and read-filtering is in place.

ALTER TABLE coffees ADD COLUMN owner_id BIGINT REFERENCES users (id);
ALTER TABLE recipes ADD COLUMN owner_id BIGINT REFERENCES users (id);
ALTER TABLE brew_sessions ADD COLUMN owner_id BIGINT REFERENCES users (id);

-- Backfill pre-existing rows to the earliest registered user. No-op (leaves
-- NULL) when no users exist yet, e.g. a fresh database.
UPDATE coffees SET owner_id = (SELECT MIN(id) FROM users) WHERE owner_id IS NULL;
UPDATE recipes SET owner_id = (SELECT MIN(id) FROM users) WHERE owner_id IS NULL;
UPDATE brew_sessions SET owner_id = (SELECT MIN(id) FROM users) WHERE owner_id IS NULL;

CREATE INDEX idx_coffees_owner_id ON coffees (owner_id);
CREATE INDEX idx_recipes_owner_id ON recipes (owner_id);
CREATE INDEX idx_brew_sessions_owner_id ON brew_sessions (owner_id);
