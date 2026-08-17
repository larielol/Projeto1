package com.vitral.dto;

import java.util.List;

public record HomeCategoriaResponse(Long id, String nome, String slug, List<ProdutoResponse> produtos, long total) {
}
