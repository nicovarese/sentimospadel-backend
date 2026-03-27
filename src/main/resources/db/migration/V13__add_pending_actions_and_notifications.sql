CREATE TABLE player_notifications (
    id BIGSERIAL PRIMARY KEY,
    player_profile_id BIGINT NOT NULL REFERENCES player_profiles(id),
    type VARCHAR(60) NOT NULL,
    status VARCHAR(20) NOT NULL,
    action_key VARCHAR(160) NOT NULL,
    title VARCHAR(160) NOT NULL,
    message VARCHAR(500) NOT NULL,
    match_id BIGINT REFERENCES matches(id),
    tournament_id BIGINT REFERENCES tournaments(id),
    tournament_match_id BIGINT REFERENCES tournament_matches(id),
    active BOOLEAN NOT NULL,
    read_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_player_notifications_player_action UNIQUE (player_profile_id, action_key)
);

CREATE INDEX idx_player_notifications_player_active
    ON player_notifications(player_profile_id, active, created_at DESC);
