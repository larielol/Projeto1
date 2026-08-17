package com.vitral.dto;

import com.vitral.enumerations.FormaPagamento;

import jakarta.validation.constraints.NotNull;

public record ConfirmarPedidoRequest(
        @NotNull FormaPagamento formaPagamento,
        String numeroCartao) {
}
