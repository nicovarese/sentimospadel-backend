ALTER TABLE tournaments ADD COLUMN format VARCHAR(20) NOT NULL DEFAULT 'LEAGUE';
ALTER TABLE tournaments ADD COLUMN americano_type VARCHAR(20);
ALTER TABLE tournaments ADD COLUMN open_enrollment BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE tournaments ADD COLUMN competitive BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE tournaments ADD COLUMN launched_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE tournaments ADD COLUMN available_courts INTEGER;
ALTER TABLE tournaments ADD COLUMN number_of_groups INTEGER;
ALTER TABLE tournaments ADD COLUMN league_rounds INTEGER;
ALTER TABLE tournaments ADD COLUMN points_for_win INTEGER NOT NULL DEFAULT 3;
ALTER TABLE tournaments ADD COLUMN points_for_tiebreak_loss INTEGER NOT NULL DEFAULT 1;
ALTER TABLE tournaments ADD COLUMN points_for_loss INTEGER NOT NULL DEFAULT 0;
ALTER TABLE tournaments ADD COLUMN standings_tiebreak VARCHAR(30) NOT NULL DEFAULT 'GAMES_DIFFERENCE';
ALTER TABLE tournaments ADD COLUMN court_names_json TEXT;

ALTER TABLE tournament_entries ADD COLUMN secondary_player_profile_id BIGINT;
ALTER TABLE tournament_entries ADD COLUMN created_by_player_id BIGINT;
ALTER TABLE tournament_entries ADD COLUMN team_name VARCHAR(160);
ALTER TABLE tournament_entries ADD COLUMN entry_status VARCHAR(20) NOT NULL DEFAULT 'CONFIRMED';
ALTER TABLE tournament_entries ADD COLUMN time_preferences_json TEXT;
ALTER TABLE tournament_entries ADD CONSTRAINT fk_tournament_entries_secondary_player
    FOREIGN KEY (secondary_player_profile_id) REFERENCES player_profiles (id);
ALTER TABLE tournament_entries ADD CONSTRAINT fk_tournament_entries_created_by_player
    FOREIGN KEY (created_by_player_id) REFERENCES player_profiles (id);

UPDATE tournament_entries
SET created_by_player_id = player_profile_id
WHERE created_by_player_id IS NULL;

CREATE INDEX idx_tournament_entries_secondary_player
    ON tournament_entries (secondary_player_profile_id);

CREATE TABLE tournament_matches (
    id BIGSERIAL PRIMARY KEY,
    tournament_id BIGINT NOT NULL,
    team_one_entry_id BIGINT NOT NULL,
    team_two_entry_id BIGINT NOT NULL,
    phase VARCHAR(30) NOT NULL,
    status VARCHAR(20) NOT NULL,
    round_number INTEGER NOT NULL,
    leg_number INTEGER,
    round_label VARCHAR(160) NOT NULL,
    scheduled_at TIMESTAMP WITH TIME ZONE,
    court_name VARCHAR(160),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_tournament_matches_tournament
        FOREIGN KEY (tournament_id) REFERENCES tournaments (id) ON DELETE CASCADE,
    CONSTRAINT fk_tournament_matches_team_one
        FOREIGN KEY (team_one_entry_id) REFERENCES tournament_entries (id),
    CONSTRAINT fk_tournament_matches_team_two
        FOREIGN KEY (team_two_entry_id) REFERENCES tournament_entries (id)
);

CREATE INDEX idx_tournament_matches_tournament_round
    ON tournament_matches (tournament_id, round_number, id);

CREATE TABLE tournament_match_results (
    id BIGSERIAL PRIMARY KEY,
    tournament_match_id BIGINT NOT NULL UNIQUE,
    submitted_by_player_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    winner_team VARCHAR(20) NOT NULL,
    set_one_team_one_games INTEGER,
    set_one_team_two_games INTEGER,
    set_two_team_one_games INTEGER,
    set_two_team_two_games INTEGER,
    set_three_team_one_games INTEGER,
    set_three_team_two_games INTEGER,
    submitted_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    confirmed_by_player_id BIGINT,
    confirmed_at TIMESTAMP WITH TIME ZONE,
    rejected_by_player_id BIGINT,
    rejected_at TIMESTAMP WITH TIME ZONE,
    rejection_reason VARCHAR(500),
    CONSTRAINT fk_tournament_match_results_match
        FOREIGN KEY (tournament_match_id) REFERENCES tournament_matches (id) ON DELETE CASCADE,
    CONSTRAINT fk_tournament_match_results_submitted_by
        FOREIGN KEY (submitted_by_player_id) REFERENCES player_profiles (id),
    CONSTRAINT fk_tournament_match_results_confirmed_by
        FOREIGN KEY (confirmed_by_player_id) REFERENCES player_profiles (id),
    CONSTRAINT fk_tournament_match_results_rejected_by
        FOREIGN KEY (rejected_by_player_id) REFERENCES player_profiles (id)
);

CREATE INDEX idx_tournament_match_results_status
    ON tournament_match_results (status);
