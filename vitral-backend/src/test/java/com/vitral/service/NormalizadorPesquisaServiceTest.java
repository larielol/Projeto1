package com.vitral.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class NormalizadorPesquisaServiceTest {
    private final NormalizadorPesquisaService service = new NormalizadorPesquisaService();

    @Test void normalizaCaixaAcentosEspacosEPontuacao() {
        assertThat(service.normalizar("  Ficção   Científica!!! ")).isEqualTo("ficcao cientifica");
    }
    @Test void ignoraTermoNulo() { assertThat(service.normalizar(null)).isNull(); }
    @Test void ignoraTermoVazio() { assertThat(service.normalizar("   ")).isNull(); }
    @Test void limitaTermoNormalizado() { assertThat(service.normalizar("a".repeat(200))).hasSize(160); }
}
