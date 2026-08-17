-- Novas colunas permanecem nulas para compatibilidade com contas e produtos legados.
ALTER TABLE produto ADD COLUMN book_genre VARCHAR(20);

ALTER TABLE produto ADD CONSTRAINT ck_produto_book_genre CHECK (book_genre IS NULL OR book_genre IN (
    'ROMANCE','FICCAO','FANTASIA','TERROR','SUSPENSE','MISTERIO','AVENTURA','BIOGRAFIA',
    'HISTORIA','FILOSOFIA','POESIA','AUTOAJUDA','INFANTIL','TECNICO','DIDATICO','OUTROS'
));

ALTER TABLE account
    ADD COLUMN cpf VARCHAR(11),
    ADD COLUMN cep VARCHAR(8),
    ADD COLUMN logradouro VARCHAR(255),
    ADD COLUMN numero VARCHAR(30),
    ADD COLUMN complemento VARCHAR(255),
    ADD COLUMN bairro VARCHAR(120),
    ADD COLUMN cidade VARCHAR(120),
    ADD COLUMN estado VARCHAR(2);

ALTER TABLE account ADD CONSTRAINT ck_account_cpf_formato CHECK (cpf IS NULL OR cpf ~ '^[0-9]{11}$');
ALTER TABLE account ADD CONSTRAINT ck_account_cep_formato CHECK (cep IS NULL OR cep ~ '^[0-9]{8}$');
ALTER TABLE account ADD CONSTRAINT ck_account_estado_formato CHECK (estado IS NULL OR estado ~ '^[A-Z]{2}$');

-- Preserva categorias antigas. Corrige apenas o registro que ja usa o slug oficial de HQs.
UPDATE categoria SET nome = 'HQs / Mangás', updated_at = NOW() WHERE slug = 'hqs-mangas';
INSERT INTO categoria (nome, slug, descricao, created_at, updated_at)
VALUES
    ('Livros', 'livros', 'Livros', NOW(), NOW()),
    ('CDs', 'cds', 'CDs', NOW(), NOW()),
    ('Vinis', 'vinis', 'Discos de vinil e LPs', NOW(), NOW())
ON CONFLICT (slug) DO UPDATE SET nome = EXCLUDED.nome, descricao = EXCLUDED.descricao, updated_at = NOW();
INSERT INTO categoria (nome, slug, descricao, created_at, updated_at)
VALUES ('HQs / Mangás', 'hqs-mangas', 'Quadrinhos, graphic novels e mangás', NOW(), NOW())
ON CONFLICT (slug) DO NOTHING;

CREATE INDEX ix_produto_categoria_book_genre ON produto (categoria_id, book_genre);
CREATE UNIQUE INDEX uk_account_cpf_not_null ON account (cpf) WHERE cpf IS NOT NULL;

-- Consulta operacional, sem alterar dados, para planejar a correcao do legado.
CREATE VIEW vw_produtos_categoria_pendente AS
SELECT p.id AS produto_id, p.titulo, p.categoria_id, c.nome AS categoria_nome, c.slug AS categoria_slug
FROM produto p
LEFT JOIN categoria c ON c.id = p.categoria_id
WHERE c.slug IS NULL OR c.slug NOT IN ('livros', 'cds', 'vinis', 'hqs-mangas');

COMMENT ON VIEW vw_produtos_categoria_pendente IS
'Produtos legados com categoria nula ou fora das quatro categorias oficiais; revisar e recategorizar manualmente.';
