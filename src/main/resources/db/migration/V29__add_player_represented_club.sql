ALTER TABLE player_profiles
    ADD COLUMN represented_club_id BIGINT NULL;

ALTER TABLE player_profiles
    ADD CONSTRAINT fk_player_profiles_represented_club
        FOREIGN KEY (represented_club_id) REFERENCES clubs (id);

CREATE INDEX idx_player_profiles_represented_club_id
    ON player_profiles (represented_club_id);
