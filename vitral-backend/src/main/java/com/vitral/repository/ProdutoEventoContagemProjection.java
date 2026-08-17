package com.vitral.repository;

import com.vitral.enumerations.TipoEventoRecomendacao;

public interface ProdutoEventoContagemProjection {
    Long getProdutoId();

    TipoEventoRecomendacao getTipo();

    long getQuantidade();
}
