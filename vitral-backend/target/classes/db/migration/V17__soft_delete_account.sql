ALTER TABLE account
    ADD COLUMN ativo BOOLEAN NOT NULL DEFAULT TRUE;

CREATE INDEX ix_account_ativo ON account (ativo);
