-- Per-user UI language (ADR-015). NULL means the user has not chosen, so the web app follows the
-- browser language; existing users are not asked again.
ALTER TABLE users ADD COLUMN language VARCHAR(5);

ALTER TABLE users
    ADD CONSTRAINT chk_users_language CHECK (language IN ('EN', 'ES'));
