-- Which version of the terms and privacy notice each user accepted, and when (backlog LEGAL-01).
-- Nullable: accounts created before the texts were published have accepted no version yet.
ALTER TABLE users
    ADD COLUMN terms_version     VARCHAR(20),
    ADD COLUMN terms_accepted_at TIMESTAMPTZ;
