CREATE TABLE coaches (
    id BIGSERIAL PRIMARY KEY,
    full_name VARCHAR(160) NOT NULL,
    club_name VARCHAR(160) NOT NULL,
    current_rating NUMERIC(4,2) NOT NULL,
    reviews_count INTEGER NOT NULL,
    average_rating NUMERIC(3,2) NOT NULL,
    hourly_rate_uyu INTEGER NOT NULL,
    phone VARCHAR(40) NOT NULL,
    photo_url VARCHAR(500),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO coaches (
    full_name,
    club_name,
    current_rating,
    reviews_count,
    average_rating,
    hourly_rate_uyu,
    phone,
    photo_url,
    is_active
)
SELECT
    'Tomi',
    'Club Reducto',
    5.30,
    70,
    4.80,
    1000,
    '098388097',
    'https://picsum.photos/100/100?r=tomi',
    TRUE
WHERE NOT EXISTS (
    SELECT 1 FROM coaches WHERE full_name = 'Tomi' AND phone = '098388097'
);

INSERT INTO coaches (
    full_name,
    club_name,
    current_rating,
    reviews_count,
    average_rating,
    hourly_rate_uyu,
    phone,
    photo_url,
    is_active
)
SELECT
    'Nico',
    'El Bosque Padel',
    5.50,
    120,
    4.90,
    1500,
    '091987654',
    'https://picsum.photos/100/100?r=nico',
    TRUE
WHERE NOT EXISTS (
    SELECT 1 FROM coaches WHERE full_name = 'Nico' AND phone = '091987654'
);

INSERT INTO coaches (
    full_name,
    club_name,
    current_rating,
    reviews_count,
    average_rating,
    hourly_rate_uyu,
    phone,
    photo_url,
    is_active
)
SELECT
    'Matias',
    'Padel Pro',
    5.10,
    45,
    4.60,
    1200,
    '099123456',
    'https://picsum.photos/100/100?r=matias',
    TRUE
WHERE NOT EXISTS (
    SELECT 1 FROM coaches WHERE full_name = 'Matias' AND phone = '099123456'
);
