package com.vitral.dto;

import java.math.BigDecimal;

import com.vitral.enumerations.CondicaoProduto;

public record CestaItemResponse(
        Long id,
        Long produtoId,
        String titulo,
        String autor,
        BigDecimal preco,
        BigDecimal precoOriginal,
        Integer quantidade,
        BigDecimal subtotal,
        Integer estoqueDisponivel,
        CondicaoProduto condicao,
        String fotoUrl) {
}
