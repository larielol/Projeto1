ALTER TABLE account
    ADD COLUMN IF NOT EXISTS username VARCHAR(50);

-- Backfill: gera um username unico a partir do nome existente para contas ja cadastradas
-- (antes deste campo existir, o proprio "name" era usado como identificador de login).
UPDATE account
SET username = lower(regexp_replace(name, '[^a-zA-Z0-9]+', '.', 'g')) || '.' || id
WHERE username IS NULL;

ALTER TABLE account
    ALTER COLUMN username SET NOT NULL;

ALTER TABLE account
    ADD CONSTRAINT uk_account_username UNIQUE (username);
