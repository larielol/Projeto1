package com.vitral.dto;

import java.util.List;

public record HomeResponse(
        HomeSectionResponse lancamentos,
        HomeSectionResponse classicos,
        HomeSectionResponse recomendados,
        List<HomeCategoriaResponse> categorias) {
}
