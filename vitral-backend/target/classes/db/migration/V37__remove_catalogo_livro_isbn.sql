-- O cadastro por ISBN foi substituido pelo autopreenchimento a partir do titulo,
-- que atende as quatro categorias do sebo (livros, CDs, vinis e HQs / mangas).
ALTER TABLE produto
    DROP COLUMN IF EXISTS catalogo_livro_id;

DROP TABLE IF EXISTS catalogo_livro;
