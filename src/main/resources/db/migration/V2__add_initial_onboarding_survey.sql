ALTER TABLE player_profiles
    ADD COLUMN survey_completed BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN survey_completed_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN initial_rating NUMERIC(4, 2),
    ADD COLUMN estimated_category VARCHAR(20),
    ADD COLUMN requires_club_verification BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN club_verification_status VARCHAR(20) NOT NULL DEFAULT 'NOT_REQUIRED',
    ADD COLUMN public_category_visible BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE initial_survey_submissions (
    id BIGSERIAL PRIMARY KEY,
    player_id BIGINT NOT NULL,
    survey_version INTEGER NOT NULL,
    q1 VARCHAR(1) NOT NULL,
    q2 VARCHAR(1) NOT NULL,
    q3 VARCHAR(1) NOT NULL,
    q4 VARCHAR(1) NOT NULL,
    q5 VARCHAR(1) NOT NULL,
    q6 VARCHAR(1) NOT NULL,
    q7 VARCHAR(1) NOT NULL,
    q8 VARCHAR(1) NOT NULL,
    q9 VARCHAR(1) NOT NULL,
    q10 VARCHAR(1) NOT NULL,
    weighted_score INTEGER NOT NULL,
    normalized_score NUMERIC(5, 2) NOT NULL,
    initial_rating NUMERIC(4, 2) NOT NULL,
    estimated_category VARCHAR(20) NOT NULL,
    requires_club_verification BOOLEAN NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_initial_survey_submissions_player
        FOREIGN KEY (player_id) REFERENCES player_profiles (id)
);

CREATE INDEX idx_initial_survey_submissions_player_created_at
    ON initial_survey_submissions (player_id, created_at DESC);
