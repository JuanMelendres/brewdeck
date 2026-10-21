-- When the retention purge last reminded an unverified account that it will be deleted (AUTH-06,
-- ADR-021). Null until a reminder is sent; the purge deletes only accounts reminded at least 2 days
-- earlier. Verified accounts never get one.
ALTER TABLE users
    ADD COLUMN retention_reminder_sent_at TIMESTAMPTZ;
