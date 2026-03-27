ALTER TABLE users
    ADD COLUMN managed_club_id BIGINT;

ALTER TABLE users
    ADD CONSTRAINT fk_users_managed_club
        FOREIGN KEY (managed_club_id) REFERENCES clubs (id);

CREATE TABLE club_courts (
    id BIGSERIAL PRIMARY KEY,
    club_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    display_order INTEGER NOT NULL,
    hourly_rate_uyu NUMERIC(10, 2) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_club_courts_club
        FOREIGN KEY (club_id) REFERENCES clubs (id)
);

CREATE UNIQUE INDEX ux_club_courts_club_name
    ON club_courts (club_id, name);

CREATE TABLE club_agenda_slot_overrides (
    id BIGSERIAL PRIMARY KEY,
    club_id BIGINT NOT NULL,
    court_id BIGINT NOT NULL,
    slot_date DATE NOT NULL,
    start_time TIME NOT NULL,
    status VARCHAR(20) NOT NULL,
    reserved_by_name VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_club_agenda_slot_overrides_club
        FOREIGN KEY (club_id) REFERENCES clubs (id),
    CONSTRAINT fk_club_agenda_slot_overrides_court
        FOREIGN KEY (court_id) REFERENCES club_courts (id)
);

CREATE UNIQUE INDEX ux_club_agenda_slot_overrides_court_slot
    ON club_agenda_slot_overrides (court_id, slot_date, start_time);

CREATE TABLE club_activity_logs (
    id BIGSERIAL PRIMARY KEY,
    club_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    description VARCHAR(500) NOT NULL,
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_club_activity_logs_club
        FOREIGN KEY (club_id) REFERENCES clubs (id)
);

INSERT INTO club_courts (club_id, name, display_order, hourly_rate_uyu, active)
SELECT club.id, seed.name, seed.display_order, seed.hourly_rate_uyu, TRUE
FROM clubs club
JOIN (
    SELECT 'Top Padel' AS club_name, 'Cancha 1 (Cristal)' AS name, 1 AS display_order, 1200.00 AS hourly_rate_uyu
    UNION ALL
    SELECT 'Top Padel', 'Cancha 2 (Cristal)', 2, 1200.00
    UNION ALL
    SELECT 'Top Padel', 'Cancha 3 (Muro)', 3, 1100.00
    UNION ALL
    SELECT 'World Padel', 'Cancha 1', 1, 1250.00
    UNION ALL
    SELECT 'World Padel', 'Cancha 2', 2, 1250.00
    UNION ALL
    SELECT 'World Padel', 'Cancha 3', 3, 1200.00
    UNION ALL
    SELECT 'Cordon Padel', 'Cancha 1', 1, 950.00
    UNION ALL
    SELECT 'Cordon Padel', 'Cancha 2', 2, 950.00
    UNION ALL
    SELECT 'Boss', 'Cancha 1', 1, 1050.00
    UNION ALL
    SELECT 'Boss', 'Cancha 2', 2, 1050.00
    UNION ALL
    SELECT 'Reducto', 'Cancha 1', 1, 900.00
    UNION ALL
    SELECT 'Reducto', 'Cancha 2', 2, 900.00
) seed
    ON club.name = seed.club_name
WHERE NOT EXISTS (
    SELECT 1
    FROM club_courts existing
    WHERE existing.club_id = club.id
      AND existing.name = seed.name
);

INSERT INTO users (email, password_hash, role, status, managed_club_id)
SELECT 'club.admin@sentimospadel.test',
       '$2a$10$jdRNNvNGquYDiww0t4cYReOsxqAY8PxujaGzXtGQqAV16H8xcP3TW',
       'ADMIN',
       'ACTIVE',
       club.id
FROM clubs club
WHERE club.name = 'Top Padel'
  AND NOT EXISTS (
      SELECT 1
      FROM users existing
      WHERE existing.email = 'club.admin@sentimospadel.test'
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
SELECT user_seed.id,
       'Top Padel Admin',
       'Montevideo',
       4.60,
       TRUE,
       0,
       0,
       TRUE,
       CURRENT_TIMESTAMP,
       4.60,
       'CUARTA',
       FALSE,
       'NOT_REQUIRED'
FROM users user_seed
WHERE user_seed.email = 'club.admin@sentimospadel.test'
  AND NOT EXISTS (
      SELECT 1
      FROM player_profiles existing
      WHERE existing.user_id = user_seed.id
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
SELECT profile.id,
       1,
       'C',
       'C',
       'C',
       'C',
       'C',
       'C',
       'C',
       'C',
       'C',
       'C',
       30,
       60.00,
       4.60,
       'CUARTA',
       FALSE
FROM player_profiles profile
JOIN users user_seed ON user_seed.id = profile.user_id
WHERE user_seed.email = 'club.admin@sentimospadel.test'
  AND NOT EXISTS (
      SELECT 1
      FROM initial_survey_submissions existing
      WHERE existing.player_id = profile.id
  );

INSERT INTO club_agenda_slot_overrides (club_id, court_id, slot_date, start_time, status, reserved_by_name)
SELECT club.id, court.id, CURRENT_DATE, TIME '19:00:00', 'RESERVED', 'Reserva Premium'
FROM clubs club
JOIN club_courts court ON court.club_id = club.id
WHERE club.name = 'Top Padel'
  AND court.name = 'Cancha 1 (Cristal)'
  AND NOT EXISTS (
      SELECT 1
      FROM club_agenda_slot_overrides existing
      WHERE existing.court_id = court.id
        AND existing.slot_date = CURRENT_DATE
        AND existing.start_time = TIME '19:00:00'
  );

INSERT INTO club_agenda_slot_overrides (club_id, court_id, slot_date, start_time, status, reserved_by_name)
SELECT club.id, court.id, CURRENT_DATE, TIME '22:00:00', 'BLOCKED', NULL
FROM clubs club
JOIN club_courts court ON court.club_id = club.id
WHERE club.name = 'Top Padel'
  AND court.name = 'Cancha 2 (Cristal)'
  AND NOT EXISTS (
      SELECT 1
      FROM club_agenda_slot_overrides existing
      WHERE existing.court_id = court.id
        AND existing.slot_date = CURRENT_DATE
        AND existing.start_time = TIME '22:00:00'
  );

INSERT INTO club_activity_logs (club_id, title, description, occurred_at)
SELECT club.id, 'Reserva confirmada', 'Cancha 1 (Cristal) • 19:00 • Reserva Premium', CURRENT_TIMESTAMP
FROM clubs club
WHERE club.name = 'Top Padel'
  AND NOT EXISTS (
      SELECT 1
      FROM club_activity_logs existing
      WHERE existing.club_id = club.id
        AND existing.title = 'Reserva confirmada'
        AND existing.description = 'Cancha 1 (Cristal) • 19:00 • Reserva Premium'
  );

INSERT INTO club_activity_logs (club_id, title, description, occurred_at)
SELECT club.id, 'Resultado validado', 'Liga QA En Curso • Jornada backend confirmada', CURRENT_TIMESTAMP
FROM clubs club
WHERE club.name = 'Top Padel'
  AND NOT EXISTS (
      SELECT 1
      FROM club_activity_logs existing
      WHERE existing.club_id = club.id
        AND existing.title = 'Resultado validado'
        AND existing.description = 'Liga QA En Curso • Jornada backend confirmada'
  );
