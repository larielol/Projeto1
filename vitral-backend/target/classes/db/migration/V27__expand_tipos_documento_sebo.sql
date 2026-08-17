ALTER TABLE documento_verificacao_sebo
DROP CONSTRAINT IF EXISTS ck_documento_sebo_tipo;

ALTER TABLE documento_verificacao_sebo
ADD CONSTRAINT ck_documento_sebo_tipo
CHECK (tipo_documento IN (
    'CARTAO_CNPJ',
    'CONTRATO_SOCIAL',
    'DOCUMENTO_RESPONSAVEL',
    'COMPROVANTE_BANCARIO',
    'COMPROVANTE_ATIVIDADE',
    'OUTRO'
));
