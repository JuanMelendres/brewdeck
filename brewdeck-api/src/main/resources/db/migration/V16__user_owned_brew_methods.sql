-- Brew methods become two-tier: a shared catalog (owner_id IS NULL, admin-managed) plus private
-- methods each user creates for themselves (owner_id set). Every pre-existing row, including the
-- V2 seed, stays in the shared catalog.
ALTER TABLE brew_methods ADD COLUMN owner_id BIGINT REFERENCES users (id);

CREATE INDEX idx_brew_methods_owner_id ON brew_methods (owner_id);

-- Names were globally unique. Now they are unique within the shared catalog and within each
-- user's private methods, so two users may each have their own "My V60".
ALTER TABLE brew_methods DROP CONSTRAINT brew_methods_name_key;

CREATE UNIQUE INDEX ux_brew_methods_shared_name
    ON brew_methods (name)
    WHERE owner_id IS NULL;

CREATE UNIQUE INDEX ux_brew_methods_owner_name
    ON brew_methods (owner_id, name)
    WHERE owner_id IS NOT NULL;
