CREATE TABLE token_verificacao_email (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    account_id BIGINT       NOT NULL,
    token      VARCHAR(36)  NOT NULL,
    expira_em  TIMESTAMPTZ  NOT NULL,
    usado      BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ  NOT NULL,
    updated_at TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uk_token_verificacao UNIQUE (token),
    CONSTRAINT fk_token_verificacao_account FOREIGN KEY (account_id)
        REFERENCES account (id) ON DELETE CASCADE
);

CREATE INDEX ix_token_verificacao_account ON token_verificacao_email (account_id);
