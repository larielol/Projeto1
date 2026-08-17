package com.vitral.enumerations;

public enum TipoEventoRecomendacao {
    PESQUISA(1),
    VISUALIZACAO(2),
    FAVORITO_ADICIONADO(4),
    FAVORITO_REMOVIDO(-4),
    CESTA_ADICIONADO(6),
    CESTA_REMOVIDO(-3),
    COMPRA_CONCLUIDA(10);

    private final int pesoBase;

    TipoEventoRecomendacao(int pesoBase) {
        this.pesoBase = pesoBase;
    }

    public int pesoBase() {
        return pesoBase;
    }
}
