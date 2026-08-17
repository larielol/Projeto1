package com.vitral.dto;

import java.math.BigDecimal;
import com.vitral.enumerations.CondicaoProduto;

public record VendedorProdutoResponse(
        Long produtoId,
        Long seboId,
        String seboNome,
        BigDecimal preco,
        BigDecimal precoPromocional,
        BigDecimal precoEfetivo,
        Integer estoque,
        CondicaoProduto condicao) {
}
