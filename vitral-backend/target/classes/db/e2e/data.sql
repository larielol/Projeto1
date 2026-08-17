INSERT INTO account (name, email, password_hash, type, email_verificado, created_at, updated_at)
VALUES
    ('Sebo E2E', 'sebo.e2e@vitral.test', '$2y$10$52z1p18.egDdQNSUIeVvsO074XSRoARd.B9WGb095Gg3NcPlP/ETO', 'SEBO', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Cliente E2E', 'cliente.e2e@vitral.test', '$2y$10$52z1p18.egDdQNSUIeVvsO074XSRoARd.B9WGb095Gg3NcPlP/ETO', 'USUARIO', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Confirmação E2E', 'confirmacao.e2e@vitral.test', '$2y$10$52z1p18.egDdQNSUIeVvsO074XSRoARd.B9WGb095Gg3NcPlP/ETO', 'USUARIO', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Sebo Descartável E2E', 'descartavel.e2e@vitral.test', '$2y$10$52z1p18.egDdQNSUIeVvsO074XSRoARd.B9WGb095Gg3NcPlP/ETO', 'SEBO', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO token_verificacao_email (account_id, token, expira_em, usado, created_at, updated_at)
VALUES (3, '11111111-1111-1111-1111-111111111111', DATEADD('HOUR', 1, CURRENT_TIMESTAMP), FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO token_recuperacao_senha (account_id, token, expira_em, usado, created_at, updated_at)
VALUES (2, '22222222-2222-2222-2222-222222222222', DATEADD('HOUR', 1, CURRENT_TIMESTAMP), FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO sebo (account_id, descricao, telefone, created_at, updated_at)
VALUES (1, 'Sebo criado exclusivamente para os testes E2E.', '85999990000', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO categoria (nome, slug, descricao, created_at, updated_at)
VALUES ('Romance E2E', 'romance-e2e', 'Categoria real do banco E2E.', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO produto (sebo_id, categoria_id, titulo, autor, descricao, preco, estoque, condicao, ativo, created_at, updated_at)
VALUES
    (1, 1, 'Livro E2E Integrado', 'Autora E2E', 'Produto servido pela API real.', 42.50, 5, 'USADO', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (1, 1, 'Segundo Livro E2E', 'Autor E2E', 'Outro produto real do catálogo.', 30.00, 2, 'SEMINOVO', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO oferta (produto_id, preco_promocional, descricao, inicio_em, fim_em, ativa, created_at, updated_at)
VALUES (2, 24.00, 'Oferta E2E ativa', CURRENT_TIMESTAMP, DATEADD('DAY', 7, CURRENT_TIMESTAMP), TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
