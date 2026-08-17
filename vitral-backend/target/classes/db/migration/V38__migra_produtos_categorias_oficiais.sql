-- A V33 criou as quatro categorias oficiais (Livros, CDs, Vinis, HQs / Mangas), mas os produtos
-- ja cadastrados continuaram apontando para as categorias antigas. Como o catalogo so oferece as
-- quatro oficiais, esses produtos ficaram invisiveis ao filtrar por categoria.

-- Preserva a informacao da categoria antiga no genero do livro, quando ainda nao houver genero.
UPDATE produto p
SET book_genre = mapa.genero
FROM (VALUES
    ('literatura-brasileira', 'ROMANCE'),
    ('ficcao', 'FICCAO'),
    ('nao-ficcao', 'OUTROS'),
    ('ciencia-tecnologia', 'TECNICO'),
    ('historia', 'HISTORIA'),
    ('filosofia', 'FILOSOFIA'),
    ('infantil-juvenil', 'INFANTIL'),
    ('auto-ajuda', 'AUTOAJUDA'),
    ('gastronomia', 'OUTROS')
) AS mapa(slug, genero)
JOIN categoria c ON c.slug = mapa.slug
WHERE p.categoria_id = c.id
  AND p.book_genre IS NULL;

UPDATE produto
SET categoria_id = (SELECT id FROM categoria WHERE slug = 'livros')
WHERE EXISTS (SELECT 1 FROM categoria WHERE slug = 'livros')
  AND categoria_id IN (SELECT id FROM categoria WHERE slug IN (
      'literatura-brasileira', 'ficcao', 'nao-ficcao', 'ciencia-tecnologia',
      'historia', 'filosofia', 'infantil-juvenil', 'auto-ajuda', 'gastronomia'));

UPDATE produto
SET categoria_id = (SELECT id FROM categoria WHERE slug = 'vinis')
WHERE EXISTS (SELECT 1 FROM categoria WHERE slug = 'vinis')
  AND categoria_id IN (SELECT id FROM categoria WHERE slug = 'vinil');

-- Mantem o historico de recomendacao apontando para as categorias que ainda existem no catalogo.
UPDATE recomendacao_evento e
SET categoria_id = p.categoria_id,
    tipo_produto = c.slug
FROM produto p
JOIN categoria c ON c.id = p.categoria_id
WHERE e.produto_id = p.id
  AND e.categoria_id IS DISTINCT FROM p.categoria_id;
