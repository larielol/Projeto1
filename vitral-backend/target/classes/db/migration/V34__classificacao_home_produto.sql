ALTER TABLE produto
    ADD COLUMN classico BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN lancamento BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX ix_produto_home_classico ON produto (classico, updated_at DESC)
    WHERE ativo = TRUE AND estoque > 0;
CREATE INDEX ix_produto_home_lancamento ON produto (lancamento, created_at DESC)
    WHERE ativo = TRUE AND estoque > 0;
CREATE INDEX ix_produto_home_categoria ON produto (categoria_id, created_at DESC)
    WHERE ativo = TRUE AND estoque > 0;
