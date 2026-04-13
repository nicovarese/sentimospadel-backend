CREATE TABLE push_device_installations (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    installation_id VARCHAR(120) NOT NULL,
    platform VARCHAR(20) NOT NULL,
    push_token VARCHAR(512) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    last_seen_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX uk_push_device_installations_installation
    ON push_device_installations(installation_id);

CREATE INDEX idx_push_device_installations_user_active
    ON push_device_installations(user_id, active, updated_at DESC);

CREATE TABLE push_notification_deliveries (
    id BIGSERIAL PRIMARY KEY,
    notification_id BIGINT NOT NULL REFERENCES player_notifications(id),
    installation_id BIGINT NOT NULL REFERENCES push_device_installations(id),
    status VARCHAR(20) NOT NULL,
    provider VARCHAR(40) NOT NULL,
    provider_message_id VARCHAR(255),
    reason VARCHAR(255),
    attempted_at TIMESTAMP WITH TIME ZONE,
    delivered_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_push_notification_deliveries_notification_installation
    ON push_notification_deliveries(notification_id, installation_id, created_at DESC);

CREATE INDEX idx_push_notification_deliveries_notification
    ON push_notification_deliveries(notification_id, created_at DESC);
