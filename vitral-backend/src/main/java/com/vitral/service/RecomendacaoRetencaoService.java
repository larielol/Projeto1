package com.vitral.service;

import java.time.OffsetDateTime;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vitral.config.RecomendacaoProperties;
import com.vitral.repository.RecomendacaoEventoRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RecomendacaoRetencaoService {
    private final RecomendacaoEventoRepository repository;
    private final RecomendacaoProperties properties;

    @Scheduled(cron = "${app.recomendacao.limpeza-cron:0 20 3 * * *}")
    @Transactional
    public void limparExpirados() {
        OffsetDateTime agora = OffsetDateTime.now();
        repository.anonimizarTermosAntigos(agora.minusDays(properties.retencaoTermoDias()));
        repository.excluirEventosAntigos(agora.minusDays(properties.retencaoEventoDias()));
    }
}
