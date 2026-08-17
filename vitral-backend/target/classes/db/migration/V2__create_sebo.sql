CREATE TABLE sebo (
    id                     BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    account_id             BIGINT       NOT NULL,
    descricao              VARCHAR(2000),
    telefone               VARCHAR(20),
    cep                    VARCHAR(9),
    logradouro             VARCHAR(255),
    cidade                 VARCHAR(120),
    uf                     VARCHAR(2),
    horario_funcionamento  VARCHAR(255),
    foto_url               VARCHAR(500),
    created_at             TIMESTAMPTZ  NOT NULL,
    updated_at             TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uk_sebo_account UNIQUE (account_id),
    CONSTRAINT fk_sebo_account FOREIGN KEY (account_id) REFERENCES account (id) ON DELETE CASCADE
);

CREATE INDEX ix_sebo_cidade ON sebo (cidade);
