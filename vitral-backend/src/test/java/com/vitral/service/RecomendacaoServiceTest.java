package com.vitral.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import com.vitral.config.RecomendacaoProperties;
import com.vitral.dto.ProdutoResponse;
import com.vitral.entity.Account;
import com.vitral.entity.BaseEntity;
import com.vitral.entity.Categoria;
import com.vitral.entity.Produto;
import com.vitral.entity.RecomendacaoEvento;
import com.vitral.entity.Sebo;
import com.vitral.enumerations.AccountType;
import com.vitral.enumerations.CondicaoProduto;
import com.vitral.enumerations.TipoEventoRecomendacao;
import com.vitral.exception.BusinessException;
import com.vitral.mapper.ProdutoMapper;
import com.vitral.repository.ProdutoRepository;
import com.vitral.repository.RecomendacaoEventoRepository;

@ExtendWith(MockitoExtension.class)
class RecomendacaoServiceTest {
    @Mock RecomendacaoEventoRepository eventoRepository;
    @Mock ProdutoRepository produtoRepository;
    @Mock ProdutoMapper produtoMapper;
    @Mock PrecoService precoService;
    private RecomendacaoService service;
    private Account usuario;
    private Categoria livros;
    private Categoria vinis;

    @BeforeEach void setUp() {
        service = new RecomendacaoService(eventoRepository, produtoRepository, produtoMapper, precoService,
                new RecenciaRecomendacaoService(), new RecomendacaoProperties(1000, 200, 20, 30, 365, 30, 3, 90, 50, 90));
        usuario = Account.builder().type(AccountType.USUARIO).build(); set(usuario, "id", 1L);
        livros = Categoria.builder().nome("Livros").slug("livros").build(); set(livros, "id", 10L);
        vinis = Categoria.builder().nome("Vinis").slug("vinis").build(); set(vinis, "id", 20L);
        lenient().when(eventoRepository.contagemPorProdutoETipo(any())).thenReturn(List.of());
        lenient().when(precoService.precosPromocionaisVigentes(any())).thenReturn(Map.of());
        lenient().when(produtoMapper.toResponse(any(), any())).thenAnswer(i -> response(i.getArgument(0)));
    }

    @Test void interesseDeCategoriaOrdenaProdutoRelacionadoPrimeiro() {
        Produto livro = produto(1L, livros, 101L);
        Produto vinil = produto(2L, vinis, 102L);
        RecomendacaoEvento evento = RecomendacaoEvento.builder().account(usuario)
                .tipo(TipoEventoRecomendacao.COMPRA_CONCLUIDA).categoria(livros).tipoProduto("livros").build();
        set(evento, "createdAt", OffsetDateTime.now().minusDays(1));
        when(eventoRepository.findByAccountIdAndCreatedAtAfterOrderByCreatedAtDesc(any(), any(), any()))
                .thenReturn(List.of(evento));
        when(produtoRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(vinil, livro)));

        var resultado = service.recomendar(usuario, PageRequest.of(0, 10), Set.of());

        assertThat(resultado.getContent()).extracting(ProdutoResponse::id).containsExactly(1L, 2L);
    }

    @Test void usuarioSemHistoricoRecebeInicioSemDados() {
        Produto livro = produto(1L, livros, 101L);
        when(eventoRepository.findByAccountIdAndCreatedAtAfterOrderByCreatedAtDesc(any(), any(), any()))
                .thenReturn(List.of());
        when(produtoRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(livro)));
        assertThat(service.recomendar(usuario, PageRequest.of(0, 10), Set.of()).getContent()).hasSize(1);
    }

    @Test void resultadoNaoDuplicaProdutos() {
        Produto livro = produto(1L, livros, 101L);
        when(eventoRepository.findByAccountIdAndCreatedAtAfterOrderByCreatedAtDesc(any(), any(), any())).thenReturn(List.of());
        when(produtoRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(livro)));
        assertThat(service.recomendar(usuario, PageRequest.of(0, 10), Set.of()).getContent())
                .extracting(ProdutoResponse::id).doesNotHaveDuplicates();
    }

    @Test void naoRecomendaProdutoCompradoRecentemente() {
        Produto livro = produto(1L, livros, 101L);
        RecomendacaoEvento compra = RecomendacaoEvento.builder().account(usuario)
                .tipo(TipoEventoRecomendacao.COMPRA_CONCLUIDA).categoria(livros).produto(livro).build();
        set(compra, "createdAt", OffsetDateTime.now().minusDays(1));
        when(eventoRepository.findByAccountIdAndCreatedAtAfterOrderByCreatedAtDesc(any(), any(), any()))
                .thenReturn(List.of(compra));
        when(produtoRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of()));

        service.recomendar(usuario, PageRequest.of(0, 10), Set.of());

        ArgumentCaptor<org.springframework.data.jpa.domain.Specification<Produto>> captor =
                ArgumentCaptor.forClass(org.springframework.data.jpa.domain.Specification.class);
        verify(produtoRepository, atLeastOnce()).findAll(captor.capture(), any(PageRequest.class));
        assertThat(captor.getAllValues()).isNotEmpty();
    }

    @Test void buscaCandidatosTambemNasCategoriasDeInteresse() {
        Produto livro = produto(1L, livros, 101L);
        RecomendacaoEvento evento = RecomendacaoEvento.builder().account(usuario)
                .tipo(TipoEventoRecomendacao.COMPRA_CONCLUIDA).categoria(livros).tipoProduto("livros").build();
        set(evento, "createdAt", OffsetDateTime.now().minusDays(1));
        when(eventoRepository.findByAccountIdAndCreatedAtAfterOrderByCreatedAtDesc(any(), any(), any()))
                .thenReturn(List.of(evento));
        when(produtoRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(livro)));

        service.recomendar(usuario, PageRequest.of(0, 10), Set.of());

        verify(produtoRepository, org.mockito.Mockito.times(2))
                .findAll(any(org.springframework.data.jpa.domain.Specification.class), any(PageRequest.class));
    }

    @Test void popularidadeUsaOPesoDefinidoNoEnumDeEvento() {
        Produto popular = produto(1L, livros, 101L);
        Produto comum = produto(2L, livros, 102L);
        when(eventoRepository.findByAccountIdAndCreatedAtAfterOrderByCreatedAtDesc(any(), any(), any()))
                .thenReturn(List.of());
        when(eventoRepository.contagemPorProdutoETipo(any()))
                .thenReturn(List.of(contagem(1L, TipoEventoRecomendacao.COMPRA_CONCLUIDA, 2L)));
        when(produtoRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(comum, popular)));

        assertThat(service.recomendar(usuario, PageRequest.of(0, 10), Set.of()).getContent())
                .extracting(ProdutoResponse::id).containsExactly(1L, 2L);
    }

    private com.vitral.repository.ProdutoEventoContagemProjection contagem(Long produtoId,
            TipoEventoRecomendacao tipo, long quantidade) {
        return new com.vitral.repository.ProdutoEventoContagemProjection() {
            @Override public Long getProdutoId() { return produtoId; }
            @Override public TipoEventoRecomendacao getTipo() { return tipo; }
            @Override public long getQuantidade() { return quantidade; }
        };
    }

    @Test void rejeitaPaginaAcimaDoLimiteConfigurado() {
        assertThatThrownBy(() -> service.recomendar(usuario, PageRequest.of(0, 51), Set.of()))
                .isInstanceOf(BusinessException.class).hasMessageContaining("50");
    }

    private Produto produto(Long id, Categoria categoria, Long seboId) {
        Account conta = Account.builder().ativo(true).build();
        Sebo sebo = Sebo.builder().account(conta).build(); set(sebo, "id", seboId);
        Produto p = Produto.builder().sebo(sebo).categoria(categoria).titulo("Produto " + id)
                .preco(BigDecimal.TEN).estoque(1).condicao(CondicaoProduto.NOVO).ativo(true).build();
        set(p, "id", id); set(p, "createdAt", OffsetDateTime.now()); return p;
    }

    private ProdutoResponse response(Produto p) {
        return new ProdutoResponse(p.getId(), p.getSebo().getId(), "Sebo", p.getCategoria().getId(),
                p.getCategoria().getNome(), null, p.getTitulo(), null, null, null, p.getPreco(), null,
                1, CondicaoProduto.NOVO, null, true, true, false, false, null);
    }

    private void set(Object alvo, String campo, Object valor) {
        try { Field f = BaseEntity.class.getDeclaredField(campo); f.setAccessible(true); f.set(alvo, valor); }
        catch (ReflectiveOperationException e) { throw new IllegalStateException(e); }
    }
}
