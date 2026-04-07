CREATE TABLE club_verification_requests (
    id BIGSERIAL PRIMARY KEY,
    player_profile_id BIGINT NOT NULL,
    club_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    reviewed_by_user_id BIGINT NULL,
    reviewed_at TIMESTAMP WITH TIME ZONE NULL,
    review_notes VARCHAR(1000) NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_club_verification_requests_player_profile
        FOREIGN KEY (player_profile_id) REFERENCES player_profiles (id),
    CONSTRAINT fk_club_verification_requests_club
        FOREIGN KEY (club_id) REFERENCES clubs (id),
    CONSTRAINT fk_club_verification_requests_reviewed_by_user
        FOREIGN KEY (reviewed_by_user_id) REFERENCES users (id)
);

CREATE INDEX ix_club_verification_requests_club_created_at
    ON club_verification_requests (club_id, created_at DESC);

CREATE INDEX ix_club_verification_requests_player_created_at
    ON club_verification_requests (player_profile_id, created_at DESC);
