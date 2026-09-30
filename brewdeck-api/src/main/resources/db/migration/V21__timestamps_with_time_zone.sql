-- Store every timestamp as an absolute instant (TIMESTAMPTZ) instead of a zone-less wall-clock
-- TIMESTAMP. The application now uses java.time.Instant and returns ISO-8601 UTC ("...Z") in JSON;
-- clients render it in the viewer's zone.
--
-- Existing rows were written with the server's local clock, so they are interpreted in
-- ${legacy_timezone} (Flyway placeholder, env BREWDECK_LEGACY_TIMEZONE, default UTC). Run with the
-- zone the data was actually written in, e.g. America/Mexico_City for a CST developer database.

-- Fail fast on a typo'd zone instead of silently converting with a wrong offset.
SELECT now() AT TIME ZONE '${legacy_timezone}';

ALTER TABLE users
    ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE '${legacy_timezone}';

ALTER TABLE coffees
    ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE '${legacy_timezone}',
    ALTER COLUMN updated_at TYPE TIMESTAMPTZ USING updated_at AT TIME ZONE '${legacy_timezone}';

ALTER TABLE brew_methods
    ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE '${legacy_timezone}';

ALTER TABLE recipes
    ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE '${legacy_timezone}',
    ALTER COLUMN updated_at TYPE TIMESTAMPTZ USING updated_at AT TIME ZONE '${legacy_timezone}';

ALTER TABLE brew_sessions
    ALTER COLUMN brewed_at TYPE TIMESTAMPTZ USING brewed_at AT TIME ZONE '${legacy_timezone}';

ALTER TABLE password_reset_tokens
    ALTER COLUMN expires_at TYPE TIMESTAMPTZ USING expires_at AT TIME ZONE '${legacy_timezone}',
    ALTER COLUMN used_at TYPE TIMESTAMPTZ USING used_at AT TIME ZONE '${legacy_timezone}',
    ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE '${legacy_timezone}';

ALTER TABLE email_verification_tokens
    ALTER COLUMN expires_at TYPE TIMESTAMPTZ USING expires_at AT TIME ZONE '${legacy_timezone}',
    ALTER COLUMN used_at TYPE TIMESTAMPTZ USING used_at AT TIME ZONE '${legacy_timezone}',
    ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE '${legacy_timezone}';

ALTER TABLE refresh_tokens
    ALTER COLUMN expires_at TYPE TIMESTAMPTZ USING expires_at AT TIME ZONE '${legacy_timezone}',
    ALTER COLUMN used_at TYPE TIMESTAMPTZ USING used_at AT TIME ZONE '${legacy_timezone}',
    ALTER COLUMN revoked_at TYPE TIMESTAMPTZ USING revoked_at AT TIME ZONE '${legacy_timezone}',
    ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE '${legacy_timezone}';

ALTER TABLE feature_flags
    ALTER COLUMN expires_at TYPE TIMESTAMPTZ USING expires_at AT TIME ZONE '${legacy_timezone}',
    ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE '${legacy_timezone}',
    ALTER COLUMN updated_at TYPE TIMESTAMPTZ USING updated_at AT TIME ZONE '${legacy_timezone}';

-- Every application timestamp must now carry a zone (flyway_schema_history is Flyway's own).
DO $$
DECLARE
    remaining TEXT;
BEGIN
    SELECT string_agg(table_name || '.' || column_name, ', ')
      INTO remaining
      FROM information_schema.columns
     WHERE table_schema = current_schema()
       AND data_type = 'timestamp without time zone'
       AND table_name <> 'flyway_schema_history';
    IF remaining IS NOT NULL THEN
        RAISE EXCEPTION 'Timestamp columns without time zone remain: %', remaining;
    END IF;
END $$;
