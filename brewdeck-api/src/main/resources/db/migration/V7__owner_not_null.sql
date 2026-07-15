-- Slice B.2: owner is now a hard invariant. Every row was backfilled in V6 and
-- every create stamps an owner, so tightening to NOT NULL is safe.
ALTER TABLE coffees       ALTER COLUMN owner_id SET NOT NULL;
ALTER TABLE recipes       ALTER COLUMN owner_id SET NOT NULL;
ALTER TABLE brew_sessions ALTER COLUMN owner_id SET NOT NULL;
