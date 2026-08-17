ALTER TABLE pedido ADD COLUMN confirmado_em TIMESTAMPTZ;
ALTER TABLE pedido ADD COLUMN pago_em TIMESTAMPTZ;
ALTER TABLE pedido ADD COLUMN cancelado_em TIMESTAMPTZ;
ALTER TABLE pedido ADD COLUMN reembolsado_em TIMESTAMPTZ;

ALTER TABLE pedido DROP CONSTRAINT ck_pedido_status;
ALTER TABLE pedido ADD CONSTRAINT ck_pedido_status CHECK (status IN (
    'AGUARDANDO_CONFIRMACAO', 'CONFIRMADO', 'CANCELADO', 'REEMBOLSADO'
));

UPDATE pedido SET confirmado_em = updated_at, pago_em = updated_at
WHERE status = 'CONFIRMADO' AND status_pagamento = 'APROVADO';
UPDATE pedido SET cancelado_em = updated_at WHERE status = 'CANCELADO';

CREATE INDEX ix_pedido_faturamento ON pedido (sebo_id, pago_em, confirmado_em);
