CREATE TABLE oferta (
    id                 BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    produto_id         BIGINT         NOT NULL,
    preco_promocional  NUMERIC(12, 2) NOT NULL,
    descricao          VARCHAR(500),
    inicio_em          TIMESTAMPTZ,
    fim_em             TIMESTAMPTZ,
    ativa              BOOLEAN        NOT NULL DEFAULT TRUE,
    created_at         TIMESTAMPTZ    NOT NULL,
    updated_at         TIMESTAMPTZ    NOT NULL,
    CONSTRAINT fk_oferta_produto FOREIGN KEY (produto_id) REFERENCES produto (id) ON DELETE CASCADE,
    CONSTRAINT ck_oferta_preco CHECK (preco_promocional > 0),
    CONSTRAINT ck_oferta_periodo CHECK (fim_em IS NULL OR inicio_em IS NULL OR fim_em >= inicio_em)
);

CREATE INDEX ix_oferta_produto ON oferta (produto_id);
CREATE INDEX ix_oferta_ativa_periodo ON oferta (ativa, inicio_em, fim_em);
