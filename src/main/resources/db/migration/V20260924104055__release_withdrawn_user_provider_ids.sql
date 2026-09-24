ALTER TABLE users ALTER COLUMN provider_id DROP NOT NULL;

UPDATE users SET provider_id = NULL WHERE deleted_at IS NOT NULL;

ALTER TABLE users ADD CONSTRAINT ck_users_identity_requires_withdrawal
    CHECK (provider_id IS NOT NULL OR deleted_at IS NOT NULL);
