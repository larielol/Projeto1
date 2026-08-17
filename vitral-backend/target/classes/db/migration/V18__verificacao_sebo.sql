ALTER TABLE account DROP CONSTRAINT ck_account_type;
ALTER TABLE account ADD CONSTRAINT ck_account_type CHECK (type IN ('SEBO', 'USUARIO', 'ADMIN'));

ALTER TABLE sebo ADD COLUMN cnpj VARCHAR(14);
ALTER TABLE sebo ADD COLUMN status_verificacao VARCHAR(20) NOT NULL DEFAULT 'VERIFICADO';
ALTER TABLE sebo ADD COLUMN verificado_em TIMESTAMPTZ;

ALTER TABLE sebo ADD CONSTRAINT uk_sebo_cnpj UNIQUE (cnpj);
ALTER TABLE sebo ADD CONSTRAINT ck_sebo_status_verificacao
    CHECK (status_verificacao IN ('PENDENTE', 'VERIFICADO', 'REJEITADO'));

UPDATE sebo SET verificado_em = CURRENT_TIMESTAMP WHERE status_verificacao = 'VERIFICADO';
CREATE INDEX ix_sebo_status_verificacao ON sebo (status_verificacao);
