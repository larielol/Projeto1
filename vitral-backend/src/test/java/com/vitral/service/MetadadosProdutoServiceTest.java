package com.vitral.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import com.vitral.config.MetadadosProdutoProperties;
import com.vitral.dto.SugestaoProdutoResponse;
import com.vitral.entity.Categoria;
import com.vitral.exception.BusinessException;
import com.vitral.repository.CategoriaRepository;
import com.vitral.service.CatalogoExternoClient.MetadadoProduto;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MetadadosProdutoServiceTest {

    private static final MetadadosProdutoProperties HABILITADO = new MetadadosProdutoProperties(
            true, "VitralApp/1.0", 3, "s", "w", "c", "m", "wiki");

    @Mock
    private OpenLibraryClient openLibraryClient;

    @Mock
    private MusicBrainzClient musicBrainzClient;

    @Mock
    private WikipediaClient wikipediaClient;

    @Mock
    private CategoriaRepository categoriaRepository;

    @InjectMocks
    private MetadadosProdutoService service;

    @Test
    @DisplayName("Deve resolver a categoria oficial a partir do slug sugerido pelo provedor")
    void shouldResolveOfficialCategoryFromSuggestedSlug() {
        habilitar();
        when(categoriaRepository.findAll()).thenReturn(List.of(categoria(12L, "Livros", "livros")));
        when(openLibraryClient.buscar("dom casmurro")).thenReturn(List.of(
                new MetadadoProduto("Dom Casmurro", "Machado", 1900, "Romance", "livros", "capa.jpg",
                        "OPEN_LIBRARY")));
        when(musicBrainzClient.buscar("dom casmurro")).thenReturn(List.of());

        List<SugestaoProdutoResponse> resultado = service.sugerir("dom casmurro");

        assertThat(resultado).hasSize(1);
        SugestaoProdutoResponse sugestao = resultado.getFirst();
        assertThat(sugestao.categoriaId()).isEqualTo(12L);
        assertThat(sugestao.categoriaNome()).isEqualTo("Livros");
        assertThat(sugestao.categoriaSlug()).isEqualTo("livros");
        assertThat(sugestao.ano()).isEqualTo(1900);
        assertThat(sugestao.fonte()).isEqualTo("OPEN_LIBRARY");
    }

    @Test
    @DisplayName("Deve deixar a categoria nula quando o provedor nao identificar o formato")
    void shouldLeaveCategoryNullWhenProviderCannotIdentifyFormat() {
        habilitar();
        when(categoriaRepository.findAll()).thenReturn(List.of(categoria(13L, "CDs", "cds")));
        when(openLibraryClient.buscar("algo raro")).thenReturn(List.of());
        when(musicBrainzClient.buscar("algo raro")).thenReturn(List.of(
                new MetadadoProduto("Algo Raro", "Artista", null, "desc", null, null, "MUSICBRAINZ")));

        SugestaoProdutoResponse sugestao = service.sugerir("algo raro").getFirst();

        assertThat(sugestao.categoriaId()).isNull();
        assertThat(sugestao.categoriaSlug()).isNull();
        assertThat(sugestao.categoriaNome()).isNull();
    }

    @Test
    @DisplayName("Deve ordenar as sugestoes mais completas antes das incompletas")
    void shouldSortMoreCompleteSuggestionsFirst() {
        habilitar();
        when(categoriaRepository.findAll()).thenReturn(List.of(categoria(12L, "Livros", "livros")));
        when(openLibraryClient.buscar("titulo")).thenReturn(List.of(
                new MetadadoProduto("Incompleto", null, null, null, null, null, "OPEN_LIBRARY"),
                new MetadadoProduto("Completo", "Autor", 2000, "Descricao", "livros", "capa.jpg", "OPEN_LIBRARY")));
        when(musicBrainzClient.buscar("titulo")).thenReturn(List.of());

        List<SugestaoProdutoResponse> resultado = service.sugerir("titulo");

        assertThat(resultado).extracting(SugestaoProdutoResponse::titulo)
                .containsExactly("Completo", "Incompleto");
    }

    @Test
    @DisplayName("Deve encurtar o resumo longo cortando no fim da ultima frase completa")
    void shouldShortenLongSummaryAtLastCompleteSentence() {
        habilitar();
        when(categoriaRepository.findAll()).thenReturn(List.of(categoria(12L, "Livros", "livros")));
        String primeiraFrase = "Romance de Machado de Assis publicado em 1899 e ambientado no Rio de Janeiro.";
        when(openLibraryClient.buscar("longo")).thenReturn(List.of(
                new MetadadoProduto("Titulo", "Autor", 2000, primeiraFrase + " " + "palavra ".repeat(200),
                        "livros", null, "OPEN_LIBRARY")));
        when(musicBrainzClient.buscar("longo")).thenReturn(List.of());

        SugestaoProdutoResponse sugestao = service.sugerir("longo").getFirst();

        assertThat(sugestao.descricao()).startsWith(primeiraFrase).hasSizeLessThanOrEqualTo(320);
    }

    @Test
    @DisplayName("Deve remover a marcacao markdown e o excesso de espacos do resumo")
    void shouldRemoveMarkdownAndExtraSpacesFromSummary() {
        habilitar();
        when(categoriaRepository.findAll()).thenReturn(List.of(categoria(12L, "Livros", "livros")));
        when(openLibraryClient.buscar("marcado")).thenReturn(List.of(
                new MetadadoProduto("Titulo", "Autor", 2000, "**Dom Casmurro**  e   um romance.", "livros", null,
                        "OPEN_LIBRARY")));
        when(musicBrainzClient.buscar("marcado")).thenReturn(List.of());

        assertThat(service.sugerir("marcado").getFirst().descricao()).isEqualTo("Dom Casmurro e um romance.");
    }

    @Test
    @DisplayName("Deve usar o resumo da Wikipedia quando o provedor de musica nao tiver sinopse")
    void shouldUseWikipediaSummaryForMusicSuggestions() {
        habilitar();
        when(categoriaRepository.findAll()).thenReturn(List.of(categoria(14L, "Vinis", "vinis")));
        when(openLibraryClient.buscar("clube da esquina")).thenReturn(List.of());
        when(musicBrainzClient.buscar("clube da esquina")).thenReturn(List.of(
                new MetadadoProduto("Clube da Esquina", "Milton Nascimento", 1972,
                        "Clube da Esquina - Milton Nascimento (Vinyl, 1972)", "vinis", null, "MUSICBRAINZ")));
        when(wikipediaClient.buscarResumo("Clube da Esquina", "vinis"))
                .thenReturn(java.util.Optional.of("Album colaborativo de Milton Nascimento e Lo Borges."));

        SugestaoProdutoResponse sugestao = service.sugerir("clube da esquina").getFirst();

        assertThat(sugestao.descricao()).isEqualTo("Album colaborativo de Milton Nascimento e Lo Borges.");
    }

    @Test
    @DisplayName("Deve consultar a Wikipedia uma unica vez para lancamentos repetidos do mesmo titulo")
    void shouldQueryWikipediaOnlyOncePerRepeatedTitle() {
        habilitar();
        when(categoriaRepository.findAll()).thenReturn(List.of(categoria(13L, "CDs", "cds")));
        when(openLibraryClient.buscar("repetido")).thenReturn(List.of());
        when(musicBrainzClient.buscar("repetido")).thenReturn(List.of(
                new MetadadoProduto("Repetido", "Artista", 1990, "linha", "cds", null, "MUSICBRAINZ"),
                new MetadadoProduto("Repetido", "Artista", 1995, "linha", "cds", null, "MUSICBRAINZ")));
        when(wikipediaClient.buscarResumo("Repetido", "cds")).thenReturn(java.util.Optional.of("Resumo unico."));

        assertThat(service.sugerir("repetido")).hasSize(2)
                .allSatisfy(sugestao -> assertThat(sugestao.descricao()).isEqualTo("Resumo unico."));
        verify(wikipediaClient).buscarResumo("Repetido", "cds");
    }

    @Test
    @DisplayName("Deve manter a linha factual do provedor quando a Wikipedia nao tiver resumo")
    void shouldKeepProviderFactualLineWhenWikipediaHasNoSummary() {
        habilitar();
        when(categoriaRepository.findAll()).thenReturn(List.of(categoria(13L, "CDs", "cds")));
        when(openLibraryClient.buscar("sem wiki")).thenReturn(List.of());
        when(musicBrainzClient.buscar("sem wiki")).thenReturn(List.of(
                new MetadadoProduto("Sem Wiki", "Artista", 1990, "Sem Wiki - Artista (CD, 1990)", "cds", null,
                        "MUSICBRAINZ")));
        when(wikipediaClient.buscarResumo("Sem Wiki", "cds")).thenReturn(java.util.Optional.empty());

        assertThat(service.sugerir("sem wiki").getFirst().descricao())
                .isEqualTo("Sem Wiki - Artista (CD, 1990)");
    }

    @Test
    @DisplayName("Deve ignorar categorias fora da lista oficial do sebo")
    void shouldIgnoreCategoriesOutsideOfficialList() {
        habilitar();
        when(categoriaRepository.findAll()).thenReturn(List.of(categoria(5L, "Historia", "historia")));
        when(openLibraryClient.buscar("historia")).thenReturn(List.of(
                new MetadadoProduto("Titulo", "Autor", 2000, "desc", "historia", null, "OPEN_LIBRARY")));
        when(musicBrainzClient.buscar("historia")).thenReturn(List.of());

        assertThat(service.sugerir("historia").getFirst().categoriaId()).isNull();
    }

    @Test
    @DisplayName("Deve retornar lista vazia sem consultar provedores quando a funcionalidade estiver desabilitada")
    void shouldReturnEmptyListWithoutCallingProvidersWhenDisabled() {
        ReflectionTestUtils.setField(service, "properties",
                new MetadadosProdutoProperties(false, "VitralApp/1.0", 3, "s", "w", "c", "m", "wiki"));

        assertThat(service.sugerir("dom casmurro")).isEmpty();
        verify(openLibraryClient, never()).buscar("dom casmurro");
        verify(musicBrainzClient, never()).buscar("dom casmurro");
    }

    @Test
    @DisplayName("Deve lancar BusinessException 400 quando o termo tiver menos de tres caracteres")
    void shouldThrowBadRequestWhenTermIsTooShort() {
        habilitar();

        assertThatThrownBy(() -> service.sugerir("ab"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("ao menos 3 caracteres")
                .satisfies(excecao -> assertThat(((BusinessException) excecao).getStatus())
                        .isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    @DisplayName("Deve lancar BusinessException quando o termo for nulo")
    void shouldThrowBadRequestWhenTermIsNull() {
        habilitar();

        assertThatThrownBy(() -> service.sugerir(null)).isInstanceOf(BusinessException.class);
    }

    private void habilitar() {
        ReflectionTestUtils.setField(service, "properties", HABILITADO);
    }

    private Categoria categoria(Long id, String nome, String slug) {
        Categoria categoria = Categoria.builder().nome(nome).slug(slug).build();
        ReflectionTestUtils.setField(categoria, "id", id);
        return categoria;
    }
}
