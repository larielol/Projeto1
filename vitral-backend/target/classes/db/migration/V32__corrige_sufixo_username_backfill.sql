-- V31 sufixava todo username herdado do "name" com ".<id>" para garantir unicidade no backfill,
-- mesmo quando o valor sem sufixo ja era unico. Isso impedia o login com o username original
-- (ex: "estante" ficou "estante.4"). Aqui removemos o sufixo apenas quando ele foi gerado
-- exatamente pela regra do V31 e a remocao nao colide com outro username existente.
UPDATE account a
SET username = lower(regexp_replace(a.name, '[^a-zA-Z0-9]+', '.', 'g'))
WHERE a.username = lower(regexp_replace(a.name, '[^a-zA-Z0-9]+', '.', 'g')) || '.' || a.id
  AND NOT EXISTS (
      SELECT 1 FROM account b
      WHERE b.id <> a.id
        AND b.username = lower(regexp_replace(a.name, '[^a-zA-Z0-9]+', '.', 'g'))
  );
