-- Phone becomes optional at registration; users now provide it during the onboarding flow
-- (after email verification). The unique constraint is kept so two users can't share a phone,
-- and PostgreSQL allows multiple NULLs in a UNIQUE column.
ALTER TABLE users ALTER COLUMN phone DROP NOT NULL;
