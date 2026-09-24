-- The request validation (and the frontend) accept longer values than these V1 columns hold, so
-- valid input failed at the database with a 409 "Data integrity violation". Widen the columns to
-- the validated limits. Increasing a VARCHAR length is a metadata-only change in PostgreSQL (no
-- table rewrite).
ALTER TABLE coffees       ALTER COLUMN roast_level   TYPE VARCHAR(80);
ALTER TABLE recipes       ALTER COLUMN grind_setting TYPE VARCHAR(120);
ALTER TABLE brew_sessions ALTER COLUMN actual_grind  TYPE VARCHAR(120);
