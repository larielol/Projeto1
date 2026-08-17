package com.vitral.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import com.vitral.dto.ProdutoResponse;
import com.vitral.entity.Account;
import com.vitral.entity.BaseEntity;
import com.vitral.entity.Categoria;
import com.vitral.entity.Produto;
import com.vitral.entity.Sebo;
import com.vitral.enumerations.AccountType;
import com.vitral.enumerations.CondicaoProduto;
import com.vitral.mapper.ProdutoMapper;
import com.vitral.repository.CategoriaRepository;
import com.vitral.repository.ProdutoRepository;

@ExtendWith(MockitoExtension.class)
class HomeServiceTest {

    @Mock ProdutoRepository produtoRepository;
    @Mock CategoriaRepository categoriaRepository;
    @Mock RecomendacaoService recomendacaoService;
    @Mock ProdutoMapper produtoMapper;
    @Mock PrecoService precoService;
    @Mock SeboService seboService;
    HomeService homeService;

    private Produto classico;
    private Produto lancamento;
    private Produto categoriaProduto;
    private Categoria livros;

    @BeforeEach
    void setUp() {
        homeService = new HomeService(produtoRepository, categoriaRepository, produtoMapper, precoService, seboService,
                recomendacaoService, new com.vitral.config.RecomendacaoProperties(1000, 200, 20, 30, 365, 30, 3, 90, 50, 90));
        livros = Categoria.builder().nome("Livros").slug("livros").build();
        setId(livros, 10L);
        classico = produto(1L, "Classico", true, false);
        lancamento = produto(2L, "Lancamento", false, true);
        categoriaProduto = produto(4L, "Categoria", false, false);
    }

    @Test
    void visitanteRecebeSecoesSemDuplicacaoComTodosOsProdutosETotal() {
        prepararPaginas();
        when(categoriaRepository.findComProdutosDisponiveis(null, com.vitral.service.CategoriaService.SLUGS_PERMITIDOS)).thenReturn(List.of(livros));
        when(produtoRepository.count(org.mockito.ArgumentMatchers.<Specification<Produto>>any())).thenReturn(9L);
        when(precoService.precosPromocionaisVigentes(any())).thenReturn(Map.of());
        when(produtoMapper.toResponse(any(), any())).thenAnswer(i -> response(i.getArgument(0)));

        var home = homeService.carregar(null);

        assertThat(home.classicos().produtos()).extracting(ProdutoResponse::id).containsExactly(1L);
        assertThat(home.lancamentos().produtos()).extracting(ProdutoResponse::id).containsExactly(2L);
        assertThat(home.classicos().total()).isEqualTo(8);
        assertThat(home.lancamentos().total()).isEqualTo(7);
        assertThat(home.categorias()).singleElement().satisfies(c -> {
            assertThat(c.slug()).isEqualTo("livros");
            assertThat(c.total()).isEqualTo(9);
            assertThat(c.produtos()).extracting(ProdutoResponse::id).containsExactly(4L);
        });
        assertThat(home.recomendados()).isNull();
        verify(seboService, never()).buscarEntidadePorAccount(any());

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(produtoRepository, org.mockito.Mockito.times(3)).findAll(
                org.mockito.ArgumentMatchers.<Specification<Produto>>any(), pageable.capture());
        assertThat(pageable.getAllValues()).allMatch(Pageable::isUnpaged);
    }

    @Test
    void seboUsaEscopoObtidoDaContaAutenticada() {
        Account account = Account.builder().type(AccountType.SEBO).build();
        setId(account, 20L);
        Sebo sebo = Sebo.builder().account(account).build();
        setId(sebo, 30L);
        when(seboService.buscarEntidadePorAccount(20L)).thenReturn(sebo);
        prepararPaginas();
        when(categoriaRepository.findComProdutosDisponiveis(30L, com.vitral.service.CategoriaService.SLUGS_PERMITIDOS)).thenReturn(List.of(livros));
        when(produtoRepository.count(org.mockito.ArgumentMatchers.<Specification<Produto>>any())).thenReturn(1L);
        when(precoService.precosPromocionaisVigentes(any())).thenReturn(Map.of());
        when(produtoMapper.toResponse(any(), any())).thenAnswer(i -> response(i.getArgument(0)));

        homeService.carregar(account);

        verify(seboService).buscarEntidadePorAccount(20L);
        verify(categoriaRepository).findComProdutosDisponiveis(30L, com.vitral.service.CategoriaService.SLUGS_PERMITIDOS);
    }

    @Test
    void usuarioUsaCategoriasDosFavoritosParaRecomendacoes() {
        Account account = Account.builder().type(AccountType.USUARIO).build();
        setId(account, 40L);
        when(recomendacaoService.paraHome(eq(account), any())).thenReturn(
                new com.vitral.dto.HomeSectionResponse("Recomendados para voce", List.of(response(categoriaProduto)), 1));
        when(produtoRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(classico)), new PageImpl<>(List.of(lancamento)));
        when(categoriaRepository.findComProdutosDisponiveis(null, com.vitral.service.CategoriaService.SLUGS_PERMITIDOS)).thenReturn(List.of());
        when(precoService.precosPromocionaisVigentes(any())).thenReturn(Map.of());
        when(produtoMapper.toResponse(any(), any())).thenAnswer(i -> response(i.getArgument(0)));

        var home = homeService.carregar(account);

        assertThat(home.recomendados()).isNotNull();
        assertThat(home.recomendados().produtos()).extracting(ProdutoResponse::id).containsExactly(4L);
    }

    private void prepararPaginas() {
        when(produtoRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(classico), Pageable.unpaged(), 8),
                        new PageImpl<>(List.of(lancamento), Pageable.unpaged(), 7),
                        new PageImpl<>(List.of(categoriaProduto), Pageable.unpaged(), 5));
    }

    private Produto produto(Long id, String titulo, boolean classicoFlag, boolean lancamentoFlag) {
        Account conta = Account.builder().name("Sebo").type(AccountType.SEBO).ativo(true).build();
        Sebo sebo = Sebo.builder().account(conta).build();
        setId(sebo, 99L);
        Produto produto = Produto.builder().sebo(sebo).categoria(livros).titulo(titulo).preco(BigDecimal.TEN)
                .estoque(1).condicao(CondicaoProduto.NOVO).ativo(true).classico(classicoFlag)
                .lancamento(lancamentoFlag).build();
        setId(produto, id);
        return produto;
    }

    private ProdutoResponse response(Produto produto) {
        return new ProdutoResponse(produto.getId(), produto.getSebo().getId(), produto.getSebo().getAccount().getName(),
                livros.getId(), livros.getNome(), null, produto.getTitulo(), null, null, null,
                produto.getPreco(), null, produto.getEstoque(), produto.getCondicao(), null, true, true,
                produto.getClassico(), produto.getLancamento(), null);
    }

    private void setId(Object entity, Long id) {
        try {
            Field field = BaseEntity.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
