ALTER TABLE recipes ADD COLUMN share_token VARCHAR(32);

CREATE UNIQUE INDEX ux_recipes_share_token
    ON recipes (share_token)
    WHERE share_token IS NOT NULL;
