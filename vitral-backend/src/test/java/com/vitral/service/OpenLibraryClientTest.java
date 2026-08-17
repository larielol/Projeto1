package com.vitral.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.vitral.config.MetadadosProdutoProperties;
import com.vitral.service.CatalogoExternoClient.MetadadoProduto;

class OpenLibraryClientTest {

    private static final String BASE = "http://openlibrary.test";

    private static final String BUSCA = """
            {"docs":[
              {"key":"/works/OL1W","title":"Dom Casmurro","author_name":["Machado de Assis"],
               "first_publish_year":1900,"cover_i":647501,"subject":["Fiction","Adultery"]},
              {"key":"/works/OL2W","title":"Naruto 33","author_name":["Masashi Kishimoto"],
               "first_publish_year":2006,"subject":["Graphic novels","Manga"]}
            ]}
            """;

    private MockRestServiceServer servidor;
    private OpenLibraryClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        servidor = MockRestServiceServer.bindTo(builder).ignoreExpectOrder(true).build();
        client = new OpenLibraryClient(propriedades(BASE), builder.build());
    }

    @Test
    @DisplayName("Deve mapear livro com autor, ano, capa e descricao vindos da Open Library")
    void shouldMapBookFromOpenLibrary() {
        responder("/search.json", BUSCA);
        responder("/works/OL1W.json", "{\"description\":\"Romance de Machado.\"}");
        responder("/works/OL2W.json", "{\"description\":{\"value\":\"Manga ninja.\"}}");

        List<MetadadoProduto> resultado = client.buscar("dom casmurro");

        assertThat(resultado).hasSize(2);
        MetadadoProduto livro = resultado.getFirst();
        assertThat(livro.titulo()).isEqualTo("Dom Casmurro");
        assertThat(livro.autor()).isEqualTo("Machado de Assis");
        assertThat(livro.ano()).isEqualTo(1900);
        assertThat(livro.descricao()).isEqualTo("Romance de Machado.");
        assertThat(livro.categoriaSlug()).isEqualTo("livros");
        assertThat(livro.fotoUrl()).isEqualTo("https://covers.openlibrary.org/b/id/647501-L.jpg");
        assertThat(livro.fonte()).isEqualTo("OPEN_LIBRARY");
    }

    @Test
    @DisplayName("Deve classificar como HQs / Mangas quando os assuntos indicarem quadrinhos")
    void shouldClassifyAsComicsWhenSubjectsIndicateGraphicNovel() {
        responder("/search.json", BUSCA);
        responder("/works/OL1W.json", "{}");
        responder("/works/OL2W.json", "{\"description\":{\"value\":\"Manga ninja.\"}}");

        MetadadoProduto manga = client.buscar("naruto").get(1);

        assertThat(manga.categoriaSlug()).isEqualTo("hqs-mangas");
        assertThat(manga.descricao()).isEqualTo("Manga ninja.");
        assertThat(manga.fotoUrl()).isNull();
    }

    @Test
    @DisplayName("Deve manter o produto sem descricao quando a obra nao tiver resumo cadastrado")
    void shouldKeepProductWithoutDescriptionWhenWorkHasNoSummary() {
        responder("/search.json", BUSCA);
        responder("/works/OL1W.json", "{}");
        responder("/works/OL2W.json", "{}");

        assertThat(client.buscar("dom casmurro").getFirst().descricao()).isNull();
    }

    @Test
    @DisplayName("Deve ignorar documentos sem titulo retornados pelo provedor")
    void shouldSkipDocumentsWithoutTitle() {
        responder("/search.json", "{\"docs\":[{\"key\":\"/works/OL9W\"}]}");

        assertThat(client.buscar("qualquer")).isEmpty();
    }

    @Test
    @DisplayName("Deve retornar lista vazia quando o provedor responder com erro")
    void shouldReturnEmptyListWhenProviderFails() {
        servidor.expect(requestTo(containsString("/search.json"))).andRespond(withServerError());

        assertThat(client.buscar("dom casmurro")).isEmpty();
    }

    private void responder(String trecho, String corpo) {
        servidor.expect(requestTo(containsString(trecho)))
                .andRespond(withSuccess(corpo, MediaType.APPLICATION_JSON));
    }

    private MetadadosProdutoProperties propriedades(String base) {
        return new MetadadosProdutoProperties(true, "VitralApp/1.0", 3,
                base + "/search.json?q={q}&limit={limit}",
                base + "{key}.json",
                "https://covers.openlibrary.org/b/id/{id}-L.jpg",
                base + "/release?query={q}&limit={limit}",
                base + "/wiki?gsrsearch={q}");
    }
}
