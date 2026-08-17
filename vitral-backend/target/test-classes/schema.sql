DROP TABLE IF EXISTS recomendacao_evento;
DROP TABLE IF EXISTS pedido_item;
DROP TABLE IF EXISTS pedido;
DROP TABLE IF EXISTS movimentacao_estoque;
DROP TABLE IF EXISTS auditoria_verificacao_sebo;
DROP TABLE IF EXISTS documento_verificacao_sebo;
DROP TABLE IF EXISTS documento_upload;
DROP TABLE IF EXISTS mensagem;
DROP TABLE IF EXISTS cesta_item;
DROP TABLE IF EXISTS favoritos;
DROP TABLE IF EXISTS oferta;
DROP TABLE IF EXISTS token_verificacao_email;
DROP TABLE IF EXISTS produto;
DROP TABLE IF EXISTS catalogo_livro;
DROP TABLE IF EXISTS categoria;
DROP TABLE IF EXISTS sebo;
DROP TABLE IF EXISTS account;

CREATE TABLE account (
    id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name             VARCHAR(255) NOT NULL,
    username         VARCHAR(50)  NOT NULL,
    email            VARCHAR(255) NOT NULL,
    password_hash    VARCHAR(255) NOT NULL,
    type             VARCHAR(20)  NOT NULL,
    email_verificado BOOLEAN      NOT NULL DEFAULT FALSE,
    foto_url         VARCHAR(500),
    cpf              VARCHAR(11),
    cep              VARCHAR(8),
    logradouro       VARCHAR(255),
    numero           VARCHAR(30),
    complemento      VARCHAR(255),
    bairro           VARCHAR(120),
    cidade           VARCHAR(120),
    estado           VARCHAR(2),
    auth_version     INTEGER      NOT NULL DEFAULT 0,
    ativo            BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMP    NOT NULL,
    updated_at       TIMESTAMP    NOT NULL,
    CONSTRAINT uk_account_email UNIQUE (email),
    CONSTRAINT uk_account_username UNIQUE (username),
    CONSTRAINT ck_account_type CHECK (type IN ('SEBO', 'USUARIO', 'ADMIN'))
);

CREATE TABLE sebo (
    id                     BIGINT       NOT NULL GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    account_id             BIGINT       NOT NULL,
    descricao              VARCHAR(2000),
    telefone               VARCHAR(20),
    cnpj                   VARCHAR(14),
    status_verificacao     VARCHAR(20) NOT NULL DEFAULT 'VERIFICADO',
    verificado_em          TIMESTAMP,
    motivo_rejeicao        VARCHAR(500),
    razao_social_receita   VARCHAR(255),
    status_consulta_cnpj   VARCHAR(20) NOT NULL DEFAULT 'NAO_CONSULTADO',
    cnpj_consultado_em     TIMESTAMP,
    mensagem_consulta_cnpj VARCHAR(500),
    foto_url               VARCHAR(500),
    cep                    VARCHAR(8),
    logradouro             VARCHAR(255),
    cidade                 VARCHAR(120),
    uf                     VARCHAR(2),
    horario_funcionamento  VARCHAR(255),
    latitude               DOUBLE PRECISION,
    longitude              DOUBLE PRECISION,
    data_criacao           TIMESTAMP,
    ultima_atividade       TIMESTAMP,
    confirmado             BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at             TIMESTAMP    NOT NULL,
    updated_at             TIMESTAMP    NOT NULL,
    CONSTRAINT uk_sebo_account UNIQUE (account_id),
    CONSTRAINT fk_sebo_account FOREIGN KEY (account_id) REFERENCES account (id) ON DELETE CASCADE
);

CREATE TABLE documento_verificacao_sebo (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    sebo_id BIGINT NOT NULL,
    tipo_documento VARCHAR(30) NOT NULL,
    arquivo_url VARCHAR(500) NOT NULL,
    status VARCHAR(20) NOT NULL,
    enviado_em TIMESTAMP NOT NULL,
    analisado_em TIMESTAMP,
    analisado_por BIGINT,
    motivo_rejeicao VARCHAR(500),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_documento_sebo FOREIGN KEY (sebo_id) REFERENCES sebo(id),
    CONSTRAINT fk_documento_admin FOREIGN KEY (analisado_por) REFERENCES account(id)
);

CREATE TABLE auditoria_verificacao_sebo (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    sebo_id BIGINT NOT NULL,
    analisado_por BIGINT NOT NULL,
    status_anterior VARCHAR(20) NOT NULL,
    novo_status VARCHAR(20) NOT NULL,
    motivo VARCHAR(500),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_auditoria_sebo FOREIGN KEY (sebo_id) REFERENCES sebo(id),
    CONSTRAINT fk_auditoria_admin FOREIGN KEY (analisado_por) REFERENCES account(id)
);

CREATE TABLE documento_upload (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    sebo_id BIGINT NOT NULL,
    nome_interno VARCHAR(80) NOT NULL UNIQUE,
    nome_original VARCHAR(255) NOT NULL,
    mime_type VARCHAR(50) NOT NULL,
    tamanho_bytes BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_documento_upload_sebo FOREIGN KEY (sebo_id) REFERENCES sebo(id)
);

CREATE TABLE categoria (
    id          BIGINT       NOT NULL GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nome        VARCHAR(120) NOT NULL,
    slug        VARCHAR(140) NOT NULL,
    descricao   VARCHAR(500),
    created_at  TIMESTAMP    NOT NULL,
    updated_at  TIMESTAMP    NOT NULL,
    CONSTRAINT uk_categoria_nome UNIQUE (nome),
    CONSTRAINT uk_categoria_slug UNIQUE (slug)
);

CREATE TABLE produto (
    id          BIGINT         NOT NULL GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    sebo_id     BIGINT         NOT NULL,
    categoria_id BIGINT,
    book_genre VARCHAR(20),
    titulo      VARCHAR(255)   NOT NULL,
    autor       VARCHAR(255),
    descricao   VARCHAR(2000),
    ano         INTEGER,
    preco       NUMERIC(12, 2) NOT NULL,
    estoque     INTEGER        NOT NULL DEFAULT 1,
    condicao    VARCHAR(20)    NOT NULL,
    foto_url    VARCHAR(500),
    ativo       BOOLEAN        NOT NULL DEFAULT TRUE,
    classico    BOOLEAN        NOT NULL DEFAULT FALSE,
    lancamento  BOOLEAN        NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP      NOT NULL,
    updated_at  TIMESTAMP      NOT NULL,
    CONSTRAINT fk_produto_sebo FOREIGN KEY (sebo_id) REFERENCES sebo (id) ON DELETE CASCADE,
    CONSTRAINT fk_produto_categoria FOREIGN KEY (categoria_id) REFERENCES categoria (id),
    CONSTRAINT ck_produto_condicao CHECK (condicao IN ('NOVO', 'USADO', 'SEMINOVO')),
    CONSTRAINT ck_produto_preco CHECK (preco >= 0),
    CONSTRAINT ck_produto_estoque CHECK (estoque >= 0)
);

CREATE TABLE movimentacao_estoque (
    id BIGINT NOT NULL GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    produto_id BIGINT NOT NULL,
    sebo_id BIGINT NOT NULL,
    operador_id BIGINT NOT NULL,
    movimentacao_origem_id BIGINT,
    tipo VARCHAR(20) NOT NULL,
    quantidade INTEGER NOT NULL,
    estoque_antes INTEGER NOT NULL,
    estoque_depois INTEGER NOT NULL,
    valor_unitario NUMERIC(12, 2),
    valor_total NUMERIC(12, 2),
    observacao VARCHAR(500),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_mov_produto FOREIGN KEY (produto_id) REFERENCES produto(id),
    CONSTRAINT fk_mov_sebo FOREIGN KEY (sebo_id) REFERENCES sebo(id),
    CONSTRAINT fk_mov_operador FOREIGN KEY (operador_id) REFERENCES account(id),
    CONSTRAINT fk_mov_origem FOREIGN KEY (movimentacao_origem_id) REFERENCES movimentacao_estoque(id)
);

CREATE TABLE oferta (
    id                 BIGINT         NOT NULL GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    produto_id         BIGINT         NOT NULL,
    preco_promocional  NUMERIC(12, 2) NOT NULL,
    descricao          VARCHAR(500),
    inicio_em          TIMESTAMP,
    fim_em             TIMESTAMP,
    ativa              BOOLEAN        NOT NULL DEFAULT TRUE,
    created_at         TIMESTAMP      NOT NULL,
    updated_at         TIMESTAMP      NOT NULL,
    CONSTRAINT fk_oferta_produto FOREIGN KEY (produto_id) REFERENCES produto (id) ON DELETE CASCADE,
    CONSTRAINT ck_oferta_preco CHECK (preco_promocional > 0),
    CONSTRAINT ck_oferta_periodo CHECK (fim_em IS NULL OR inicio_em IS NULL OR fim_em >= inicio_em)
);

CREATE TABLE token_verificacao_email (
    id         BIGINT      NOT NULL GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    account_id BIGINT      NOT NULL,
    token      VARCHAR(36) NOT NULL,
    expira_em  TIMESTAMP   NOT NULL,
    usado      BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP   NOT NULL,
    updated_at TIMESTAMP   NOT NULL,
    CONSTRAINT uk_token_verificacao UNIQUE (token),
    CONSTRAINT fk_token_verificacao_account FOREIGN KEY (account_id)
        REFERENCES account (id) ON DELETE CASCADE
);

CREATE TABLE favoritos (
    id         BIGINT    NOT NULL GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    account_id BIGINT    NOT NULL,
    produto_id BIGINT    NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_favorito UNIQUE (account_id, produto_id),
    CONSTRAINT fk_favorito_account FOREIGN KEY (account_id) REFERENCES account (id) ON DELETE CASCADE,
    CONSTRAINT fk_favorito_produto FOREIGN KEY (produto_id) REFERENCES produto (id) ON DELETE CASCADE
);

CREATE TABLE cesta_item (
    id         BIGINT    NOT NULL GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    account_id BIGINT    NOT NULL,
    produto_id BIGINT    NOT NULL,
    quantidade INTEGER   NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_cesta_item UNIQUE (account_id, produto_id),
    CONSTRAINT fk_cesta_account FOREIGN KEY (account_id) REFERENCES account (id) ON DELETE CASCADE,
    CONSTRAINT fk_cesta_produto FOREIGN KEY (produto_id) REFERENCES produto (id) ON DELETE CASCADE,
    CONSTRAINT ck_cesta_item_quantidade CHECK (quantidade > 0)
);

CREATE TABLE mensagem (
    id              BIGINT        NOT NULL GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    remetente_id    BIGINT        NOT NULL,
    destinatario_id BIGINT        NOT NULL,
    conteudo        VARCHAR(2000) NOT NULL,
    lida            BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP     NOT NULL,
    updated_at      TIMESTAMP     NOT NULL,
    CONSTRAINT fk_mensagem_remetente FOREIGN KEY (remetente_id) REFERENCES account (id) ON DELETE CASCADE,
    CONSTRAINT fk_mensagem_destinatario FOREIGN KEY (destinatario_id) REFERENCES account (id) ON DELETE CASCADE,
    CONSTRAINT ck_mensagem_destinatario CHECK (remetente_id <> destinatario_id)
);

CREATE TABLE pedido (
    id         BIGINT         NOT NULL GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    account_id BIGINT         NOT NULL,
    sebo_id    BIGINT         NOT NULL,
    status     VARCHAR(30)    NOT NULL DEFAULT 'AGUARDANDO_CONFIRMACAO',
    forma_pagamento  VARCHAR(20),
    status_pagamento VARCHAR(20) NOT NULL DEFAULT 'PENDENTE',
    total      NUMERIC(12, 2) NOT NULL,
    confirmado_em TIMESTAMP,
    pago_em TIMESTAMP,
    cancelado_em TIMESTAMP,
    reembolsado_em TIMESTAMP,
    created_at TIMESTAMP      NOT NULL,
    updated_at TIMESTAMP      NOT NULL,
    CONSTRAINT fk_pedido_account FOREIGN KEY (account_id) REFERENCES account (id),
    CONSTRAINT fk_pedido_sebo    FOREIGN KEY (sebo_id)    REFERENCES sebo (id),
    CONSTRAINT ck_pedido_total   CHECK (total >= 0)
);

CREATE TABLE pedido_item (
    id               BIGINT         NOT NULL GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    pedido_id        BIGINT         NOT NULL,
    produto_id       BIGINT         NOT NULL,
    titulo_snapshot  VARCHAR(255)   NOT NULL,
    preco_snapshot   NUMERIC(12, 2) NOT NULL,
    quantidade       INTEGER        NOT NULL DEFAULT 1,
    created_at       TIMESTAMP      NOT NULL,
    updated_at       TIMESTAMP      NOT NULL,
    CONSTRAINT fk_pedido_item_pedido  FOREIGN KEY (pedido_id)  REFERENCES pedido (id) ON DELETE CASCADE,
    CONSTRAINT fk_pedido_item_produto FOREIGN KEY (produto_id) REFERENCES produto (id),
    CONSTRAINT ck_pedido_item_quantidade CHECK (quantidade > 0)
);

CREATE TABLE recomendacao_evento (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    account_id BIGINT NOT NULL,
    tipo VARCHAR(30) NOT NULL,
    produto_id BIGINT,
    categoria_id BIGINT,
    sebo_id BIGINT,
    pedido_id BIGINT,
    tipo_produto VARCHAR(40),
    genero VARCHAR(40),
    autor_artista VARCHAR(255),
    faixa_preco VARCHAR(30),
    termo_pesquisa VARCHAR(160),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_rec_account FOREIGN KEY (account_id) REFERENCES account(id) ON DELETE CASCADE,
    CONSTRAINT fk_rec_produto FOREIGN KEY (produto_id) REFERENCES produto(id) ON DELETE SET NULL,
    CONSTRAINT fk_rec_categoria FOREIGN KEY (categoria_id) REFERENCES categoria(id) ON DELETE SET NULL,
    CONSTRAINT fk_rec_sebo FOREIGN KEY (sebo_id) REFERENCES sebo(id) ON DELETE SET NULL,
    CONSTRAINT fk_rec_pedido FOREIGN KEY (pedido_id) REFERENCES pedido(id) ON DELETE SET NULL
);

CREATE INDEX idx_rec_usuario_data ON recomendacao_evento(account_id, created_at);
CREATE INDEX idx_rec_usuario_tipo_data ON recomendacao_evento(account_id, tipo, created_at);
CREATE INDEX idx_rec_produto_data ON recomendacao_evento(produto_id, created_at);
CREATE INDEX idx_rec_categoria_data ON recomendacao_evento(categoria_id, created_at);
-- Equivalente ao indice parcial da V35: o H2 nao suporta indice com WHERE, e como pedido_id
-- so e preenchido em eventos de compra, o indice completo cobre o mesmo caso.
CREATE UNIQUE INDEX uk_rec_compra_pedido_produto ON recomendacao_evento(pedido_id, produto_id, tipo);
