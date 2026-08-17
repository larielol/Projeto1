CREATE TABLE movimentacao_estoque (
    id                    BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    produto_id            BIGINT NOT NULL,
    sebo_id               BIGINT NOT NULL,
    operador_id           BIGINT NOT NULL,
    movimentacao_origem_id BIGINT,
    tipo                  VARCHAR(20) NOT NULL,
    quantidade            INTEGER NOT NULL,
    estoque_antes         INTEGER NOT NULL,
    estoque_depois        INTEGER NOT NULL,
    valor_unitario        NUMERIC(12, 2),
    valor_total           NUMERIC(12, 2),
    observacao            VARCHAR(500),
    created_at            TIMESTAMPTZ NOT NULL,
    updated_at            TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_mov_estoque_produto FOREIGN KEY (produto_id) REFERENCES produto (id),
    CONSTRAINT fk_mov_estoque_sebo FOREIGN KEY (sebo_id) REFERENCES sebo (id),
    CONSTRAINT fk_mov_estoque_operador FOREIGN KEY (operador_id) REFERENCES account (id),
    CONSTRAINT fk_mov_estoque_origem FOREIGN KEY (movimentacao_origem_id) REFERENCES movimentacao_estoque (id),
    CONSTRAINT ck_mov_estoque_tipo CHECK (tipo IN ('ENTRADA','SAIDA','VENDA_FISICA','VENDA_ONLINE','ESTORNO','AJUSTE')),
    CONSTRAINT ck_mov_estoque_quantidade CHECK (quantidade > 0),
    CONSTRAINT ck_mov_estoque_saldos CHECK (estoque_antes >= 0 AND estoque_depois >= 0)
);

CREATE INDEX ix_mov_estoque_produto ON movimentacao_estoque (produto_id, created_at);
CREATE INDEX ix_mov_estoque_sebo ON movimentacao_estoque (sebo_id, created_at);
CREATE INDEX ix_mov_estoque_origem ON movimentacao_estoque (movimentacao_origem_id);
