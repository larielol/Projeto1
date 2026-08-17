package com.vitral.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.metadados")
public record MetadadosProdutoProperties(
        boolean enabled,
        String userAgent,
        int limitePorFonte,
        String openLibrarySearchUrl,
        String openLibraryWorkUrl,
        String openLibraryCoverUrl,
        String musicBrainzSearchUrl,
        String wikipediaSearchUrl) {
}
