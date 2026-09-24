-- Emails are case-insensitive, but they were stored and compared as typed, so "Juan@x.com" and
-- "juan@x.com" could register as two accounts. From now on the application stores emails trimmed and
-- lowercase; this migration normalizes existing rows and makes the database enforce it.

-- Refuse to merge accounts silently: if two rows differ only by case/whitespace, stop the
-- deployment so an operator decides which account survives.
DO $$
DECLARE
    duplicates TEXT;
BEGIN
    SELECT string_agg(normalized, ', ')
      INTO duplicates
      FROM (SELECT lower(trim(email)) AS normalized
              FROM users
             GROUP BY lower(trim(email))
            HAVING count(*) > 1) AS clash;

    IF duplicates IS NOT NULL THEN
        RAISE EXCEPTION 'Cannot normalize user emails: multiple accounts share these addresses ignoring case: %', duplicates;
    END IF;
END $$;

UPDATE users
   SET email = lower(trim(email))
 WHERE email <> lower(trim(email));

-- Together with the existing UNIQUE(email), this makes email uniqueness case-insensitive.
ALTER TABLE users
    ADD CONSTRAINT chk_users_email_normalized CHECK (email = lower(trim(email)));
