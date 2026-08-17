CREATE TABLE mensagem (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    remetente_id    BIGINT        NOT NULL,
    destinatario_id BIGINT        NOT NULL,
    conteudo        VARCHAR(2000) NOT NULL,
    lida            BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ   NOT NULL,
    updated_at      TIMESTAMPTZ   NOT NULL,
    CONSTRAINT fk_mensagem_remetente FOREIGN KEY (remetente_id) REFERENCES account (id) ON DELETE CASCADE,
    CONSTRAINT fk_mensagem_destinatario FOREIGN KEY (destinatario_id) REFERENCES account (id) ON DELETE CASCADE,
    CONSTRAINT ck_mensagem_destinatario CHECK (remetente_id <> destinatario_id)
);

CREATE INDEX ix_mensagem_remetente ON mensagem (remetente_id);
CREATE INDEX ix_mensagem_destinatario ON mensagem (destinatario_id);
CREATE INDEX ix_mensagem_conversa ON mensagem (remetente_id, destinatario_id, created_at);
