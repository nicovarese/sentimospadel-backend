ALTER TABLE users
    ADD COLUMN phone VARCHAR(40);

UPDATE users
SET phone = CONCAT('598000', LPAD(CAST(id AS VARCHAR), 6, '0'))
WHERE phone IS NULL;

ALTER TABLE users
    ALTER COLUMN phone SET NOT NULL;

ALTER TABLE users
    ADD CONSTRAINT uk_users_phone UNIQUE (phone);

ALTER TABLE users
    ADD COLUMN email_verified_at TIMESTAMP WITH TIME ZONE;

UPDATE users
SET email_verified_at = CURRENT_TIMESTAMP
WHERE email_verified_at IS NULL
  AND status = 'ACTIVE';

ALTER TABLE users
    ADD COLUMN email_verification_token_hash VARCHAR(64);

ALTER TABLE users
    ADD COLUMN email_verification_token_expires_at TIMESTAMP WITH TIME ZONE;
