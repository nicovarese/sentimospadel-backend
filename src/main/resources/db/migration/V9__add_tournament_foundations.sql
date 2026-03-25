CREATE TABLE tournaments (
    id BIGSERIAL PRIMARY KEY,
    created_by_player_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(1000),
    club_id BIGINT,
    city VARCHAR(120),
    start_date DATE NOT NULL,
    end_date DATE,
    status VARCHAR(20) NOT NULL,
    max_entries INTEGER,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_tournaments_created_by_player
        FOREIGN KEY (created_by_player_id) REFERENCES player_profiles (id),
    CONSTRAINT fk_tournaments_club
        FOREIGN KEY (club_id) REFERENCES clubs (id)
);

CREATE INDEX idx_tournaments_start_date ON tournaments (start_date);
CREATE INDEX idx_tournaments_status ON tournaments (status);

CREATE TABLE tournament_entries (
    id BIGSERIAL PRIMARY KEY,
    tournament_id BIGINT NOT NULL,
    player_profile_id BIGINT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_tournament_entries_tournament
        FOREIGN KEY (tournament_id) REFERENCES tournaments (id) ON DELETE CASCADE,
    CONSTRAINT fk_tournament_entries_player
        FOREIGN KEY (player_profile_id) REFERENCES player_profiles (id),
    CONSTRAINT uk_tournament_entries_tournament_player
        UNIQUE (tournament_id, player_profile_id)
);

CREATE INDEX idx_tournament_entries_tournament ON tournament_entries (tournament_id, created_at);
