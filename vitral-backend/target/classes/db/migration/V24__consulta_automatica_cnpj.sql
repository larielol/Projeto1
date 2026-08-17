ALTER TABLE sebo ADD COLUMN razao_social_receita VARCHAR(255);
ALTER TABLE sebo ADD COLUMN status_consulta_cnpj VARCHAR(20) NOT NULL DEFAULT 'NAO_CONSULTADO';
ALTER TABLE sebo ADD COLUMN cnpj_consultado_em TIMESTAMPTZ;
ALTER TABLE sebo ADD COLUMN mensagem_consulta_cnpj VARCHAR(500);
ALTER TABLE sebo ADD CONSTRAINT ck_sebo_status_consulta_cnpj
    CHECK (status_consulta_cnpj IN ('NAO_CONSULTADO','ATIVA','INATIVA','INDISPONIVEL'));
CREATE INDEX ix_sebo_status_consulta_cnpj ON sebo(status_consulta_cnpj);
