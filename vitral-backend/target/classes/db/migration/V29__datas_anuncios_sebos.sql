ALTER TABLE sebo
ADD COLUMN IF NOT EXISTS data_criacao TIMESTAMPTZ;

ALTER TABLE sebo
ADD COLUMN IF NOT EXISTS ultima_atividade TIMESTAMPTZ;

UPDATE sebo
SET data_criacao = created_at
WHERE data_criacao IS NULL
  AND created_at IS NOT NULL;

UPDATE sebo
SET ultima_atividade = created_at
WHERE ultima_atividade IS NULL
  AND created_at IS NOT NULL;
