package com.vitral.service;

import java.util.List;

public interface CatalogoExternoClient {

    String FONTE_OPEN_LIBRARY = "OPEN_LIBRARY";
    String FONTE_MUSICBRAINZ = "MUSICBRAINZ";

    List<MetadadoProduto> buscar(String termo);

    record MetadadoProduto(
            String titulo,
            String autor,
            Integer ano,
            String descricao,
            String categoriaSlug,
            String fotoUrl,
            String fonte) {
    }
}
