ALTER TABLE clubs
    ADD COLUMN booking_mode VARCHAR(40);

UPDATE clubs
SET booking_mode = CASE
    WHEN is_integrated THEN 'DIRECT'
    ELSE 'UNAVAILABLE'
END
WHERE booking_mode IS NULL;

UPDATE clubs
SET booking_mode = 'CONFIRMATION_REQUIRED'
WHERE LOWER(name) = LOWER('World Padel');

ALTER TABLE clubs
    ALTER COLUMN booking_mode SET NOT NULL;
