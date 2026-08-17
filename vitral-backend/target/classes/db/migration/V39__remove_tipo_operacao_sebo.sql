-- Todo sebo passa a ser virtual: a venda presencial foi removida do produto.
-- O endereco continua obrigatorio no cadastro, porque alimenta a ordenacao por proximidade.
ALTER TABLE sebo
    DROP COLUMN IF EXISTS tipo_operacao;
