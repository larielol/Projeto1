package com.vitral.dto;

import java.math.BigDecimal;

import com.vitral.enumerations.CondicaoProduto;

public record FavoritoResponse(
        Long id,
        Long produtoId,
        String titulo,
        String autor,
        BigDecimal preco,
        BigDecimal precoPromocional,
        CondicaoProduto condicao,
        String fotoUrl) {
}
