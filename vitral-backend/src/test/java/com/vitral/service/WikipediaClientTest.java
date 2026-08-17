package com.vitral.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.vitral.config.MetadadosProdutoProperties;

class WikipediaClientTest {

    private static final String BASE = "http://wikipedia.test";

    private static final String RESPOSTA = """
            {"query":{"pages":{"123":{"title":"Abbey Road",
              "extract":"Abbey Road e o decimo primeiro album de estudio dos Beatles."}}}}
            """;

    private MockRestServiceServer servidor;
    private WikipediaClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        servidor = MockRestServiceServer.bindTo(builder).ignoreExpectOrder(true).build();
        client = new WikipediaClient(propriedades(), builder.build());
    }

    @Test
    @DisplayName("Deve retornar o resumo da pagina encontrada para a categoria informada")
    void shouldReturnSummaryOfFoundPage() {
        responder(RESPOSTA);

        Optional<String> resumo = client.buscarResumo("Abbey Road", "vinis");

        assertThat(resumo).contains("Abbey Road e o decimo primeiro album de estudio dos Beatles.");
    }

    @Test
    @DisplayName("Deve buscar o resumo mesmo quando a categoria for nula, sem lancar excecao")
    void shouldSearchSummaryWhenCategoryIsNull() {
        responder(RESPOSTA);

        Optional<String> resumo = client.buscarResumo("Abbey Road", null);

        assertThat(resumo).contains("Abbey Road e o decimo primeiro album de estudio dos Beatles.");
    }

    @Test
    @DisplayName("Deve buscar o resumo quando a categoria nao tiver termo de busca mapeado")
    void shouldSearchSummaryWhenCategoryHasNoMappedTerm() {
        responder(RESPOSTA);

        assertThat(client.buscarResumo("Abbey Road", "categoria-desconhecida")).isPresent();
    }

    @Test
    @DisplayName("Deve retornar vazio quando o titulo for nulo ou em branco")
    void shouldReturnEmptyWhenTitleIsNullOrBlank() {
        assertThat(client.buscarResumo(null, "livros")).isEmpty();
        assertThat(client.buscarResumo("   ", "livros")).isEmpty();
    }

    @Test
    @DisplayName("Deve retornar vazio quando a pagina encontrada nao tiver resumo")
    void shouldReturnEmptyWhenPageHasNoExtract() {
        responder("{\"query\":{\"pages\":{\"123\":{\"title\":\"Sem resumo\",\"extract\":\"\"}}}}");

        assertThat(client.buscarResumo("Sem resumo", "livros")).isEmpty();
    }

    @Test
    @DisplayName("Deve retornar vazio quando a busca nao encontrar nenhuma pagina")
    void shouldReturnEmptyWhenSearchFindsNoPage() {
        responder("{\"batchcomplete\":\"\"}");

        assertThat(client.buscarResumo("Inexistente", "livros")).isEmpty();
    }

    @Test
    @DisplayName("Deve retornar vazio quando o provedor responder com erro")
    void shouldReturnEmptyWhenProviderFails() {
        servidor.expect(requestTo(containsString("/wiki"))).andRespond(withServerError());

        assertThat(client.buscarResumo("Abbey Road", "vinis")).isEmpty();
    }

    private void responder(String corpo) {
        servidor.expect(requestTo(containsString("/wiki")))
                .andRespond(withSuccess(corpo, MediaType.APPLICATION_JSON));
    }

    private MetadadosProdutoProperties propriedades() {
        return new MetadadosProdutoProperties(true, "VitralApp/1.0", 3,
                BASE + "/search.json?q={q}&limit={limit}",
                BASE + "{key}.json",
                "https://covers.openlibrary.org/b/id/{id}-L.jpg",
                BASE + "/release?query={q}&limit={limit}",
                BASE + "/wiki?gsrsearch={q}");
    }
}
