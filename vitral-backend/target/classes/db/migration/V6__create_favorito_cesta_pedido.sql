-- ============================================================
-- Favoritos (faltava a tabela formal com FK corretas)
-- ============================================================
CREATE TABLE IF NOT EXISTS favoritos (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    account_id  BIGINT      NOT NULL,
    produto_id  BIGINT      NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL,
    updated_at  TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_favorito UNIQUE (account_id, produto_id),
    CONSTRAINT fk_favorito_account FOREIGN KEY (account_id) REFERENCES account (id) ON DELETE CASCADE,
    CONSTRAINT fk_favorito_produto FOREIGN KEY (produto_id) REFERENCES produto (id) ON DELETE CASCADE
);

CREATE INDEX ix_favorito_account ON favoritos (account_id);

-- ============================================================
-- Cesta de compras
-- ============================================================
CREATE TABLE cesta_item (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    account_id  BIGINT      NOT NULL,
    produto_id  BIGINT      NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL,
    updated_at  TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_cesta_item UNIQUE (account_id, produto_id),
    CONSTRAINT fk_cesta_account FOREIGN KEY (account_id) REFERENCES account (id) ON DELETE CASCADE,
    CONSTRAINT fk_cesta_produto FOREIGN KEY (produto_id) REFERENCES produto (id) ON DELETE CASCADE
);

CREATE INDEX ix_cesta_account ON cesta_item (account_id);

-- ============================================================
-- Pedidos
-- ============================================================
CREATE TABLE pedido (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    account_id  BIGINT          NOT NULL,
    sebo_id     BIGINT          NOT NULL,
    status      VARCHAR(30)     NOT NULL DEFAULT 'AGUARDANDO_CONFIRMACAO',
    total       NUMERIC(12, 2)  NOT NULL,
    created_at  TIMESTAMPTZ     NOT NULL,
    updated_at  TIMESTAMPTZ     NOT NULL,
    CONSTRAINT fk_pedido_account FOREIGN KEY (account_id) REFERENCES account (id),
    CONSTRAINT fk_pedido_sebo    FOREIGN KEY (sebo_id)    REFERENCES sebo (id),
    CONSTRAINT ck_pedido_status  CHECK (status IN (
        'AGUARDANDO_CONFIRMACAO',
        'CONFIRMADO',
        'CANCELADO'
    )),
    CONSTRAINT ck_pedido_total CHECK (total >= 0)
);

CREATE INDEX ix_pedido_account ON pedido (account_id);
CREATE INDEX ix_pedido_sebo    ON pedido (sebo_id);

-- ============================================================
-- Itens do pedido (snapshot do produto no momento da compra)
-- ============================================================
CREATE TABLE pedido_item (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    pedido_id       BIGINT          NOT NULL,
    produto_id      BIGINT          NOT NULL,
    titulo_snapshot VARCHAR(255)    NOT NULL,
    preco_snapshot  NUMERIC(12, 2)  NOT NULL,
    created_at      TIMESTAMPTZ     NOT NULL,
    updated_at      TIMESTAMPTZ     NOT NULL,
    CONSTRAINT fk_pedido_item_pedido  FOREIGN KEY (pedido_id)  REFERENCES pedido (id) ON DELETE CASCADE,
    CONSTRAINT fk_pedido_item_produto FOREIGN KEY (produto_id) REFERENCES produto (id)
);

CREATE INDEX ix_pedido_item_pedido ON pedido_item (pedido_id);
