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
    points_for_win,
    points_for_tiebreak_loss,
    points_for_loss,
    standings_tiebreak,
    court_names_json
)
SELECT
    creator_profile.id,
    'Eliminatoria QA Abierta',
    'Seed QA para probar explorer y join/leave de eliminatoria',
    'ELIMINATION',
    club.id,
    club.city,
    DATE '2026-04-06',
    DATE '2026-04-20',
    'OPEN',
    4,
    TRUE,
    TRUE,
    2,
    0,
    0,
    'GAMES_DIFFERENCE',
    '["Cancha 1","Cancha 2"]'
FROM users creator
JOIN player_profiles creator_profile ON creator_profile.user_id = creator.id
JOIN clubs club ON club.name = 'Top Padel'
WHERE creator.email = 'nicolas.seed@sentimospadel.test'
  AND NOT EXISTS (
      SELECT 1
      FROM tournaments existing
      WHERE existing.name = 'Eliminatoria QA Abierta'
  );

INSERT INTO tournament_entries (
    tournament_id,
    player_profile_id,
    secondary_player_profile_id,
    created_by_player_id,
    team_name,
    entry_status,
    time_preferences_json,
    group_label
)
SELECT
    tournament.id,
    primary_profile.id,
    secondary_profile.id,
    creator_profile.id,
    'Lobos KO Open',
    'CONFIRMED',
    '["Noches"]',
    NULL
FROM tournaments tournament
JOIN users creator ON creator.email = 'nicolas.seed@sentimospadel.test'
JOIN player_profiles creator_profile ON creator_profile.user_id = creator.id
JOIN users primary_user ON primary_user.email = 'nicolas.seed@sentimospadel.test'
JOIN player_profiles primary_profile ON primary_profile.user_id = primary_user.id
JOIN users secondary_user ON secondary_user.email = 'martin.seed@sentimospadel.test'
JOIN player_profiles secondary_profile ON secondary_profile.user_id = secondary_user.id
WHERE tournament.name = 'Eliminatoria QA Abierta'
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
    'Eliminatoria QA En Curso',
    'Seed QA para grupos, playoffs y flujo de resultados de eliminatoria',
    'ELIMINATION',
    club.id,
    club.city,
    DATE '2026-03-20',
    DATE '2026-04-10',
    'IN_PROGRESS',
    4,
    FALSE,
    TRUE,
    CURRENT_TIMESTAMP,
    2,
    2,
    1,
    2,
    0,
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
      WHERE existing.name = 'Eliminatoria QA En Curso'
  );

INSERT INTO tournament_entries (
    tournament_id,
    player_profile_id,
    secondary_player_profile_id,
    created_by_player_id,
    team_name,
    entry_status,
    time_preferences_json,
    group_label
)
SELECT
    tournament.id,
    primary_profile.id,
    secondary_profile.id,
    creator_profile.id,
    seed.team_name,
    'CONFIRMED',
    seed.time_preferences_json,
    seed.group_label
FROM tournaments tournament
JOIN users creator ON creator.email = 'diego.seed@sentimospadel.test'
JOIN player_profiles creator_profile ON creator_profile.user_id = creator.id
JOIN (
    VALUES
        ('Lobos KO', 'nicolas.seed@sentimospadel.test', 'martin.seed@sentimospadel.test', '["Noches","Fin de semana"]', 'Grupo A'),
        ('Sur KO', 'ana.seed@sentimospadel.test', 'felipe.seed@sentimospadel.test', '["Noches"]', 'Grupo A'),
        ('Norte KO', 'diego.seed@sentimospadel.test', 'juan.seed@sentimospadel.test', '["Tarde"]', 'Grupo B'),
        ('Oeste KO', 'lucia.seed@sentimospadel.test', 'paula.seed@sentimospadel.test', '["Fin de semana"]', 'Grupo B')
) AS seed(team_name, primary_email, secondary_email, time_preferences_json, group_label) ON 1 = 1
JOIN users primary_user ON primary_user.email = seed.primary_email
JOIN player_profiles primary_profile ON primary_profile.user_id = primary_user.id
JOIN users secondary_user ON secondary_user.email = seed.secondary_email
JOIN player_profiles secondary_profile ON secondary_profile.user_id = secondary_user.id
WHERE tournament.name = 'Eliminatoria QA En Curso'
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
    'GROUP_STAGE',
    seed.status,
    seed.round_number,
    NULL,
    seed.round_label,
    seed.scheduled_at,
    seed.court_name
FROM tournaments tournament
JOIN (
    VALUES
        ('Fase de Grupos - Grupo A', 'Lobos KO', 'Sur KO', 'COMPLETED', 1, TIMESTAMP WITH TIME ZONE '2026-03-24 18:00:00+00:00', 'Cancha 1'),
        ('Fase de Grupos - Grupo B', 'Norte KO', 'Oeste KO', 'RESULT_PENDING', 2, TIMESTAMP WITH TIME ZONE '2026-03-25 18:00:00+00:00', 'Cancha 2')
) AS seed(round_label, team_one_name, team_two_name, status, round_number, scheduled_at, court_name) ON 1 = 1
JOIN tournament_entries team_one ON team_one.tournament_id = tournament.id AND team_one.team_name = seed.team_one_name
JOIN tournament_entries team_two ON team_two.tournament_id = tournament.id AND team_two.team_name = seed.team_two_name
WHERE tournament.name = 'Eliminatoria QA En Curso'
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
FROM tournaments tournament
JOIN tournament_matches match ON match.tournament_id = tournament.id
JOIN users submitted_by_user ON submitted_by_user.email = 'nicolas.seed@sentimospadel.test'
JOIN player_profiles submitted_by_profile ON submitted_by_profile.user_id = submitted_by_user.id
JOIN users confirmed_by_user ON confirmed_by_user.email = 'felipe.seed@sentimospadel.test'
JOIN player_profiles confirmed_by_profile ON confirmed_by_profile.user_id = confirmed_by_user.id
WHERE tournament.name = 'Eliminatoria QA En Curso'
  AND match.round_label = 'Fase de Grupos - Grupo A'
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
FROM tournaments tournament
JOIN tournament_matches match ON match.tournament_id = tournament.id
JOIN users submitted_by_user ON submitted_by_user.email = 'diego.seed@sentimospadel.test'
JOIN player_profiles submitted_by_profile ON submitted_by_profile.user_id = submitted_by_user.id
WHERE tournament.name = 'Eliminatoria QA En Curso'
  AND match.round_label = 'Fase de Grupos - Grupo B'
  AND NOT EXISTS (
      SELECT 1
      FROM tournament_match_results existing
      WHERE existing.tournament_match_id = match.id
  );
