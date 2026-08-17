CREATE TABLE produto (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    sebo_id     BIGINT          NOT NULL,
    titulo      VARCHAR(255)    NOT NULL,
    autor       VARCHAR(255),
    descricao   VARCHAR(2000),
    preco       NUMERIC(12, 2)  NOT NULL,
    condicao    VARCHAR(20)     NOT NULL,
    foto_url    VARCHAR(500),
    ativo       BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ     NOT NULL,
    updated_at  TIMESTAMPTZ     NOT NULL,
    CONSTRAINT fk_produto_sebo FOREIGN KEY (sebo_id) REFERENCES sebo (id) ON DELETE CASCADE,
    CONSTRAINT ck_produto_condicao CHECK (condicao IN ('NOVO', 'USADO', 'SEMINOVO')),
    CONSTRAINT ck_produto_preco CHECK (preco >= 0)
);

CREATE INDEX ix_produto_sebo ON produto (sebo_id);
CREATE INDEX ix_produto_titulo ON produto (titulo);
