ALTER TABLE users
    ADD COLUMN account_deletion_requested_at TIMESTAMP WITH TIME ZONE;

ALTER TABLE users
    ADD COLUMN account_deletion_reason VARCHAR(1000);
