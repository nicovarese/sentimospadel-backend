ALTER TABLE users
    ADD COLUMN accepted_terms_version VARCHAR(40);

ALTER TABLE users
    ADD COLUMN accepted_terms_at TIMESTAMP WITH TIME ZONE;

ALTER TABLE users
    ADD COLUMN accepted_privacy_version VARCHAR(40);

ALTER TABLE users
    ADD COLUMN accepted_privacy_at TIMESTAMP WITH TIME ZONE;

ALTER TABLE users
    ADD COLUMN consent_preferences_version VARCHAR(40);

ALTER TABLE users
    ADD COLUMN activity_tracking_enabled BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE users
    ADD COLUMN activity_tracking_updated_at TIMESTAMP WITH TIME ZONE;

ALTER TABLE users
    ADD COLUMN operational_notifications_enabled BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE users
    ADD COLUMN operational_notifications_updated_at TIMESTAMP WITH TIME ZONE;
