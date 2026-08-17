package com.vitral.dto;

import java.util.List;

public record HomeSectionResponse(String titulo, List<ProdutoResponse> produtos, long total) {
}
