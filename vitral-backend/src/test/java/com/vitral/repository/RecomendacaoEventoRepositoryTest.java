package com.vitral.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
class RecomendacaoEventoRepositoryTest {
    @Autowired RecomendacaoEventoRepository repository;

    @Test void consultaAgregadaDePopularidadeFuncionaSemHistorico() {
        assertThat(repository.contagemPorProdutoETipo(OffsetDateTime.now().minusDays(90))).isEmpty();
    }

    @Test void limpezaDeRetencaoECompativelComUsuariosExistentes() {
        assertThat(repository.anonimizarTermosAntigos(OffsetDateTime.now())).isZero();
        assertThat(repository.excluirEventosAntigos(OffsetDateTime.now())).isZero();
    }
}
