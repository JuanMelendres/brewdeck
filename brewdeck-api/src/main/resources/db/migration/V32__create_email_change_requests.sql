-- Pending email changes (backlog AUTH-04, ADR-019). A request holds the new address until the user
-- confirms it from that inbox; the old inbox gets a cancel link. Only SHA-256 hashes of the two
-- tokens are stored; the raw tokens live only in the emailed links.
CREATE TABLE email_change_requests (
    id                 BIGSERIAL PRIMARY KEY,
    user_id            BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    new_email          VARCHAR(255) NOT NULL,
    confirm_token_hash VARCHAR(64) NOT NULL UNIQUE,
    cancel_token_hash  VARCHAR(64) NOT NULL UNIQUE,
    expires_at         TIMESTAMPTZ NOT NULL,
    confirmed_at       TIMESTAMPTZ,
    cancelled_at       TIMESTAMPTZ,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    -- Same canonical form as users.email (V18), so the confirm step compares like with like.
    CONSTRAINT chk_email_change_requests_email_normalized CHECK (new_email = lower(trim(new_email))),
    CONSTRAINT chk_email_change_requests_one_outcome CHECK (confirmed_at IS NULL OR cancelled_at IS NULL)
);
CREATE INDEX idx_email_change_requests_user ON email_change_requests (user_id);
