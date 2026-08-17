package com.vitral.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record OfertaRequest(
        @NotNull Long produtoId,
        @NotNull @DecimalMin(value = "0.0", inclusive = false) @Digits(integer = 10, fraction = 2) BigDecimal precoPromocional,
        @Size(max = 500) String descricao,
        OffsetDateTime inicioEm,
        OffsetDateTime fimEm,
        Boolean ativa) {
}
