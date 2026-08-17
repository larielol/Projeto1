CREATE TABLE suporte (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    assunto      VARCHAR(255)  NOT NULL,
    mensagem     VARCHAR(2000) NOT NULL,
    remetente_id BIGINT        NOT NULL,
    status       VARCHAR(20)   NOT NULL DEFAULT 'ABERTO',
    created_at   TIMESTAMPTZ   NOT NULL,
    updated_at   TIMESTAMPTZ   NOT NULL,
    CONSTRAINT fk_suporte_remetente FOREIGN KEY (remetente_id) REFERENCES account (id) ON DELETE CASCADE,
    CONSTRAINT ck_suporte_status CHECK (status IN ('ABERTO', 'EM_ANALISE', 'RESOLVIDO'))
);

CREATE INDEX ix_suporte_remetente ON suporte (remetente_id);
CREATE INDEX ix_suporte_status ON suporte (status);
