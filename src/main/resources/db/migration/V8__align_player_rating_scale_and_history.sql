ALTER TABLE player_profiles
    RENAME COLUMN current_elo TO current_rating_legacy;

ALTER TABLE player_profiles
    ADD COLUMN current_rating NUMERIC(4, 2) NOT NULL DEFAULT 1.00;

UPDATE player_profiles
SET current_rating = CASE
    WHEN initial_rating IS NOT NULL THEN initial_rating
    WHEN current_rating_legacy BETWEEN 1 AND 7 THEN CAST(current_rating_legacy AS NUMERIC(4, 2))
    ELSE CAST(1.00 AS NUMERIC(4, 2))
END;

ALTER TABLE player_profiles
    DROP COLUMN current_rating_legacy;

ALTER TABLE player_profiles
    ADD COLUMN rated_matches_count INTEGER NOT NULL DEFAULT 0;

ALTER TABLE match_results
    ADD COLUMN rating_applied BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE match_results
    ADD COLUMN rating_applied_at TIMESTAMP WITH TIME ZONE;

CREATE TABLE player_rating_history (
    id BIGSERIAL PRIMARY KEY,
    player_profile_id BIGINT NOT NULL,
    match_id BIGINT NOT NULL,
    old_rating NUMERIC(4, 2) NOT NULL,
    delta NUMERIC(5, 2) NOT NULL,
    new_rating NUMERIC(4, 2) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_player_rating_history_player
        FOREIGN KEY (player_profile_id) REFERENCES player_profiles (id),
    CONSTRAINT fk_player_rating_history_match
        FOREIGN KEY (match_id) REFERENCES matches (id),
    CONSTRAINT uk_player_rating_history_match_player
        UNIQUE (match_id, player_profile_id)
);

CREATE INDEX idx_player_rating_history_player_created_at
    ON player_rating_history (player_profile_id, created_at DESC);
