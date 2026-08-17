package com.vitral.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.lang.reflect.Field;
import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.vitral.config.RecomendacaoProperties;
import com.vitral.entity.Account;
import com.vitral.entity.BaseEntity;
import com.vitral.entity.Categoria;
import com.vitral.entity.Pedido;
import com.vitral.entity.Produto;
import com.vitral.entity.RecomendacaoEvento;
import com.vitral.entity.Sebo;
import com.vitral.enumerations.AccountType;
import com.vitral.enumerations.BookGenre;
import com.vitral.enumerations.TipoEventoRecomendacao;

@ExtendWith(MockitoExtension.class)
class RecomendacaoEventoServiceTest {
    @Mock RecomendacaoEventoPersistenceService persistence;
    private RecomendacaoEventoService service;
    private Account usuario;
    private Produto produto;

    @BeforeEach void setUp() {
        service = new RecomendacaoEventoService(persistence, new NormalizadorPesquisaService(),
                new RecomendacaoProperties(1000, 200, 20, 30, 365, 30, 3, 90, 50, 90));
        usuario = Account.builder().type(AccountType.USUARIO).build(); setId(usuario, 1L);
        Categoria categoria = Categoria.builder().nome("Livros").slug("livros").build(); setId(categoria, 2L);
        Sebo sebo = Sebo.builder().build(); setId(sebo, 3L);
        produto = Produto.builder().categoria(categoria).sebo(sebo).bookGenre(BookGenre.FICCAO)
                .autor("Clarice Lispector").preco(BigDecimal.valueOf(42)).build(); setId(produto, 4L);
    }

    @Test
    @DisplayName("Deve registrar o snapshot estruturado do produto no evento")
    void registraSnapshotEstruturadoDeProduto() {
        service.registrarProduto(usuario, TipoEventoRecomendacao.FAVORITO_ADICIONADO, produto);
        ArgumentCaptor<RecomendacaoEvento> captor = ArgumentCaptor.forClass(RecomendacaoEvento.class);
        verify(persistence).salvar(captor.capture());
        assertThat(captor.getValue()).satisfies(e -> {
            assertThat(e.getTipoProduto()).isEqualTo("livros");
            assertThat(e.getGenero()).isEqualTo("FICCAO");
            assertThat(e.getAutorArtista()).isEqualTo("clarice lispector");
            assertThat(e.getFaixaPreco()).isEqualTo("DE_25_A_50");
        });
    }

    @Test
    @DisplayName("Nao deve registrar evento para visitante nem para conta de sebo")
    void naoRegistraVisitanteNemSebo() {
        service.registrarProduto(null, TipoEventoRecomendacao.VISUALIZACAO, produto);
        Account sebo = Account.builder().type(AccountType.SEBO).build(); setId(sebo, 9L);
        service.registrarProduto(sebo, TipoEventoRecomendacao.VISUALIZACAO, produto);
        verifyNoInteractions(persistence);
    }

    @Test
    @DisplayName("Deve delegar a visualizacao com a janela e o teto diario configurados")
    void delegaVisualizacaoComJanelaETetoDiario() {
        service.registrarProduto(usuario, TipoEventoRecomendacao.VISUALIZACAO, produto);

        verify(persistence).salvarVisualizacao(any(RecomendacaoEvento.class), any(), any(), eq3());
        verify(persistence, never()).salvar(any());
    }

    @Test
    @DisplayName("Deve delegar a compra para o fluxo idempotente de persistencia")
    void delegaCompraParaFluxoIdempotente() {
        Pedido pedido = Pedido.builder().build(); setId(pedido, 8L);

        service.registrarCompra(usuario, pedido, produto);

        ArgumentCaptor<RecomendacaoEvento> captor = ArgumentCaptor.forClass(RecomendacaoEvento.class);
        verify(persistence).salvarCompra(captor.capture());
        assertThat(captor.getValue().getPedido()).isSameAs(pedido);
        verify(persistence, never()).salvar(any());
    }

    @Test
    @DisplayName("Deve normalizar o termo pesquisado e registrar os filtros aplicados")
    void normalizaPesquisaERegistraFiltros() {
        service.registrarPesquisa(usuario, "  Ficção Científica! ", produto.getCategoria(), BookGenre.FICCAO,
                BigDecimal.valueOf(20), BigDecimal.valueOf(60));
        ArgumentCaptor<RecomendacaoEvento> captor = ArgumentCaptor.forClass(RecomendacaoEvento.class);
        verify(persistence).salvarPesquisa(captor.capture(), any(), any(), eq3());
        assertThat(captor.getValue().getTermoPesquisa()).isEqualTo("ficcao cientifica");
        assertThat(captor.getValue().getFaixaPreco()).isEqualTo("DE_25_A_50");
    }

    @Test
    @DisplayName("Nao deve registrar pesquisa sem termo e sem nenhum filtro")
    void naoRegistraPesquisaSemTermoNemFiltro() {
        service.registrarPesquisa(usuario, " ", null, null, null, null);
        verifyNoInteractions(persistence);
    }

    @Test
    @DisplayName("Falha ao rastrear nao deve interromper o fluxo principal")
    void falhaDoRastreamentoNaoInterrompeFluxoPrincipal() {
        doThrow(new IllegalStateException("banco indisponivel"))
                .when(persistence).salvarVisualizacao(any(), any(), any(), anyInt());

        assertThatCode(() -> service.registrarProduto(usuario, TipoEventoRecomendacao.VISUALIZACAO, produto))
                .doesNotThrowAnyException();
    }

    private int eq3() {
        return org.mockito.ArgumentMatchers.eq(3);
    }

    private void setId(Object alvo, Long id) {
        try { Field f = BaseEntity.class.getDeclaredField("id"); f.setAccessible(true); f.set(alvo, id); }
        catch (ReflectiveOperationException e) { throw new IllegalStateException(e); }
    }
}
