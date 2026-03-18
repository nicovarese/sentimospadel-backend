CREATE TABLE matches (
    id BIGSERIAL PRIMARY KEY,
    created_by_player_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    scheduled_at TIMESTAMP WITH TIME ZONE NOT NULL,
    club_id BIGINT,
    location_text VARCHAR(255),
    notes VARCHAR(1000),
    max_players INTEGER NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_matches_created_by_player
        FOREIGN KEY (created_by_player_id) REFERENCES player_profiles (id),
    CONSTRAINT fk_matches_club
        FOREIGN KEY (club_id) REFERENCES clubs (id)
);

CREATE INDEX idx_matches_scheduled_at ON matches (scheduled_at);
CREATE INDEX idx_matches_status ON matches (status);

CREATE TABLE match_participants (
    id BIGSERIAL PRIMARY KEY,
    match_id BIGINT NOT NULL,
    player_profile_id BIGINT NOT NULL,
    joined_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_match_participants_match
        FOREIGN KEY (match_id) REFERENCES matches (id) ON DELETE CASCADE,
    CONSTRAINT fk_match_participants_player
        FOREIGN KEY (player_profile_id) REFERENCES player_profiles (id),
    CONSTRAINT uk_match_participants_match_player
        UNIQUE (match_id, player_profile_id)
);

CREATE INDEX idx_match_participants_match ON match_participants (match_id, joined_at);
