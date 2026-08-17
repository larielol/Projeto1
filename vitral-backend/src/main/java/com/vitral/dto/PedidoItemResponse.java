package com.vitral.dto;

import java.math.BigDecimal;

public record PedidoItemResponse(
        Long id,
        Long produtoId,
        String tituloSnapshot,
        BigDecimal precoSnapshot,
        Integer quantidade) {
}
