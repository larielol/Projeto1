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

class MusicBrainzClientTest {

    private static final String BASE = "http://musicbrainz.test";

    private static final String BUSCA = """
            {"releases":[
              {"title":"Clube da Esquina","date":"1989-05-02",
               "artist-credit":[{"name":"Milton Nascimento"},{"name":"Lo Borges"}],
               "media":[{"format":"CD"}]},
              {"title":"Abbey Road","date":"1969",
               "artist-credit":[{"name":"The Beatles"}],
               "media":[{"format":"12\\" Vinyl"}]},
              {"title":"Sem Formato","artist-credit":[{"name":"Artista"}],"media":[{}]}
            ]}
            """;

    private MockRestServiceServer servidor;
    private MusicBrainzClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        servidor = MockRestServiceServer.bindTo(builder).ignoreExpectOrder(true).build();
        client = new MusicBrainzClient(propriedades(BASE), builder.build());
    }

    @Test
    @DisplayName("Deve classificar como CDs e extrair artista e ano a partir da data completa")
    void shouldClassifyAsCdAndExtractArtistAndYear() {
        responder(BUSCA);

        MetadadoProduto cd = client.buscar("Clube da Esquina").getFirst();

        assertThat(cd.titulo()).isEqualTo("Clube da Esquina");
        assertThat(cd.autor()).isEqualTo("Milton Nascimento, Lo Borges");
        assertThat(cd.ano()).isEqualTo(1989);
        assertThat(cd.categoriaSlug()).isEqualTo("cds");
        assertThat(cd.descricao()).isEqualTo("Clube da Esquina - Milton Nascimento, Lo Borges (CD, 1989)");
        assertThat(cd.fonte()).isEqualTo("MUSICBRAINZ");
    }

    @Test
    @DisplayName("Deve classificar como Vinis quando o formato da midia for vinil")
    void shouldClassifyAsVinylWhenMediaFormatIsVinyl() {
        responder(BUSCA);

        MetadadoProduto vinil = client.buscar("Abbey Road").get(1);

        assertThat(vinil.categoriaSlug()).isEqualTo("vinis");
        assertThat(vinil.ano()).isEqualTo(1969);
        assertThat(vinil.autor()).isEqualTo("The Beatles");
    }

    @Test
    @DisplayName("Deve deixar a categoria em branco quando o formato da midia for desconhecido")
    void shouldLeaveCategoryBlankWhenMediaFormatIsUnknown() {
        responder(BUSCA);

        MetadadoProduto semFormato = client.buscar("Sem Formato").get(2);

        assertThat(semFormato.categoriaSlug()).isNull();
        assertThat(semFormato.ano()).isNull();
        assertThat(semFormato.descricao()).isEqualTo("Sem Formato - Artista");
    }

    @Test
    @DisplayName("Deve ignorar lancamentos sem titulo retornados pelo provedor")
    void shouldSkipReleasesWithoutTitle() {
        responder("{\"releases\":[{\"date\":\"1990\"}]}");

        assertThat(client.buscar("qualquer")).isEmpty();
    }

    @Test
    @DisplayName("Deve retornar lista vazia quando o provedor responder com erro")
    void shouldReturnEmptyListWhenProviderFails() {
        servidor.expect(requestTo(containsString("/release"))).andRespond(withServerError());

        assertThat(client.buscar("Clube da Esquina")).isEmpty();
    }

    private void responder(String corpo) {
        servidor.expect(requestTo(containsString("/release")))
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
