INSERT INTO clubs (name, city, address, description, is_integrated)
SELECT 'Top Padel', 'Montevideo', 'Av. Rivera 6000', NULL, TRUE
WHERE NOT EXISTS (
    SELECT 1
    FROM clubs
    WHERE LOWER(name) = LOWER('Top Padel')
      AND COALESCE(LOWER(address), '') = LOWER('Av. Rivera 6000')
);

INSERT INTO clubs (name, city, address, description, is_integrated)
SELECT 'World Padel', 'Montevideo', 'Ellauri 350', NULL, TRUE
WHERE NOT EXISTS (
    SELECT 1
    FROM clubs
    WHERE LOWER(name) = LOWER('World Padel')
      AND COALESCE(LOWER(address), '') = LOWER('Ellauri 350')
);

INSERT INTO clubs (name, city, address, description, is_integrated)
SELECT 'Cordon Padel', 'Montevideo', 'Galicia 1234', NULL, FALSE
WHERE NOT EXISTS (
    SELECT 1
    FROM clubs
    WHERE LOWER(name) = LOWER('Cordon Padel')
      AND COALESCE(LOWER(address), '') = LOWER('Galicia 1234')
);

INSERT INTO clubs (name, city, address, description, is_integrated)
SELECT 'Boss', 'Montevideo', 'Av. Brasil 2000', NULL, FALSE
WHERE NOT EXISTS (
    SELECT 1
    FROM clubs
    WHERE LOWER(name) = LOWER('Boss')
      AND COALESCE(LOWER(address), '') = LOWER('Av. Brasil 2000')
);

INSERT INTO clubs (name, city, address, description, is_integrated)
SELECT 'Reducto', 'Montevideo', 'San Martín 2500', NULL, FALSE
WHERE NOT EXISTS (
    SELECT 1
    FROM clubs
    WHERE LOWER(name) = LOWER('Reducto')
      AND COALESCE(LOWER(address), '') = LOWER('San Martín 2500')
);
