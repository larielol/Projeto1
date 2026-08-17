ALTER TABLE sebo ADD COLUMN motivo_rejeicao VARCHAR(500);

CREATE TABLE documento_verificacao_sebo (
    id BIGSERIAL PRIMARY KEY,
    sebo_id BIGINT NOT NULL REFERENCES sebo(id),
    tipo_documento VARCHAR(30) NOT NULL,
    arquivo_url VARCHAR(500) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDENTE',
    enviado_em TIMESTAMPTZ NOT NULL,
    analisado_em TIMESTAMPTZ,
    analisado_por BIGINT REFERENCES account(id),
    motivo_rejeicao VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_documento_sebo_tipo CHECK (tipo_documento IN ('CARTAO_CNPJ','CONTRATO_SOCIAL','DOCUMENTO_RESPONSAVEL','COMPROVANTE_BANCARIO','COMPROVANTE_ATIVIDADE','OUTRO')),
    CONSTRAINT ck_documento_sebo_status CHECK (status IN ('PENDENTE','APROVADO','REJEITADO'))
);

CREATE TABLE auditoria_verificacao_sebo (
    id BIGSERIAL PRIMARY KEY,
    sebo_id BIGINT NOT NULL REFERENCES sebo(id),
    analisado_por BIGINT NOT NULL REFERENCES account(id),
    status_anterior VARCHAR(20) NOT NULL,
    novo_status VARCHAR(20) NOT NULL,
    motivo VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_auditoria_status_anterior CHECK (status_anterior IN ('PENDENTE','VERIFICADO','REJEITADO')),
    CONSTRAINT ck_auditoria_novo_status CHECK (novo_status IN ('PENDENTE','VERIFICADO','REJEITADO'))
);

CREATE INDEX ix_documento_verificacao_sebo ON documento_verificacao_sebo(sebo_id, enviado_em DESC);
CREATE INDEX ix_auditoria_verificacao_sebo ON auditoria_verificacao_sebo(sebo_id, created_at DESC);
