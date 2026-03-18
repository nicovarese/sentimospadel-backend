CREATE TABLE match_results (
    id BIGSERIAL PRIMARY KEY,
    match_id BIGINT NOT NULL UNIQUE,
    submitted_by_player_id BIGINT NOT NULL,
    winner_team VARCHAR(20) NOT NULL,
    team_one_score INTEGER NOT NULL,
    team_two_score INTEGER NOT NULL,
    submitted_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_match_results_match
        FOREIGN KEY (match_id) REFERENCES matches (id) ON DELETE CASCADE,
    CONSTRAINT fk_match_results_submitted_by_player
        FOREIGN KEY (submitted_by_player_id) REFERENCES player_profiles (id)
);
