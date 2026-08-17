ALTER TABLE account ADD COLUMN email_verificado BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE account SET email_verificado = TRUE;
