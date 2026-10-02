-- Per-user light/dark theme. NULL means the user has not chosen yet, which is what makes the web
-- app show its one-time theme dialog, so existing users get that dialog once too.
ALTER TABLE users ADD COLUMN theme_preference VARCHAR(10);

ALTER TABLE users
    ADD CONSTRAINT chk_users_theme_preference CHECK (theme_preference IN ('LIGHT', 'DARK'));
