package com.vitral.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.recomendacao")
public record RecomendacaoProperties(
        int historicoMaximo,
        int candidatosMaximo,
        int tamanhoHome,
        int retencaoTermoDias,
        int retencaoEventoDias,
        int janelaVisualizacaoMinutos,
        int eventosRepetidosDiaMaximo,
        int compraRecenteDias,
        int tamanhoPaginaMaximo,
        int popularidadeDias) {

    public RecomendacaoProperties {
        if (historicoMaximo < 1) historicoMaximo = 1000;
        if (candidatosMaximo < 1) candidatosMaximo = 200;
        if (tamanhoHome < 1) tamanhoHome = 20;
        if (retencaoTermoDias < 1) retencaoTermoDias = 30;
        if (retencaoEventoDias < 1) retencaoEventoDias = 365;
        if (janelaVisualizacaoMinutos < 1) janelaVisualizacaoMinutos = 30;
        if (eventosRepetidosDiaMaximo < 1) eventosRepetidosDiaMaximo = 3;
        if (compraRecenteDias < 1) compraRecenteDias = 90;
        if (tamanhoPaginaMaximo < 1) tamanhoPaginaMaximo = 50;
        if (popularidadeDias < 1) popularidadeDias = 90;
    }
}
