-- Slice C.1: account profile. Optional human-friendly display name.
-- Nullable: existing accounts keep email-only identity until they set one.
ALTER TABLE users ADD COLUMN display_name VARCHAR(100);
