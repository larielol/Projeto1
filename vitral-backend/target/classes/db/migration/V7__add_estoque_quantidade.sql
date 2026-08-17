ALTER TABLE produto
    ADD COLUMN estoque INTEGER NOT NULL DEFAULT 1;

ALTER TABLE produto
    ADD CONSTRAINT ck_produto_estoque CHECK (estoque >= 0);

ALTER TABLE cesta_item
    ADD COLUMN quantidade INTEGER NOT NULL DEFAULT 1;

ALTER TABLE cesta_item
    ADD CONSTRAINT ck_cesta_item_quantidade CHECK (quantidade > 0);

ALTER TABLE pedido_item
    ADD COLUMN quantidade INTEGER NOT NULL DEFAULT 1;

ALTER TABLE pedido_item
    ADD CONSTRAINT ck_pedido_item_quantidade CHECK (quantidade > 0);
