ALTER TABLE match_participants
    ADD COLUMN team VARCHAR(20);

ALTER TABLE match_results
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'PENDING';

ALTER TABLE match_results
    ADD COLUMN confirmed_by_player_id BIGINT;

ALTER TABLE match_results
    ADD COLUMN confirmed_at TIMESTAMP WITH TIME ZONE;

ALTER TABLE match_results
    ADD CONSTRAINT fk_match_results_confirmed_by_player
        FOREIGN KEY (confirmed_by_player_id) REFERENCES player_profiles (id);
