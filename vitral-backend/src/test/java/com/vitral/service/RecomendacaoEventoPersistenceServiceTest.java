package com.vitral.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.time.OffsetDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.vitral.entity.Account;
import com.vitral.entity.BaseEntity;
import com.vitral.entity.Pedido;
import com.vitral.entity.Produto;
import com.vitral.entity.RecomendacaoEvento;
import com.vitral.enumerations.TipoEventoRecomendacao;
import com.vitral.repository.RecomendacaoEventoRepository;

@ExtendWith(MockitoExtension.class)
class RecomendacaoEventoPersistenceServiceTest {

    private static final OffsetDateTime JANELA = OffsetDateTime.now().minusMinutes(30);
    private static final OffsetDateTime INICIO_DO_DIA = OffsetDateTime.now().toLocalDate()
            .atStartOfDay().atOffset(OffsetDateTime.now().getOffset());

    @Mock
    private RecomendacaoEventoRepository repository;

    @InjectMocks
    private RecomendacaoEventoPersistenceService service;

    private Account usuario;
    private Produto produto;

    @BeforeEach
    void setUp() {
        usuario = Account.builder().build();
        setId(usuario, 1L);
        produto = Produto.builder().build();
        setId(produto, 4L);
    }

    @Test
    @DisplayName("Nao deve salvar visualizacao repetida dentro da janela configurada")
    void naoSalvaVisualizacaoRepetidaNaJanela() {
        when(repository.existsByAccountIdAndProdutoIdAndTipoAndCreatedAtAfter(
                1L, 4L, TipoEventoRecomendacao.VISUALIZACAO, JANELA)).thenReturn(true);

        service.salvarVisualizacao(visualizacao(), JANELA, INICIO_DO_DIA, 3);

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Nao deve salvar visualizacao que exceda o teto diario do mesmo produto")
    void naoSalvaVisualizacaoAcimaDoTetoDiario() {
        when(repository.existsByAccountIdAndProdutoIdAndTipoAndCreatedAtAfter(
                1L, 4L, TipoEventoRecomendacao.VISUALIZACAO, JANELA)).thenReturn(false);
        when(repository.countByAccountIdAndProdutoIdAndTipoAndCreatedAtAfter(
                1L, 4L, TipoEventoRecomendacao.VISUALIZACAO, INICIO_DO_DIA)).thenReturn(3L);

        service.salvarVisualizacao(visualizacao(), JANELA, INICIO_DO_DIA, 3);

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Deve salvar visualizacao quando estiver dentro dos limites")
    void salvaVisualizacaoDentroDosLimites() {
        RecomendacaoEvento evento = visualizacao();
        when(repository.existsByAccountIdAndProdutoIdAndTipoAndCreatedAtAfter(
                1L, 4L, TipoEventoRecomendacao.VISUALIZACAO, JANELA)).thenReturn(false);
        when(repository.countByAccountIdAndProdutoIdAndTipoAndCreatedAtAfter(
                1L, 4L, TipoEventoRecomendacao.VISUALIZACAO, INICIO_DO_DIA)).thenReturn(1L);

        service.salvarVisualizacao(evento, JANELA, INICIO_DO_DIA, 3);

        verify(repository).save(evento);
    }

    @Test
    @DisplayName("Nao deve salvar pesquisa igual repetida dentro da janela configurada")
    void naoSalvaPesquisaRepetidaNaJanela() {
        when(repository.existsByAccountIdAndTipoAndTermoPesquisaAndCreatedAtAfter(
                1L, TipoEventoRecomendacao.PESQUISA, "ficcao", JANELA)).thenReturn(true);

        service.salvarPesquisa(pesquisa("ficcao"), JANELA, INICIO_DO_DIA, 3);

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Deve salvar pesquisa sem termo textual sem consultar a deduplicacao")
    void salvaPesquisaSemTermoTextual() {
        RecomendacaoEvento evento = pesquisa(null);

        service.salvarPesquisa(evento, JANELA, INICIO_DO_DIA, 3);

        verify(repository).save(evento);
    }

    @Test
    @DisplayName("Nao deve salvar compra ja registrada para o mesmo pedido e produto")
    void naoSalvaCompraJaRegistrada() {
        when(repository.existsByPedidoIdAndProdutoIdAndTipo(8L, 4L, TipoEventoRecomendacao.COMPRA_CONCLUIDA))
                .thenReturn(true);

        service.salvarCompra(compra());

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Deve salvar a compra na primeira vez que o par pedido e produto aparecer")
    void salvaCompraNaPrimeiraOcorrencia() {
        RecomendacaoEvento evento = compra();
        when(repository.existsByPedidoIdAndProdutoIdAndTipo(8L, 4L, TipoEventoRecomendacao.COMPRA_CONCLUIDA))
                .thenReturn(false);

        service.salvarCompra(evento);

        verify(repository).save(evento);
    }

    private RecomendacaoEvento visualizacao() {
        return RecomendacaoEvento.builder().account(usuario).produto(produto)
                .tipo(TipoEventoRecomendacao.VISUALIZACAO).build();
    }

    private RecomendacaoEvento pesquisa(String termo) {
        return RecomendacaoEvento.builder().account(usuario).tipo(TipoEventoRecomendacao.PESQUISA)
                .termoPesquisa(termo).build();
    }

    private RecomendacaoEvento compra() {
        Pedido pedido = Pedido.builder().build();
        setId(pedido, 8L);
        return RecomendacaoEvento.builder().account(usuario).produto(produto).pedido(pedido)
                .tipo(TipoEventoRecomendacao.COMPRA_CONCLUIDA).build();
    }

    private void setId(Object alvo, Long id) {
        try {
            Field campo = BaseEntity.class.getDeclaredField("id");
            campo.setAccessible(true);
            campo.set(alvo, id);
        } catch (ReflectiveOperationException excecao) {
            throw new IllegalStateException(excecao);
        }
    }
}
