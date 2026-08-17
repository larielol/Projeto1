package com.vitral.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import com.vitral.enumerations.TipoMovimentacaoEstoque;

public record MovimentacaoEstoqueResponse(
        Long id,
        Long produtoId,
        Long seboId,
        Long operadorId,
        Long movimentacaoOrigemId,
        TipoMovimentacaoEstoque tipo,
        Integer quantidade,
        Integer estoqueAntes,
        Integer estoqueDepois,
        BigDecimal valorUnitario,
        BigDecimal valorTotal,
        String observacao,
        OffsetDateTime criadoEm) {
}
