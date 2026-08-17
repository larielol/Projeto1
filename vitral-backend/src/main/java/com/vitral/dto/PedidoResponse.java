package com.vitral.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import com.vitral.enumerations.FormaPagamento;
import com.vitral.enumerations.StatusPagamento;
import com.vitral.enumerations.StatusPedido;

public record PedidoResponse(
        Long id,
        Long accountId,
        Long seboId,
        StatusPedido status,
        FormaPagamento formaPagamento,
        StatusPagamento statusPagamento,
        BigDecimal total,
        OffsetDateTime createdAt,
        OffsetDateTime confirmadoEm,
        OffsetDateTime pagoEm,
        OffsetDateTime canceladoEm,
        OffsetDateTime reembolsadoEm,
        List<PedidoItemResponse> itens) {
}
