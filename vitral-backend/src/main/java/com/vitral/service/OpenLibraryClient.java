package com.vitral.service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.databind.JsonNode;
import com.vitral.config.MetadadosProdutoProperties;

@Component
public class OpenLibraryClient implements CatalogoExternoClient {

    private static final List<String> TERMOS_HQ = List.of("manga", "comic", "graphic novel", "cartoon");

    private final MetadadosProdutoProperties properties;
    private final RestClient restClient;

    @Autowired
    public OpenLibraryClient(MetadadosProdutoProperties properties) {
        this(properties, null);
    }

    OpenLibraryClient(MetadadosProdutoProperties properties, RestClient restClient) {
        this.properties = properties;
        this.restClient = restClient;
    }

    @Override
    public List<MetadadoProduto> buscar(String termo) {
        JsonNode resposta = executar(properties.openLibrarySearchUrl()
                .replace("{q}", URLEncoder.encode(termo, StandardCharsets.UTF_8))
                .replace("{limit}", String.valueOf(properties.limitePorFonte())));
        if (resposta == null) {
            return List.of();
        }
        JsonNode documentos = resposta.path("docs");
        List<MetadadoProduto> encontrados = new ArrayList<>();
        for (JsonNode documento : documentos) {
            String titulo = texto(documento.path("title"));
            if (titulo == null) {
                continue;
            }
            encontrados.add(new MetadadoProduto(
                    titulo,
                    primeiroAutor(documento.path("author_name")),
                    inteiro(documento.path("first_publish_year")),
                    descricao(texto(documento.path("key"))),
                    categoriaSlug(documento.path("subject")),
                    capa(documento.path("cover_i")),
                    FONTE_OPEN_LIBRARY));
        }
        return encontrados;
    }

    private String descricao(String chaveObra) {
        if (chaveObra == null) {
            return null;
        }
        JsonNode obra = executar(properties.openLibraryWorkUrl().replace("{key}", chaveObra));
        if (obra == null) {
            return null;
        }
        JsonNode descricao = obra.path("description");
        return descricao.isTextual() ? descricao.asText() : texto(descricao.path("value"));
    }

    private String categoriaSlug(JsonNode assuntos) {
        for (JsonNode assunto : assuntos) {
            String valor = assunto.asText("").toLowerCase(Locale.ROOT);
            if (TERMOS_HQ.stream().anyMatch(valor::contains)) {
                return "hqs-mangas";
            }
        }
        return "livros";
    }

    private String capa(JsonNode identificador) {
        if (!identificador.isNumber()) {
            return null;
        }
        return properties.openLibraryCoverUrl().replace("{id}", identificador.asText());
    }

    private String primeiroAutor(JsonNode autores) {
        return autores.isArray() && !autores.isEmpty() ? texto(autores.get(0)) : null;
    }

    private Integer inteiro(JsonNode no) {
        return no.isNumber() ? no.asInt() : null;
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
