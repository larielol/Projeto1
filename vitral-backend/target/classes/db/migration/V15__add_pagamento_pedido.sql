ALTER TABLE pedido
    ADD COLUMN forma_pagamento VARCHAR(20);

ALTER TABLE pedido
    ADD COLUMN status_pagamento VARCHAR(20) NOT NULL DEFAULT 'PENDENTE';

ALTER TABLE pedido
    ADD CONSTRAINT ck_pedido_status_pagamento CHECK (status_pagamento IN ('PENDENTE', 'APROVADO', 'RECUSADO'));
