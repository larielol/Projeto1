package com.vitral.service;

import java.time.Duration;
import java.time.OffsetDateTime;

import org.springframework.stereotype.Component;

@Component
public class RecenciaRecomendacaoService {
    /** Faixas deterministicas: 0-7d=1; 8-30d=.75; 31-90d=.5; 91-180d=.25; depois=0. */
    public double fator(OffsetDateTime evento, OffsetDateTime agora) {
        if (evento == null || evento.isAfter(agora)) return 1.0;
        long dias = Duration.between(evento, agora).toDays();
        if (dias <= 7) return 1.0;
        if (dias <= 30) return 0.75;
        if (dias <= 90) return 0.5;
        if (dias <= 180) return 0.25;
        return 0.0;
    }
}
