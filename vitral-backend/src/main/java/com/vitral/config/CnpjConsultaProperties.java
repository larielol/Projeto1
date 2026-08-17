package com.vitral.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.cnpj-consulta")
public record CnpjConsultaProperties(boolean enabled, String tokenUrl, String consultaUrl,
        String clientId, String clientSecret, boolean mockAutoAprovar) {
}
