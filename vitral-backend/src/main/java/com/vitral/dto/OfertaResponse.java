package com.vitral.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record OfertaResponse(
        Long id,
        Long produtoId,
        Long seboId,
        String tituloProduto,
        BigDecimal precoOriginal,
        BigDecimal precoPromocional,
        String descricao,
        OffsetDateTime inicioEm,
        OffsetDateTime fimEm,
        Boolean ativa) {
}
