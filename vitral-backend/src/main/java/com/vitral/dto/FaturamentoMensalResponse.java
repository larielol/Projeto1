package com.vitral.dto;

import java.math.BigDecimal;

public record FaturamentoMensalResponse(
        int ano,
        int mes,
        BigDecimal vendasOnline,
        BigDecimal reembolsos,
        BigDecimal total,
        BigDecimal totalLiquido) {
}
