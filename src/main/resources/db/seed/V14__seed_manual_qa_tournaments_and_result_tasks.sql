INSERT INTO matches (created_by_player_id, status, scheduled_at, club_id, location_text, notes, max_players)
SELECT
    creator_profile.id,
    'OPEN',
    TIMESTAMP WITH TIME ZONE '2026-03-29 21:30:00+00:00',
    club.id,
    'World Padel - Cancha QA Join',
    'QA Social Join',
    4
FROM users creator
JOIN player_profiles creator_profile ON creator_profile.user_id = creator.id
JOIN clubs club ON club.name = 'World Padel'
WHERE creator.email = 'martin.seed@sentimospadel.test'
  AND NOT EXISTS (
      SELECT 1
      FROM matches existing
      WHERE existing.location_text = 'World Padel - Cancha QA Join'
  );

INSERT INTO match_participants (match_id, player_profile_id, joined_at)
SELECT
    match.id,
    participant_profile.id,
    CURRENT_TIMESTAMP
FROM matches match
JOIN users creator ON creator.email = 'martin.seed@sentimospadel.test'
JOIN player_profiles creator_profile ON creator_profile.user_id = creator.id
JOIN (
    VALUES
        ('martin.seed@sentimospadel.test'),
        ('felipe.seed@sentimospadel.test'),
        ('ana.seed@sentimospadel.test')
) AS seed(email) ON 1 = 1
JOIN users participant_user ON participant_user.email = seed.email
JOIN player_profiles participant_profile ON participant_profile.user_id = participant_user.id
WHERE match.created_by_player_id = creator_profile.id
  AND match.location_text = 'World Padel - Cancha QA Join'
  AND NOT EXISTS (
      SELECT 1
      FROM match_participants existing
      WHERE existing.match_id = match.id
        AND existing.player_profile_id = participant_profile.id
  );

INSERT INTO matches (created_by_player_id, status, scheduled_at, club_id, location_text, notes, max_players)
SELECT
    creator_profile.id,
    'FULL',
    TIMESTAMP WITH TIME ZONE '2026-03-24 21:00:00+00:00',
    club.id,
    'Top Padel - Cancha QA Submit',
    'QA Social Submit',
    4
FROM users creator
JOIN player_profiles creator_profile ON creator_profile.user_id = creator.id
JOIN clubs club ON club.name = 'Top Padel'
WHERE creator.email = 'diego.seed@sentimospadel.test'
  AND NOT EXISTS (
      SELECT 1
      FROM matches existing
      WHERE existing.location_text = 'Top Padel - Cancha QA Submit'
  );

INSERT INTO match_participants (match_id, player_profile_id, team, joined_at)
SELECT
    match.id,
    participant_profile.id,
    seed.team,
    CURRENT_TIMESTAMP
FROM matches match
JOIN users creator ON creator.email = 'diego.seed@sentimospadel.test'
JOIN player_profiles creator_profile ON creator_profile.user_id = creator.id
JOIN (
    VALUES
        ('diego.seed@sentimospadel.test', 'TEAM_ONE'),
        ('juan.seed@sentimospadel.test', 'TEAM_ONE'),
        ('lucia.seed@sentimospadel.test', 'TEAM_TWO'),
        ('paula.seed@sentimospadel.test', 'TEAM_TWO')
) AS seed(email, team) ON 1 = 1
JOIN users participant_user ON participant_user.email = seed.email
JOIN player_profiles participant_profile ON participant_profile.user_id = participant_user.id
WHERE match.created_by_player_id = creator_profile.id
  AND match.location_text = 'Top Padel - Cancha QA Submit'
  AND NOT EXISTS (
      SELECT 1
      FROM match_participants existing
      WHERE existing.match_id = match.id
        AND existing.player_profile_id = participant_profile.id
  );

INSERT INTO matches (created_by_player_id, status, scheduled_at, club_id, location_text, notes, max_players)
SELECT
    creator_profile.id,
    'RESULT_PENDING',
    TIMESTAMP WITH TIME ZONE '2026-03-23 20:00:00+00:00',
    club.id,
    'Top Padel - Cancha QA Validacion',
    'QA Social Validacion',
    4
FROM users creator
JOIN player_profiles creator_profile ON creator_profile.user_id = creator.id
JOIN clubs club ON club.name = 'Top Padel'
WHERE creator.email = 'nicolas.seed@sentimospadel.test'
  AND NOT EXISTS (
      SELECT 1
      FROM matches existing
      WHERE existing.location_text = 'Top Padel - Cancha QA Validacion'
  );

INSERT INTO match_participants (match_id, player_profile_id, team, joined_at)
SELECT
    match.id,
    participant_profile.id,
    seed.team,
    CURRENT_TIMESTAMP
FROM matches match
JOIN users creator ON creator.email = 'nicolas.seed@sentimospadel.test'
JOIN player_profiles creator_profile ON creator_profile.user_id = creator.id
JOIN (
    VALUES
        ('nicolas.seed@sentimospadel.test', 'TEAM_ONE'),
        ('martin.seed@sentimospadel.test', 'TEAM_ONE'),
        ('ana.seed@sentimospadel.test', 'TEAM_TWO'),
        ('felipe.seed@sentimospadel.test', 'TEAM_TWO')
) AS seed(email, team) ON 1 = 1
JOIN users participant_user ON participant_user.email = seed.email
JOIN player_profiles participant_profile ON participant_profile.user_id = participant_user.id
WHERE match.created_by_player_id = creator_profile.id
  AND match.location_text = 'Top Padel - Cancha QA Validacion'
  AND NOT EXISTS (
      SELECT 1
      FROM match_participants existing
      WHERE existing.match_id = match.id
        AND existing.player_profile_id = participant_profile.id
  );

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
    match.id,
    submitter_profile.id,
    'TEAM_ONE',
    2,
    0,
    CURRENT_TIMESTAMP,
    'PENDING'
FROM matches match
JOIN users submitter_user ON submitter_user.email = 'nicolas.seed@sentimospadel.test'
JOIN player_profiles submitter_profile ON submitter_profile.user_id = submitter_user.id
WHERE match.location_text = 'Top Padel - Cancha QA Validacion'
  AND NOT EXISTS (
      SELECT 1
      FROM match_results existing
      WHERE existing.match_id = match.id
  );

INSERT INTO tournaments (
    created_by_player_id,
    name,
    description,
    format,
    club_id,
    city,
    start_date,
    end_date,
    status,
    max_entries,
    open_enrollment,
    competitive,
    available_courts,
    number_of_groups,
    league_rounds,
    points_for_win,
    points_for_tiebreak_loss,
    points_for_loss,
    standings_tiebreak,
    court_names_json
)
SELECT
    creator_profile.id,
    'Liga QA Abierta',
    'Seed QA para probar join y leave de ligas desde frontend',
    'LEAGUE',
    club.id,
    club.city,
    DATE '2026-04-05',
    DATE '2026-04-30',
    'OPEN',
    4,
    TRUE,
    TRUE,
    NULL,
    NULL,
    2,
    3,
    1,
    0,
    'GAMES_DIFFERENCE',
    NULL
FROM users creator
JOIN player_profiles creator_profile ON creator_profile.user_id = creator.id
JOIN clubs club ON club.name = 'Top Padel'
WHERE creator.email = 'nicolas.seed@sentimospadel.test'
  AND NOT EXISTS (
      SELECT 1
      FROM tournaments existing
      WHERE existing.name = 'Liga QA Abierta'
  );

INSERT INTO tournament_entries (
    tournament_id,
    player_profile_id,
    secondary_player_profile_id,
    created_by_player_id,
    team_name,
    entry_status,
    time_preferences_json
)
SELECT
    tournament.id,
    primary_profile.id,
    secondary_profile.id,
    creator_profile.id,
    'Lobos QA Open',
    'CONFIRMED',
    '["Noches"]'
FROM tournaments tournament
JOIN users creator ON creator.email = 'nicolas.seed@sentimospadel.test'
JOIN player_profiles creator_profile ON creator_profile.user_id = creator.id
JOIN users primary_user ON primary_user.email = 'nicolas.seed@sentimospadel.test'
JOIN player_profiles primary_profile ON primary_profile.user_id = primary_user.id
JOIN users secondary_user ON secondary_user.email = 'martin.seed@sentimospadel.test'
JOIN player_profiles secondary_profile ON secondary_profile.user_id = secondary_user.id
WHERE tournament.name = 'Liga QA Abierta'
  AND NOT EXISTS (
      SELECT 1
      FROM tournament_entries existing
      WHERE existing.tournament_id = tournament.id
  );

INSERT INTO tournaments (
    created_by_player_id,
    name,
    description,
    format,
    club_id,
    city,
    start_date,
    end_date,
    status,
    max_entries,
    open_enrollment,
    competitive,
    launched_at,
    available_courts,
    number_of_groups,
    league_rounds,
    points_for_win,
    points_for_tiebreak_loss,
    points_for_loss,
    standings_tiebreak,
    court_names_json
)
SELECT
    creator_profile.id,
    'Liga QA En Curso',
    'Seed QA para standings, partidos generados y flujo de resultados de liga',
    'LEAGUE',
    club.id,
    club.city,
    DATE '2026-03-01',
    DATE '2026-04-15',
    'IN_PROGRESS',
    4,
    FALSE,
    TRUE,
    CURRENT_TIMESTAMP,
    2,
    1,
    2,
    3,
    1,
    0,
    'GAMES_DIFFERENCE',
    '["Cancha 1","Cancha 2"]'
FROM users creator
JOIN player_profiles creator_profile ON creator_profile.user_id = creator.id
JOIN clubs club ON club.name = 'Top Padel'
WHERE creator.email = 'diego.seed@sentimospadel.test'
  AND NOT EXISTS (
      SELECT 1
      FROM tournaments existing
      WHERE existing.name = 'Liga QA En Curso'
  );

INSERT INTO tournament_entries (
    tournament_id,
    player_profile_id,
    secondary_player_profile_id,
    created_by_player_id,
    team_name,
    entry_status,
    time_preferences_json
)
SELECT
    tournament.id,
    primary_profile.id,
    secondary_profile.id,
    creator_profile.id,
    seed.team_name,
    'CONFIRMED',
    seed.time_preferences_json
FROM tournaments tournament
JOIN users creator ON creator.email = 'diego.seed@sentimospadel.test'
JOIN player_profiles creator_profile ON creator_profile.user_id = creator.id
JOIN (
    VALUES
        ('Lobos QA', 'nicolas.seed@sentimospadel.test', 'martin.seed@sentimospadel.test', '["Noches","Fin de semana"]'),
        ('Sur QA', 'ana.seed@sentimospadel.test', 'felipe.seed@sentimospadel.test', '["Noches"]'),
        ('Norte QA', 'diego.seed@sentimospadel.test', 'juan.seed@sentimospadel.test', '["Tarde"]'),
        ('Oeste QA', 'lucia.seed@sentimospadel.test', 'paula.seed@sentimospadel.test', '["Fin de semana"]')
) AS seed(team_name, primary_email, secondary_email, time_preferences_json) ON 1 = 1
JOIN users primary_user ON primary_user.email = seed.primary_email
JOIN player_profiles primary_profile ON primary_profile.user_id = primary_user.id
JOIN users secondary_user ON secondary_user.email = seed.secondary_email
JOIN player_profiles secondary_profile ON secondary_profile.user_id = secondary_user.id
WHERE tournament.name = 'Liga QA En Curso'
  AND NOT EXISTS (
      SELECT 1
      FROM tournament_entries existing
      WHERE existing.tournament_id = tournament.id
  );

INSERT INTO tournament_matches (
    tournament_id,
    team_one_entry_id,
    team_two_entry_id,
    phase,
    status,
    round_number,
    leg_number,
    round_label,
    scheduled_at,
    court_name
)
SELECT
    tournament.id,
    team_one.id,
    team_two.id,
    'LEAGUE_STAGE',
    seed.status,
    seed.round_number,
    seed.leg_number,
    seed.round_label,
    seed.scheduled_at,
    seed.court_name
FROM tournaments tournament
JOIN (
    VALUES
        ('QA Jornada 1A', 'Lobos QA', 'Sur QA', 'COMPLETED', 1, 1, TIMESTAMP WITH TIME ZONE '2026-03-20 18:00:00+00:00', 'Cancha 1'),
        ('QA Jornada 1B', 'Norte QA', 'Oeste QA', 'RESULT_PENDING', 1, 1, TIMESTAMP WITH TIME ZONE '2026-03-20 18:00:00+00:00', 'Cancha 2'),
        ('QA Jornada 2A', 'Lobos QA', 'Norte QA', 'SCHEDULED', 2, 1, TIMESTAMP WITH TIME ZONE '2026-03-22 19:30:00+00:00', 'Cancha 1'),
        ('QA Jornada 2B', 'Sur QA', 'Oeste QA', 'SCHEDULED', 2, 1, TIMESTAMP WITH TIME ZONE '2026-03-29 19:30:00+00:00', 'Cancha 2')
) AS seed(round_label, team_one_name, team_two_name, status, round_number, leg_number, scheduled_at, court_name) ON 1 = 1
JOIN tournament_entries team_one ON team_one.tournament_id = tournament.id AND team_one.team_name = seed.team_one_name
JOIN tournament_entries team_two ON team_two.tournament_id = tournament.id AND team_two.team_name = seed.team_two_name
WHERE tournament.name = 'Liga QA En Curso'
  AND NOT EXISTS (
      SELECT 1
      FROM tournament_matches existing
      WHERE existing.tournament_id = tournament.id
        AND existing.round_label = seed.round_label
  );

INSERT INTO tournament_match_results (
    tournament_match_id,
    submitted_by_player_id,
    status,
    winner_team,
    set_one_team_one_games,
    set_one_team_two_games,
    set_two_team_one_games,
    set_two_team_two_games,
    submitted_at,
    confirmed_by_player_id,
    confirmed_at
)
SELECT
    match.id,
    submitted_by_profile.id,
    'CONFIRMED',
    'TEAM_ONE',
    6,
    4,
    6,
    3,
    CURRENT_TIMESTAMP,
    confirmed_by_profile.id,
    CURRENT_TIMESTAMP
FROM tournament_matches match
JOIN tournaments tournament ON tournament.id = match.tournament_id
JOIN users submitted_by_user ON submitted_by_user.email = 'nicolas.seed@sentimospadel.test'
JOIN player_profiles submitted_by_profile ON submitted_by_profile.user_id = submitted_by_user.id
JOIN users confirmed_by_user ON confirmed_by_user.email = 'ana.seed@sentimospadel.test'
JOIN player_profiles confirmed_by_profile ON confirmed_by_profile.user_id = confirmed_by_user.id
WHERE tournament.name = 'Liga QA En Curso'
  AND match.round_label = 'QA Jornada 1A'
  AND NOT EXISTS (
      SELECT 1
      FROM tournament_match_results existing
      WHERE existing.tournament_match_id = match.id
  );

INSERT INTO tournament_match_results (
    tournament_match_id,
    submitted_by_player_id,
    status,
    winner_team,
    set_one_team_one_games,
    set_one_team_two_games,
    set_two_team_one_games,
    set_two_team_two_games,
    submitted_at
)
SELECT
    match.id,
    submitted_by_profile.id,
    'PENDING',
    'TEAM_ONE',
    6,
    4,
    6,
    4,
    CURRENT_TIMESTAMP
FROM tournament_matches match
JOIN tournaments tournament ON tournament.id = match.tournament_id
JOIN users submitted_by_user ON submitted_by_user.email = 'diego.seed@sentimospadel.test'
JOIN player_profiles submitted_by_profile ON submitted_by_profile.user_id = submitted_by_user.id
WHERE tournament.name = 'Liga QA En Curso'
  AND match.round_label = 'QA Jornada 1B'
  AND NOT EXISTS (
      SELECT 1
      FROM tournament_match_results existing
      WHERE existing.tournament_match_id = match.id
  );
