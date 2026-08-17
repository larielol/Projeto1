CREATE TABLE categoria (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nome        VARCHAR(120) NOT NULL,
    slug        VARCHAR(140) NOT NULL,
    descricao   VARCHAR(500),
    created_at  TIMESTAMPTZ  NOT NULL,
    updated_at  TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uk_categoria_nome UNIQUE (nome),
    CONSTRAINT uk_categoria_slug UNIQUE (slug)
);

ALTER TABLE produto
    ADD COLUMN categoria_id BIGINT;

ALTER TABLE produto
    ADD CONSTRAINT fk_produto_categoria
    FOREIGN KEY (categoria_id) REFERENCES categoria (id);

CREATE INDEX ix_produto_categoria ON produto (categoria_id);

INSERT INTO categoria (nome, slug, descricao, created_at, updated_at) VALUES
    ('Literatura Brasileira', 'literatura-brasileira', 'Autores e obras nacionais', NOW(), NOW()),
    ('Ficcao', 'ficcao', 'Romances, contos e narrativas ficcionais', NOW(), NOW()),
    ('Nao-ficcao', 'nao-ficcao', 'Biografias, memorias e relatos reais', NOW(), NOW()),
    ('Ciencia e Tecnologia', 'ciencia-tecnologia', 'Obras cientificas e tecnicas', NOW(), NOW()),
    ('Historia', 'historia', 'Obras historicas e documentais', NOW(), NOW()),
    ('Filosofia', 'filosofia', 'Filosofia, etica e pensamento', NOW(), NOW()),
    ('Infantil e Juvenil', 'infantil-juvenil', 'Livros para criancas e jovens', NOW(), NOW()),
    ('Auto-ajuda', 'auto-ajuda', 'Desenvolvimento pessoal e bem-estar', NOW(), NOW()),
    ('HQs e Mangas', 'hqs-mangas', 'Quadrinhos, graphic novels e mangas', NOW(), NOW()),
    ('Gastronomia', 'gastronomia', 'Culinaria e cultura gastronomica', NOW(), NOW());
