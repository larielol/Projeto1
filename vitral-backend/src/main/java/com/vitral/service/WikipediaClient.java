package com.vitral.service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.databind.JsonNode;
import com.vitral.config.MetadadosProdutoProperties;

@Component
public class WikipediaClient {

    private static final Map<String, String> TERMO_POR_CATEGORIA = Map.of(
            "livros", "livro",
            "cds", "álbum",
            "vinis", "álbum",
            "hqs-mangas", "mangá");

    private final MetadadosProdutoProperties properties;
    private final RestClient restClient;

    @Autowired
    public WikipediaClient(MetadadosProdutoProperties properties) {
        this(properties, null);
    }

    WikipediaClient(MetadadosProdutoProperties properties, RestClient restClient) {
        this.properties = properties;
        this.restClient = restClient;
    }

    public Optional<String> buscarResumo(String titulo, String categoriaSlug) {
        if (titulo == null || titulo.isBlank()) {
            return Optional.empty();
        }
        String tipo = categoriaSlug == null ? "" : TERMO_POR_CATEGORIA.getOrDefault(categoriaSlug, "");
        String consulta = "\"" + titulo + "\" " + tipo;
        JsonNode resposta = executar(properties.wikipediaSearchUrl()
                .replace("{q}", URLEncoder.encode(consulta.trim(), StandardCharsets.UTF_8)));
        if (resposta == null) {
            return Optional.empty();
        }
        for (JsonNode pagina : resposta.path("query").path("pages")) {
            String extrato = pagina.path("extract").asText(null);
            if (extrato != null && !extrato.isBlank()) {
                return Optional.of(extrato);
            }
        }
        return Optional.empty();
    }

    private JsonNode executar(String url) {
        try {
            return (restClient == null ? RestClient.create() : restClient).get().uri(url)
                    .header("User-Agent", properties.userAgent())
                    .retrieve().body(JsonNode.class);
        } catch (RuntimeException exception) {
            return null;
        }
    }
}
