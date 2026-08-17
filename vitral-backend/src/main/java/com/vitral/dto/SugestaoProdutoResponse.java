package com.vitral.dto;

public record SugestaoProdutoResponse(
        String titulo,
        String autor,
        Integer ano,
        String descricao,
        Long categoriaId,
        String categoriaSlug,
        String categoriaNome,
        String fotoUrl,
        String fonte) {
}
