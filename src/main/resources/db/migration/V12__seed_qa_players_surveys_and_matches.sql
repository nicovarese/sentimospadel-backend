INSERT INTO users (email, password_hash, role, status)
VALUES
    ('nicolas.seed@sentimospadel.test', '$2a$10$jdRNNvNGquYDiww0t4cYReOsxqAY8PxujaGzXtGQqAV16H8xcP3TW', 'PLAYER', 'ACTIVE'),
    ('martin.seed@sentimospadel.test', '$2a$10$jdRNNvNGquYDiww0t4cYReOsxqAY8PxujaGzXtGQqAV16H8xcP3TW', 'PLAYER', 'ACTIVE'),
    ('felipe.seed@sentimospadel.test', '$2a$10$jdRNNvNGquYDiww0t4cYReOsxqAY8PxujaGzXtGQqAV16H8xcP3TW', 'PLAYER', 'ACTIVE'),
    ('diego.seed@sentimospadel.test', '$2a$10$jdRNNvNGquYDiww0t4cYReOsxqAY8PxujaGzXtGQqAV16H8xcP3TW', 'PLAYER', 'ACTIVE'),
    ('ana.seed@sentimospadel.test', '$2a$10$jdRNNvNGquYDiww0t4cYReOsxqAY8PxujaGzXtGQqAV16H8xcP3TW', 'PLAYER', 'ACTIVE'),
    ('juan.seed@sentimospadel.test', '$2a$10$jdRNNvNGquYDiww0t4cYReOsxqAY8PxujaGzXtGQqAV16H8xcP3TW', 'PLAYER', 'ACTIVE'),
    ('lucia.seed@sentimospadel.test', '$2a$10$jdRNNvNGquYDiww0t4cYReOsxqAY8PxujaGzXtGQqAV16H8xcP3TW', 'PLAYER', 'ACTIVE'),
    ('paula.seed@sentimospadel.test', '$2a$10$jdRNNvNGquYDiww0t4cYReOsxqAY8PxujaGzXtGQqAV16H8xcP3TW', 'PLAYER', 'ACTIVE');

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
    seed.initial_rating,
    TRUE,
    0,
    0,
    TRUE,
    CURRENT_TIMESTAMP,
    seed.initial_rating,
    seed.estimated_category,
    seed.requires_club_verification,
    seed.club_verification_status
FROM users u
JOIN (
    VALUES
        ('nicolas.seed@sentimospadel.test', 'Nicolas Varese', 'Montevideo', 3.84, 'QUINTA', FALSE, 'NOT_REQUIRED'),
        ('martin.seed@sentimospadel.test', 'Martin Gomez', 'Montevideo', 4.75, 'CUARTA', FALSE, 'NOT_REQUIRED'),
        ('felipe.seed@sentimospadel.test', 'Felipe Rodriguez', 'Canelones', 5.16, 'TERCERA', FALSE, 'NOT_REQUIRED'),
        ('diego.seed@sentimospadel.test', 'Diego Fernandez', 'Montevideo', 5.85, 'SEGUNDA', TRUE, 'PENDING'),
        ('ana.seed@sentimospadel.test', 'Ana Pereira', 'Maldonado', 2.77, 'SEXTA', FALSE, 'NOT_REQUIRED'),
        ('juan.seed@sentimospadel.test', 'Juan Benitez', 'Montevideo', 4.08, 'QUINTA', FALSE, 'NOT_REQUIRED'),
        ('lucia.seed@sentimospadel.test', 'Lucia Moreira', 'Canelones', 5.79, 'SEGUNDA', TRUE, 'PENDING'),
        ('paula.seed@sentimospadel.test', 'Paula Sosa', 'Salto', 2.37, 'SEPTIMA', FALSE, 'NOT_REQUIRED')
) AS seed(email, full_name, city, initial_rating, estimated_category, requires_club_verification, club_verification_status)
    ON seed.email = u.email;

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
    p.id,
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
    seed.requires_club_verification
FROM player_profiles p
JOIN users u ON u.id = p.user_id
JOIN (
    VALUES
        ('nicolas.seed@sentimospadel.test', 'C', 'D', 'C', 'C', 'B', 'C', 'C', 'B', 'B', 'C', 75, 18.75, 3.84, 'QUINTA', FALSE),
        ('martin.seed@sentimospadel.test', 'D', 'C', 'D', 'C', 'C', 'C', 'D', 'C', 'C', 'D', 98, 24.50, 4.75, 'CUARTA', FALSE),
        ('felipe.seed@sentimospadel.test', 'D', 'D', 'D', 'C', 'D', 'D', 'C', 'D', 'D', 'D', 113, 28.25, 5.16, 'TERCERA', FALSE),
        ('diego.seed@sentimospadel.test', 'E', 'D', 'E', 'D', 'D', 'D', 'D', 'E', 'D', 'E', 138, 34.50, 5.85, 'SEGUNDA', TRUE),
        ('ana.seed@sentimospadel.test', 'B', 'C', 'C', 'B', 'B', 'B', 'A', 'B', 'B', 'C', 49, 12.25, 2.77, 'SEXTA', FALSE),
        ('juan.seed@sentimospadel.test', 'C', 'C', 'D', 'C', 'C', 'B', 'C', 'C', 'C', 'D', 81, 20.25, 4.08, 'QUINTA', FALSE),
        ('lucia.seed@sentimospadel.test', 'E', 'D', 'D', 'D', 'D', 'D', 'E', 'D', 'E', 'E', 136, 34.00, 5.79, 'SEGUNDA', TRUE),
        ('paula.seed@sentimospadel.test', 'B', 'B', 'B', 'C', 'B', 'B', 'B', 'A', 'B', 'B', 39, 9.75, 2.37, 'SEPTIMA', FALSE)
) AS seed(
    email, q1, q2, q3, q4, q5, q6, q7, q8, q9, q10,
    weighted_score, normalized_score, initial_rating, estimated_category, requires_club_verification
)
    ON seed.email = u.email;

INSERT INTO matches (created_by_player_id, status, scheduled_at, club_id, location_text, notes, max_players)
SELECT
    p.id,
    'OPEN',
    TIMESTAMP WITH TIME ZONE '2030-04-10 20:00:00+00:00',
    c.id,
    'Top Padel - Cancha 1',
    'Por los puntos',
    4
FROM player_profiles p
JOIN users u ON u.id = p.user_id
JOIN clubs c ON c.name = 'Top Padel'
WHERE u.email = 'nicolas.seed@sentimospadel.test';

INSERT INTO match_participants (match_id, player_profile_id, joined_at)
SELECT
    m.id,
    p.id,
    CURRENT_TIMESTAMP
FROM matches m
JOIN player_profiles p ON m.created_by_player_id = p.id
JOIN users u ON u.id = p.user_id
WHERE u.email = 'nicolas.seed@sentimospadel.test'
  AND m.scheduled_at = TIMESTAMP WITH TIME ZONE '2030-04-10 20:00:00+00:00';

INSERT INTO matches (created_by_player_id, status, scheduled_at, club_id, location_text, notes, max_players)
SELECT
    p.id,
    'OPEN',
    TIMESTAMP WITH TIME ZONE '2030-04-11 21:30:00+00:00',
    c.id,
    'World Padel - Cancha 2',
    'Recreativo',
    4
FROM player_profiles p
JOIN users u ON u.id = p.user_id
JOIN clubs c ON c.name = 'World Padel'
WHERE u.email = 'martin.seed@sentimospadel.test';

INSERT INTO match_participants (match_id, player_profile_id, joined_at)
SELECT m.id, p.id, CURRENT_TIMESTAMP
FROM matches m
JOIN users creator ON creator.email = 'martin.seed@sentimospadel.test'
JOIN player_profiles creator_profile ON creator_profile.user_id = creator.id
JOIN users u ON u.email IN (
    'martin.seed@sentimospadel.test',
    'felipe.seed@sentimospadel.test',
    'ana.seed@sentimospadel.test'
)
JOIN player_profiles p ON p.user_id = u.id
WHERE m.created_by_player_id = creator_profile.id
  AND m.scheduled_at = TIMESTAMP WITH TIME ZONE '2030-04-11 21:30:00+00:00';

INSERT INTO matches (created_by_player_id, status, scheduled_at, club_id, location_text, notes, max_players)
SELECT
    p.id,
    'FULL',
    TIMESTAMP WITH TIME ZONE '2030-04-12 19:00:00+00:00',
    c.id,
    'Cordon Padel - Cancha 1',
    'Por los puntos',
    4
FROM player_profiles p
JOIN users u ON u.id = p.user_id
JOIN clubs c ON c.name = 'Cordon Padel'
WHERE u.email = 'diego.seed@sentimospadel.test';

INSERT INTO match_participants (match_id, player_profile_id, team, joined_at)
SELECT
    m.id,
    p.id,
    seed.team,
    CURRENT_TIMESTAMP
FROM matches m
JOIN users creator ON creator.email = 'diego.seed@sentimospadel.test'
JOIN player_profiles creator_profile ON creator_profile.user_id = creator.id
JOIN (
    VALUES
        ('diego.seed@sentimospadel.test', 'TEAM_ONE'),
        ('juan.seed@sentimospadel.test', 'TEAM_ONE'),
        ('lucia.seed@sentimospadel.test', 'TEAM_TWO'),
        ('paula.seed@sentimospadel.test', 'TEAM_TWO')
) AS seed(email, team)
    ON 1 = 1
JOIN users u ON u.email = seed.email
JOIN player_profiles p ON p.user_id = u.id
WHERE m.created_by_player_id = creator_profile.id
  AND m.scheduled_at = TIMESTAMP WITH TIME ZONE '2030-04-12 19:00:00+00:00';

INSERT INTO matches (created_by_player_id, status, scheduled_at, club_id, location_text, notes, max_players)
SELECT
    p.id,
    'RESULT_PENDING',
    TIMESTAMP WITH TIME ZONE '2026-03-10 20:00:00+00:00',
    c.id,
    'Top Padel - Cancha 3',
    'Por los puntos',
    4
FROM player_profiles p
JOIN users u ON u.id = p.user_id
JOIN clubs c ON c.name = 'Top Padel'
WHERE u.email = 'nicolas.seed@sentimospadel.test';

INSERT INTO match_participants (match_id, player_profile_id, team, joined_at)
SELECT
    m.id,
    p.id,
    seed.team,
    CURRENT_TIMESTAMP
FROM matches m
JOIN users creator ON creator.email = 'nicolas.seed@sentimospadel.test'
JOIN player_profiles creator_profile ON creator_profile.user_id = creator.id
JOIN (
    VALUES
        ('nicolas.seed@sentimospadel.test', 'TEAM_ONE'),
        ('martin.seed@sentimospadel.test', 'TEAM_ONE'),
        ('ana.seed@sentimospadel.test', 'TEAM_TWO'),
        ('felipe.seed@sentimospadel.test', 'TEAM_TWO')
) AS seed(email, team)
    ON 1 = 1
JOIN users u ON u.email = seed.email
JOIN player_profiles p ON p.user_id = u.id
WHERE m.created_by_player_id = creator_profile.id
  AND m.scheduled_at = TIMESTAMP WITH TIME ZONE '2026-03-10 20:00:00+00:00';

INSERT INTO match_results (
    match_id,
    submitted_by_player_id,
    winner_team,
    team_one_score,
    team_two_score,
    submitted_at,
    status
)
SELECT
    m.id,
    p.id,
    'TEAM_ONE',
    2,
    1,
    CURRENT_TIMESTAMP,
    'PENDING'
FROM matches m
JOIN users u ON u.email = 'nicolas.seed@sentimospadel.test'
JOIN player_profiles p ON p.user_id = u.id
WHERE m.created_by_player_id = p.id
  AND m.scheduled_at = TIMESTAMP WITH TIME ZONE '2026-03-10 20:00:00+00:00';

INSERT INTO matches (created_by_player_id, status, scheduled_at, club_id, location_text, notes, max_players)
SELECT
    p.id,
    'CANCELLED',
    TIMESTAMP WITH TIME ZONE '2030-04-13 18:00:00+00:00',
    c.id,
    'Boss - Cancha 1',
    'Por los puntos',
    4
FROM player_profiles p
JOIN users u ON u.id = p.user_id
JOIN clubs c ON c.name = 'Boss'
WHERE u.email = 'juan.seed@sentimospadel.test';

INSERT INTO match_participants (match_id, player_profile_id, joined_at)
SELECT
    m.id,
    p.id,
    CURRENT_TIMESTAMP
FROM matches m
JOIN player_profiles p ON m.created_by_player_id = p.id
JOIN users u ON u.id = p.user_id
WHERE u.email = 'juan.seed@sentimospadel.test'
  AND m.scheduled_at = TIMESTAMP WITH TIME ZONE '2030-04-13 18:00:00+00:00';
