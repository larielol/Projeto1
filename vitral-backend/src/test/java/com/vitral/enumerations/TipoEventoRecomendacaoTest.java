package com.vitral.enumerations;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

class TipoEventoRecomendacaoTest {
    @Test void pesosFicamCentralizadosNoEnum() {
        assertThat(TipoEventoRecomendacao.PESQUISA.pesoBase()).isEqualTo(1);
        assertThat(TipoEventoRecomendacao.VISUALIZACAO.pesoBase()).isEqualTo(2);
        assertThat(TipoEventoRecomendacao.FAVORITO_ADICIONADO.pesoBase()).isEqualTo(4);
        assertThat(TipoEventoRecomendacao.CESTA_ADICIONADO.pesoBase()).isEqualTo(6);
        assertThat(TipoEventoRecomendacao.COMPRA_CONCLUIDA.pesoBase()).isEqualTo(10);
        assertThat(TipoEventoRecomendacao.FAVORITO_REMOVIDO.pesoBase()).isEqualTo(-4);
        assertThat(TipoEventoRecomendacao.CESTA_REMOVIDO.pesoBase()).isEqualTo(-3);
    }
}
