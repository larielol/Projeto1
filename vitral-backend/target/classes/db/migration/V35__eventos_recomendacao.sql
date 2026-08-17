CREATE TABLE recomendacao_evento (
    id BIGSERIAL PRIMARY KEY,
    account_id BIGINT NOT NULL REFERENCES account(id) ON DELETE CASCADE,
    tipo VARCHAR(30) NOT NULL,
    produto_id BIGINT REFERENCES produto(id) ON DELETE SET NULL,
    categoria_id BIGINT REFERENCES categoria(id) ON DELETE SET NULL,
    sebo_id BIGINT REFERENCES sebo(id) ON DELETE SET NULL,
    pedido_id BIGINT REFERENCES pedido(id) ON DELETE SET NULL,
    tipo_produto VARCHAR(40),
    genero VARCHAR(40),
    autor_artista VARCHAR(255),
    faixa_preco VARCHAR(30),
    termo_pesquisa VARCHAR(160),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_recomendacao_evento_tipo CHECK (tipo IN (
        'PESQUISA', 'VISUALIZACAO', 'FAVORITO_ADICIONADO', 'FAVORITO_REMOVIDO',
        'CESTA_ADICIONADO', 'CESTA_REMOVIDO', 'COMPRA_CONCLUIDA'))
);

CREATE INDEX idx_recomendacao_evento_usuario_data
    ON recomendacao_evento(account_id, created_at DESC);
CREATE INDEX idx_recomendacao_evento_usuario_tipo_data
    ON recomendacao_evento(account_id, tipo, created_at DESC);
CREATE INDEX idx_recomendacao_evento_produto_data
    ON recomendacao_evento(produto_id, created_at DESC);
CREATE INDEX idx_recomendacao_evento_categoria_data
    ON recomendacao_evento(categoria_id, created_at DESC);
CREATE UNIQUE INDEX uk_recomendacao_compra_pedido_produto
    ON recomendacao_evento(pedido_id, produto_id, tipo)
    WHERE tipo = 'COMPRA_CONCLUIDA';
