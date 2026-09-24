-- Role-based authorization. Every account is a regular USER; ADMIN is granted only out-of-band
-- (see AdminBootstrap / BREWDECK_ADMIN_EMAIL), never through a public endpoint.
ALTER TABLE users ADD COLUMN role VARCHAR(20) NOT NULL DEFAULT 'USER';

ALTER TABLE users
    ADD CONSTRAINT chk_users_role CHECK (role IN ('USER', 'ADMIN'));
