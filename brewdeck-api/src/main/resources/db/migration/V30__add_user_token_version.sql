-- Access JWTs carry the user's token version (claim "ver"). Logout, password change, and password
-- reset bump it, so access tokens issued before stop working at once instead of living out their
-- 15-minute TTL (backlog AUTH-05.6, ADR-013). Existing rows start at 0, which matches tokens
-- issued before this migration (they have no "ver" claim, read as 0).
ALTER TABLE users ADD COLUMN token_version INTEGER NOT NULL DEFAULT 0;
