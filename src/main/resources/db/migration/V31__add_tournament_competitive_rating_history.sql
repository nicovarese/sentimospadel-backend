ALTER TABLE tournament_match_results
    ADD COLUMN rating_applied BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE tournament_match_results
    ADD COLUMN rating_applied_at TIMESTAMP WITH TIME ZONE;

ALTER TABLE player_rating_history
    ALTER COLUMN match_id DROP NOT NULL;

ALTER TABLE player_rating_history
    ADD COLUMN tournament_match_id BIGINT;

ALTER TABLE player_rating_history
    ADD CONSTRAINT fk_player_rating_history_tournament_match
        FOREIGN KEY (tournament_match_id) REFERENCES tournament_matches (id) ON DELETE CASCADE;

ALTER TABLE player_rating_history
    ADD CONSTRAINT uk_player_rating_history_tournament_match_player
        UNIQUE (tournament_match_id, player_profile_id);
