package com.vitral.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class GeoUtilsTest {

    @Test
    @DisplayName("Deve retornar zero quando os dois pontos sao identicos")
    void haversineKm_mesmoPonto_retornaZero() {
        double distancia = GeoUtils.haversineKm(-7.1219, -34.8850, -7.1219, -34.8850);

        assertThat(distancia).isEqualTo(0.0, within(0.0001));
    }

    @Test
    @DisplayName("Deve calcular a distancia aproximada entre Joao Pessoa e Sao Paulo")
    void haversineKm_pontosDistantes_calculaDistanciaAproximada() {
        double distancia = GeoUtils.haversineKm(-7.1219, -34.8850, -23.5505, -46.6333);

        assertThat(distancia).isCloseTo(2180.0, within(50.0));
    }

    @Test
    @DisplayName("Deve ser simetrica independente da ordem dos pontos")
    void haversineKm_ordemInvertida_retornaMesmaDistancia() {
        double distanciaIda = GeoUtils.haversineKm(-7.1219, -34.8850, -23.5505, -46.6333);
        double distanciaVolta = GeoUtils.haversineKm(-23.5505, -46.6333, -7.1219, -34.8850);

        assertThat(distanciaIda).isEqualTo(distanciaVolta, within(0.0001));
    }
}
