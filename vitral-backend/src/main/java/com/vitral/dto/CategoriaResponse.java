package com.vitral.dto;

public record CategoriaResponse(
        Long id,
        String nome,
        String slug,
        String descricao) {
}
