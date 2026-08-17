package com.vitral.service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.StringJoiner;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.databind.JsonNode;
import com.vitral.config.MetadadosProdutoProperties;

@Component
public class MusicBrainzClient implements CatalogoExternoClient {

    private final MetadadosProdutoProperties properties;
    private final RestClient restClient;

    @Autowired
    public MusicBrainzClient(MetadadosProdutoProperties properties) {
        this(properties, null);
    }

    MusicBrainzClient(MetadadosProdutoProperties properties, RestClient restClient) {
        this.properties = properties;
        this.restClient = restClient;
    }

    @Override
    public List<MetadadoProduto> buscar(String termo) {
        String consulta = "release:\"" + termo.replace("\"", " ") + "\"";
        JsonNode resposta = executar(properties.musicBrainzSearchUrl()
                .replace("{q}", URLEncoder.encode(consulta, StandardCharsets.UTF_8))
                .replace("{limit}", String.valueOf(properties.limitePorFonte())));
        if (resposta == null) {
            return List.of();
        }
        List<MetadadoProduto> encontrados = new ArrayList<>();
        for (JsonNode lancamento : resposta.path("releases")) {
            String titulo = texto(lancamento.path("title"));
            if (titulo == null) {
                continue;
            }
            String artista = artista(lancamento.path("artist-credit"));
            Integer ano = ano(texto(lancamento.path("date")));
            String formato = formato(lancamento.path("media"));
            encontrados.add(new MetadadoProduto(
                    titulo,
                    artista,
                    ano,
                    descricao(titulo, artista, ano, formato),
                    categoriaSlug(formato),
                    null,
                    FONTE_MUSICBRAINZ));
        }
        return encontrados;
    }

    private String categoriaSlug(String formato) {
        if (formato == null) {
            return null;
        }
        String normalizado = formato.toLowerCase(Locale.ROOT);
        if (normalizado.contains("vinyl") || normalizado.contains("lp")) {
            return "vinis";
        }
        return normalizado.contains("cd") ? "cds" : null;
    }

    private String descricao(String titulo, String artista, Integer ano, String formato) {
        StringJoiner detalhes = new StringJoiner(", ");
        if (formato != null) {
            detalhes.add(formato);
        }
        if (ano != null) {
            detalhes.add(String.valueOf(ano));
        }
        String cabecalho = artista == null ? titulo : titulo + " - " + artista;
        return detalhes.length() == 0 ? cabecalho : cabecalho + " (" + detalhes + ")";
    }

    private String formato(JsonNode midias) {
        for (JsonNode midia : midias) {
            String formato = texto(midia.path("format"));
            if (formato != null) {
                return formato;
            }
        }
        return null;
    }

    private String artista(JsonNode creditos) {
        StringJoiner nomes = new StringJoiner(", ");
        for (JsonNode credito : creditos) {
            String nome = texto(credito.path("name"));
            if (nome != null) {
                nomes.add(nome);
            }
        }
        return nomes.length() == 0 ? null : nomes.toString();
    }

    private Integer ano(String data) {
        if (data == null || data.length() < 4 || !data.substring(0, 4).matches("\\d{4}")) {
            return null;
        }
        return Integer.valueOf(data.substring(0, 4));
    }

    private String texto(JsonNode no) {
        String valor = no.asText(null);
        return valor == null || valor.isBlank() ? null : valor;
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
