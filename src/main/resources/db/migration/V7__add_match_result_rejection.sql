ALTER TABLE match_results
    ADD COLUMN rejected_by_player_id BIGINT;

ALTER TABLE match_results
    ADD COLUMN rejected_at TIMESTAMP WITH TIME ZONE;

ALTER TABLE match_results
    ADD COLUMN rejection_reason VARCHAR(255);

ALTER TABLE match_results
    ADD CONSTRAINT fk_match_results_rejected_by_player
        FOREIGN KEY (rejected_by_player_id) REFERENCES player_profiles (id);
