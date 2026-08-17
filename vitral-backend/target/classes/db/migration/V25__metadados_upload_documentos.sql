CREATE TABLE documento_upload (
    id BIGSERIAL PRIMARY KEY,
    sebo_id BIGINT NOT NULL REFERENCES sebo(id),
    nome_interno VARCHAR(80) NOT NULL UNIQUE,
    nome_original VARCHAR(255) NOT NULL,
    mime_type VARCHAR(50) NOT NULL,
    tamanho_bytes BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_documento_upload_tamanho CHECK (tamanho_bytes > 0),
    CONSTRAINT ck_documento_upload_mime CHECK (mime_type IN ('application/pdf','image/jpeg','image/png'))
);
CREATE INDEX ix_documento_upload_sebo ON documento_upload(sebo_id);
