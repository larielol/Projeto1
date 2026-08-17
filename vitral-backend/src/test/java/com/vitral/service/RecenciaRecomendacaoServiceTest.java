package com.vitral.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;

class RecenciaRecomendacaoServiceTest {
    private final RecenciaRecomendacaoService service = new RecenciaRecomendacaoService();
    private final OffsetDateTime agora = OffsetDateTime.parse("2026-07-27T12:00:00-03:00");

    @Test void ateSeteDiasValeIntegralmente() { assertThat(service.fator(agora.minusDays(7), agora)).isEqualTo(1); }
    @Test void ateTrintaDiasValeSetentaECincoPorCento() { assertThat(service.fator(agora.minusDays(20), agora)).isEqualTo(.75); }
    @Test void ateNoventaDiasValeMetade() { assertThat(service.fator(agora.minusDays(60), agora)).isEqualTo(.5); }
    @Test void ateCentoEOitentaDiasValeUmQuarto() { assertThat(service.fator(agora.minusDays(120), agora)).isEqualTo(.25); }
    @Test void acimaDeCentoEOitentaDiasEIgnorado() { assertThat(service.fator(agora.minusDays(181), agora)).isZero(); }
    @Test void dataFuturaNaoAmplificaPontuacao() { assertThat(service.fator(agora.plusDays(1), agora)).isEqualTo(1); }
}
