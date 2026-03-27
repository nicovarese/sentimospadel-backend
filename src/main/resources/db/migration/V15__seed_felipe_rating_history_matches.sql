INSERT INTO users (email, password_hash, role, status)
SELECT seed.email, seed.password_hash, seed.role, seed.status
FROM (
    VALUES
        ('alvaro.historia@sentimospadel.test', '$2a$10$jdRNNvNGquYDiww0t4cYReOsxqAY8PxujaGzXtGQqAV16H8xcP3TW', 'PLAYER', 'ACTIVE'),
        ('bruno.historia@sentimospadel.test', '$2a$10$jdRNNvNGquYDiww0t4cYReOsxqAY8PxujaGzXtGQqAV16H8xcP3TW', 'PLAYER', 'ACTIVE'),
        ('carla.historia@sentimospadel.test', '$2a$10$jdRNNvNGquYDiww0t4cYReOsxqAY8PxujaGzXtGQqAV16H8xcP3TW', 'PLAYER', 'ACTIVE'),
        ('valentina.historia@sentimospadel.test', '$2a$10$jdRNNvNGquYDiww0t4cYReOsxqAY8PxujaGzXtGQqAV16H8xcP3TW', 'PLAYER', 'ACTIVE')
) AS seed(email, password_hash, role, status)
WHERE NOT EXISTS (
    SELECT 1
    FROM users existing
    WHERE existing.email = seed.email
);

INSERT INTO player_profiles (
    user_id,
    full_name,
    city,
    current_rating,
    provisional,
    matches_played,
    rated_matches_count,
    survey_completed,
    survey_completed_at,
    initial_rating,
    estimated_category,
    requires_club_verification,
    club_verification_status
)
SELECT
    u.id,
    seed.full_name,
    seed.city,
    seed.current_rating,
    FALSE,
    seed.matches_played,
    seed.rated_matches_count,
    TRUE,
    CURRENT_TIMESTAMP,
    seed.initial_rating,
    seed.estimated_category,
    FALSE,
    'NOT_REQUIRED'
FROM users u
JOIN (
    VALUES
        ('alvaro.historia@sentimospadel.test', 'Alvaro Costa', 'Montevideo', 4.97, 4, 4, 4.90, 'CUARTA'),
        ('bruno.historia@sentimospadel.test', 'Bruno Silva', 'Canelones', 5.04, 3, 3, 5.00, 'TERCERA'),
        ('carla.historia@sentimospadel.test', 'Carla Nuñez', 'Montevideo', 4.61, 2, 2, 4.70, 'CUARTA'),
        ('valentina.historia@sentimospadel.test', 'Valentina Reyes', 'Maldonado', 4.93, 3, 3, 5.10, 'TERCERA')
) AS seed(email, full_name, city, current_rating, matches_played, rated_matches_count, initial_rating, estimated_category)
    ON seed.email = u.email
WHERE NOT EXISTS (
    SELECT 1
    FROM player_profiles existing
    WHERE existing.user_id = u.id
);

INSERT INTO initial_survey_submissions (
    player_id,
    survey_version,
    q1,
    q2,
    q3,
    q4,
    q5,
    q6,
    q7,
    q8,
    q9,
    q10,
    weighted_score,
    normalized_score,
    initial_rating,
    estimated_category,
    requires_club_verification
)
SELECT
    profile.id,
    1,
    seed.q1,
    seed.q2,
    seed.q3,
    seed.q4,
    seed.q5,
    seed.q6,
    seed.q7,
    seed.q8,
    seed.q9,
    seed.q10,
    seed.weighted_score,
    seed.normalized_score,
    seed.initial_rating,
    seed.estimated_category,
    FALSE
FROM player_profiles profile
JOIN users u ON u.id = profile.user_id
JOIN (
    VALUES
        ('alvaro.historia@sentimospadel.test', 'D', 'C', 'D', 'C', 'C', 'C', 'C', 'D', 'C', 'C', 96, 24.00, 4.90, 'CUARTA'),
        ('bruno.historia@sentimospadel.test', 'D', 'D', 'D', 'C', 'D', 'C', 'D', 'D', 'C', 'D', 106, 26.50, 5.00, 'TERCERA'),
        ('carla.historia@sentimospadel.test', 'D', 'C', 'C', 'C', 'C', 'C', 'C', 'C', 'C', 'C', 94, 23.50, 4.70, 'CUARTA'),
        ('valentina.historia@sentimospadel.test', 'D', 'D', 'D', 'C', 'D', 'D', 'D', 'D', 'C', 'D', 108, 27.00, 5.10, 'TERCERA')
) AS seed(
    email, q1, q2, q3, q4, q5, q6, q7, q8, q9, q10,
    weighted_score, normalized_score, initial_rating, estimated_category
) ON seed.email = u.email
WHERE NOT EXISTS (
    SELECT 1
    FROM initial_survey_submissions existing
    WHERE existing.player_id = profile.id
);

INSERT INTO matches (created_by_player_id, status, scheduled_at, club_id, location_text, notes, max_players, created_at, updated_at)
SELECT
    creator_profile.id,
    'COMPLETED',
    seed.scheduled_at,
    club.id,
    seed.location_text,
    'Historial Felipe',
    4,
    seed.created_at,
    seed.created_at
FROM (
    VALUES
        ('Historial Felipe 1', 'Top Padel', TIMESTAMP WITH TIME ZONE '2026-02-18 20:00:00+00:00', TIMESTAMP WITH TIME ZONE '2026-02-18 22:00:00+00:00', 'felipe.seed@sentimospadel.test'),
        ('Historial Felipe 2', 'World Padel', TIMESTAMP WITH TIME ZONE '2026-02-26 20:30:00+00:00', TIMESTAMP WITH TIME ZONE '2026-02-26 22:30:00+00:00', 'felipe.seed@sentimospadel.test'),
        ('Historial Felipe 3', 'Cordon Padel', TIMESTAMP WITH TIME ZONE '2026-03-04 19:30:00+00:00', TIMESTAMP WITH TIME ZONE '2026-03-04 21:30:00+00:00', 'felipe.seed@sentimospadel.test'),
        ('Historial Felipe 4', 'Boss', TIMESTAMP WITH TIME ZONE '2026-03-08 20:00:00+00:00', TIMESTAMP WITH TIME ZONE '2026-03-08 22:00:00+00:00', 'felipe.seed@sentimospadel.test')
) AS seed(location_text, club_name, scheduled_at, created_at, creator_email)
JOIN users creator_user ON creator_user.email = seed.creator_email
JOIN player_profiles creator_profile ON creator_profile.user_id = creator_user.id
JOIN clubs club ON club.name = seed.club_name
WHERE NOT EXISTS (
    SELECT 1
    FROM matches existing
    WHERE existing.location_text = seed.location_text
);

INSERT INTO match_participants (match_id, player_profile_id, team, joined_at)
SELECT
    match.id,
    participant_profile.id,
    seed.team,
    seed.joined_at
FROM (
    VALUES
        ('Historial Felipe 1', 'felipe.seed@sentimospadel.test', 'TEAM_ONE', TIMESTAMP WITH TIME ZONE '2026-02-18 19:55:00+00:00'),
        ('Historial Felipe 1', 'alvaro.historia@sentimospadel.test', 'TEAM_ONE', TIMESTAMP WITH TIME ZONE '2026-02-18 19:55:00+00:00'),
        ('Historial Felipe 1', 'bruno.historia@sentimospadel.test', 'TEAM_TWO', TIMESTAMP WITH TIME ZONE '2026-02-18 19:56:00+00:00'),
        ('Historial Felipe 1', 'carla.historia@sentimospadel.test', 'TEAM_TWO', TIMESTAMP WITH TIME ZONE '2026-02-18 19:56:00+00:00'),
        ('Historial Felipe 2', 'felipe.seed@sentimospadel.test', 'TEAM_ONE', TIMESTAMP WITH TIME ZONE '2026-02-26 20:25:00+00:00'),
        ('Historial Felipe 2', 'valentina.historia@sentimospadel.test', 'TEAM_ONE', TIMESTAMP WITH TIME ZONE '2026-02-26 20:25:00+00:00'),
        ('Historial Felipe 2', 'bruno.historia@sentimospadel.test', 'TEAM_TWO', TIMESTAMP WITH TIME ZONE '2026-02-26 20:26:00+00:00'),
        ('Historial Felipe 2', 'alvaro.historia@sentimospadel.test', 'TEAM_TWO', TIMESTAMP WITH TIME ZONE '2026-02-26 20:26:00+00:00'),
        ('Historial Felipe 3', 'felipe.seed@sentimospadel.test', 'TEAM_ONE', TIMESTAMP WITH TIME ZONE '2026-03-04 19:25:00+00:00'),
        ('Historial Felipe 3', 'alvaro.historia@sentimospadel.test', 'TEAM_ONE', TIMESTAMP WITH TIME ZONE '2026-03-04 19:25:00+00:00'),
        ('Historial Felipe 3', 'valentina.historia@sentimospadel.test', 'TEAM_TWO', TIMESTAMP WITH TIME ZONE '2026-03-04 19:26:00+00:00'),
        ('Historial Felipe 3', 'carla.historia@sentimospadel.test', 'TEAM_TWO', TIMESTAMP WITH TIME ZONE '2026-03-04 19:26:00+00:00'),
        ('Historial Felipe 4', 'felipe.seed@sentimospadel.test', 'TEAM_ONE', TIMESTAMP WITH TIME ZONE '2026-03-08 19:55:00+00:00'),
        ('Historial Felipe 4', 'bruno.historia@sentimospadel.test', 'TEAM_ONE', TIMESTAMP WITH TIME ZONE '2026-03-08 19:55:00+00:00'),
        ('Historial Felipe 4', 'alvaro.historia@sentimospadel.test', 'TEAM_TWO', TIMESTAMP WITH TIME ZONE '2026-03-08 19:56:00+00:00'),
        ('Historial Felipe 4', 'valentina.historia@sentimospadel.test', 'TEAM_TWO', TIMESTAMP WITH TIME ZONE '2026-03-08 19:56:00+00:00')
) AS seed(location_text, participant_email, team, joined_at)
JOIN matches match ON match.location_text = seed.location_text
JOIN users participant_user ON participant_user.email = seed.participant_email
JOIN player_profiles participant_profile ON participant_profile.user_id = participant_user.id
WHERE NOT EXISTS (
    SELECT 1
    FROM match_participants existing
    WHERE existing.match_id = match.id
      AND existing.player_profile_id = participant_profile.id
);

INSERT INTO match_results (
    match_id,
    submitted_by_player_id,
    status,
    winner_team,
    team_one_score,
    team_two_score,
    submitted_at,
    confirmed_by_player_id,
    confirmed_at,
    rating_applied,
    rating_applied_at
)
SELECT
    match.id,
    submitted_by_profile.id,
    'CONFIRMED',
    seed.winner_team,
    seed.team_one_score,
    seed.team_two_score,
    seed.confirmed_at,
    confirmed_by_profile.id,
    seed.confirmed_at,
    TRUE,
    seed.confirmed_at
FROM (
    VALUES
        ('Historial Felipe 1', 'felipe.seed@sentimospadel.test', 'bruno.historia@sentimospadel.test', 'TEAM_ONE', 2, 0, TIMESTAMP WITH TIME ZONE '2026-02-18 22:00:00+00:00'),
        ('Historial Felipe 2', 'bruno.historia@sentimospadel.test', 'felipe.seed@sentimospadel.test', 'TEAM_TWO', 0, 2, TIMESTAMP WITH TIME ZONE '2026-02-26 22:30:00+00:00'),
        ('Historial Felipe 3', 'felipe.seed@sentimospadel.test', 'valentina.historia@sentimospadel.test', 'TEAM_ONE', 2, 1, TIMESTAMP WITH TIME ZONE '2026-03-04 21:30:00+00:00'),
        ('Historial Felipe 4', 'felipe.seed@sentimospadel.test', 'alvaro.historia@sentimospadel.test', 'TEAM_ONE', 2, 0, TIMESTAMP WITH TIME ZONE '2026-03-08 22:00:00+00:00')
) AS seed(location_text, submitted_by_email, confirmed_by_email, winner_team, team_one_score, team_two_score, confirmed_at)
JOIN matches match ON match.location_text = seed.location_text
JOIN users submitted_by_user ON submitted_by_user.email = seed.submitted_by_email
JOIN player_profiles submitted_by_profile ON submitted_by_profile.user_id = submitted_by_user.id
JOIN users confirmed_by_user ON confirmed_by_user.email = seed.confirmed_by_email
JOIN player_profiles confirmed_by_profile ON confirmed_by_profile.user_id = confirmed_by_user.id
WHERE NOT EXISTS (
    SELECT 1
    FROM match_results existing
    WHERE existing.match_id = match.id
);

INSERT INTO player_rating_history (player_profile_id, match_id, old_rating, delta, new_rating, created_at)
SELECT
    profile.id,
    match.id,
    seed.old_rating,
    seed.delta,
    seed.new_rating,
    seed.created_at
FROM (
    VALUES
        ('Historial Felipe 1', 'felipe.seed@sentimospadel.test', 5.00, 0.05, 5.05, TIMESTAMP WITH TIME ZONE '2026-02-18 22:00:00+00:00'),
        ('Historial Felipe 1', 'alvaro.historia@sentimospadel.test', 4.90, 0.04, 4.94, TIMESTAMP WITH TIME ZONE '2026-02-18 22:00:00+00:00'),
        ('Historial Felipe 1', 'bruno.historia@sentimospadel.test', 5.00, -0.05, 4.95, TIMESTAMP WITH TIME ZONE '2026-02-18 22:00:00+00:00'),
        ('Historial Felipe 1', 'carla.historia@sentimospadel.test', 4.70, -0.04, 4.66, TIMESTAMP WITH TIME ZONE '2026-02-18 22:00:00+00:00'),
        ('Historial Felipe 2', 'felipe.seed@sentimospadel.test', 5.05, -0.03, 5.02, TIMESTAMP WITH TIME ZONE '2026-02-26 22:30:00+00:00'),
        ('Historial Felipe 2', 'valentina.historia@sentimospadel.test', 5.10, -0.05, 5.05, TIMESTAMP WITH TIME ZONE '2026-02-26 22:30:00+00:00'),
        ('Historial Felipe 2', 'bruno.historia@sentimospadel.test', 4.95, 0.04, 4.99, TIMESTAMP WITH TIME ZONE '2026-02-26 22:30:00+00:00'),
        ('Historial Felipe 2', 'alvaro.historia@sentimospadel.test', 4.94, 0.03, 4.97, TIMESTAMP WITH TIME ZONE '2026-02-26 22:30:00+00:00'),
        ('Historial Felipe 3', 'felipe.seed@sentimospadel.test', 5.02, 0.08, 5.10, TIMESTAMP WITH TIME ZONE '2026-03-04 21:30:00+00:00'),
        ('Historial Felipe 3', 'alvaro.historia@sentimospadel.test', 4.97, 0.06, 5.03, TIMESTAMP WITH TIME ZONE '2026-03-04 21:30:00+00:00'),
        ('Historial Felipe 3', 'valentina.historia@sentimospadel.test', 5.05, -0.07, 4.98, TIMESTAMP WITH TIME ZONE '2026-03-04 21:30:00+00:00'),
        ('Historial Felipe 3', 'carla.historia@sentimospadel.test', 4.66, -0.05, 4.61, TIMESTAMP WITH TIME ZONE '2026-03-04 21:30:00+00:00'),
        ('Historial Felipe 4', 'felipe.seed@sentimospadel.test', 5.10, 0.06, 5.16, TIMESTAMP WITH TIME ZONE '2026-03-08 22:00:00+00:00'),
        ('Historial Felipe 4', 'bruno.historia@sentimospadel.test', 4.99, 0.05, 5.04, TIMESTAMP WITH TIME ZONE '2026-03-08 22:00:00+00:00'),
        ('Historial Felipe 4', 'alvaro.historia@sentimospadel.test', 5.03, -0.06, 4.97, TIMESTAMP WITH TIME ZONE '2026-03-08 22:00:00+00:00'),
        ('Historial Felipe 4', 'valentina.historia@sentimospadel.test', 4.98, -0.05, 4.93, TIMESTAMP WITH TIME ZONE '2026-03-08 22:00:00+00:00')
) AS seed(location_text, email, old_rating, delta, new_rating, created_at)
JOIN matches match ON match.location_text = seed.location_text
JOIN users u ON u.email = seed.email
JOIN player_profiles profile ON profile.user_id = u.id
WHERE NOT EXISTS (
    SELECT 1
    FROM player_rating_history existing
    WHERE existing.player_profile_id = profile.id
      AND existing.match_id = match.id
);

UPDATE player_profiles
SET
    matches_played = CASE WHEN matches_played < 5 THEN 5 ELSE matches_played END,
    rated_matches_count = CASE WHEN rated_matches_count < 5 THEN 5 ELSE rated_matches_count END
WHERE user_id = (
    SELECT id
    FROM users
    WHERE email = 'felipe.seed@sentimospadel.test'
);
