CREATE TABLE catalogo_livro (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    isbn           VARCHAR(13)  NOT NULL,
    titulo         VARCHAR(255) NOT NULL,
    autores        VARCHAR(500),
    editora        VARCHAR(255),
    edicao         VARCHAR(100),
    ano_publicacao INTEGER,
    idioma         VARCHAR(50),
    capa_url       VARCHAR(500),
    created_at     TIMESTAMPTZ NOT NULL,
    updated_at     TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_catalogo_livro_isbn UNIQUE (isbn)
);

ALTER TABLE produto ADD COLUMN catalogo_livro_id BIGINT;
ALTER TABLE produto ADD CONSTRAINT fk_produto_catalogo_livro
    FOREIGN KEY (catalogo_livro_id) REFERENCES catalogo_livro (id);
CREATE INDEX ix_produto_catalogo_livro ON produto (catalogo_livro_id);
