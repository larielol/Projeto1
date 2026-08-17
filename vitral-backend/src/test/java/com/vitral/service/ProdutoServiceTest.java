package com.vitral.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;

import com.vitral.dto.ProdutoRequest;
import com.vitral.dto.ProdutoResponse;
import com.vitral.dto.VendedorProdutoResponse;
import com.vitral.entity.Account;
import com.vitral.entity.BaseEntity;
import com.vitral.entity.Categoria;
import com.vitral.entity.Produto;
import com.vitral.entity.Sebo;
import com.vitral.enumerations.AccountType;
import com.vitral.enumerations.CondicaoProduto;
import com.vitral.enumerations.StatusVerificacaoSebo;
import com.vitral.enumerations.TipoMovimentacaoEstoque;
import com.vitral.exception.BusinessException;
import com.vitral.exception.ResourceNotFoundException;
import com.vitral.mapper.ProdutoMapper;
import com.vitral.repository.ProdutoRepository;

@ExtendWith(MockitoExtension.class)
class ProdutoServiceTest {

    @Mock RecomendacaoEventoService recomendacaoEventoService;

    @Mock
    private ProdutoRepository produtoRepository;

    @Mock
    private ProdutoMapper produtoMapper;

    @Mock
    private SeboService seboService;

    @Mock
    private CategoriaService categoriaService;

    @Mock
    private PrecoService precoService;


    @Mock
    private MovimentacaoEstoqueService movimentacaoEstoqueService;

    @InjectMocks
    private ProdutoService produtoService;

    @Captor
    private ArgumentCaptor<Produto> produtoCaptor;

    private Account contaDona;
    private Account contaOutroSebo;
    private Sebo seboDono;
    private Sebo seboOutro;
    private ProdutoRequest request;

    @BeforeEach
    void setUp() {
        contaDona = Account.builder()
                .name("Sebo Dono")
                .email("dono@vitral.com")
                .passwordHash("hash")
                .type(AccountType.SEBO)
                .build();
        setId(contaDona, 100L);

        contaOutroSebo = Account.builder()
                .name("Outro Sebo")
                .email("outro@vitral.com")
                .passwordHash("hash")
                .type(AccountType.SEBO)
                .build();
        setId(contaOutroSebo, 200L);

        seboDono = Sebo.builder().account(contaDona)
                .statusVerificacao(StatusVerificacaoSebo.VERIFICADO).build();
        setId(seboDono, 1L);

        seboOutro = Sebo.builder().account(contaOutroSebo)
                .statusVerificacao(StatusVerificacaoSebo.VERIFICADO).build();
        setId(seboOutro, 2L);

        request = new ProdutoRequest(
                "Dom Casmurro",
                7L,
                com.vitral.enumerations.BookGenre.ROMANCE,
                "Machado de Assis",
                "Edicao de 1899, capa dura",
                new BigDecimal("49.90"),
                3,
                CondicaoProduto.USADO,
                "https://cdn/dom.jpg");
    }

    @Test
    @DisplayName("Deve criar produto vinculado ao sebo da conta autenticada com ativo igual a true")
    void shouldCreateProductLinkedToOwnerSeboWithAtivoTrue() {
        Categoria categoria = Categoria.builder().nome("Livros").slug("livros").build();
        setId(categoria, 7L);
        ProdutoResponse responseEsperado = new ProdutoResponse(1L, 1L, null, null, request.titulo(), request.autor(),
                request.descricao(), request.preco(), null, request.estoque(), request.condicao(), request.fotoUrl(), Boolean.TRUE);

        when(seboService.buscarEntidadePorAccount(100L)).thenReturn(seboDono);
        when(categoriaService.buscarPermitida(7L)).thenReturn(categoria);
        when(produtoRepository.save(produtoCaptor.capture())).thenAnswer(invocation -> invocation.getArgument(0));
        when(precoService.precoPromocionalVigente(any())).thenReturn(Optional.empty());
        when(produtoMapper.toResponse(produtoCaptor.capture(), any())).thenReturn(responseEsperado);

        ProdutoResponse resultado = produtoService.criar(contaDona, request);

        assertThat(resultado).isEqualTo(responseEsperado);
        Produto persistido = produtoCaptor.getAllValues().get(0);
        assertThat(persistido.getSebo()).isSameAs(seboDono);
        assertThat(persistido.getTitulo()).isEqualTo("Dom Casmurro");
        assertThat(persistido.getPreco()).isEqualByComparingTo("49.90");
        assertThat(persistido.getEstoque()).isEqualTo(3);
        assertThat(persistido.getCondicao()).isEqualTo(CondicaoProduto.USADO);
        assertThat(persistido.getAtivo()).isTrue();
        verify(seboService).registrarAtividade(seboDono);
    }

    @Test
    @DisplayName("Deve criar produto com estoque padrao e categoria quando estoque vier nulo")
    void criar_estoqueNulo_usaPadraoECategoria() {
        Categoria categoria = Categoria.builder().nome("Livros").slug("livros").build();
        setId(categoria, 7L);
        ProdutoRequest requestSemEstoque = new ProdutoRequest( "Dom Casmurro", 7L, "Machado",
                "Descricao", BigDecimal.TEN, null, CondicaoProduto.USADO, null);
        ProdutoResponse responseEsperado = new ProdutoResponse(1L, 1L, 7L, "Romance",
                "Dom Casmurro", "Machado", "Descricao", BigDecimal.TEN, null, 1, CondicaoProduto.USADO, null, true);

        when(seboService.buscarEntidadePorAccount(100L)).thenReturn(seboDono);
        when(categoriaService.buscarPermitida(7L)).thenReturn(categoria);
        when(produtoRepository.save(produtoCaptor.capture())).thenAnswer(invocation -> invocation.getArgument(0));
        when(precoService.precoPromocionalVigente(any())).thenReturn(Optional.empty());
        when(produtoMapper.toResponse(produtoCaptor.capture(), any())).thenReturn(responseEsperado);

        ProdutoResponse resultado = produtoService.criar(contaDona, requestSemEstoque);

        assertThat(resultado).isSameAs(responseEsperado);
        assertThat(produtoCaptor.getAllValues().get(0).getEstoque()).isEqualTo(1);
        verify(movimentacaoEstoqueService).registrarAlteracao(any(), eq(contaDona), eq(TipoMovimentacaoEstoque.ENTRADA),
                eq(1), eq(0), eq(1), eq(BigDecimal.TEN), eq("Estoque inicial"));
        verify(seboService).registrarAtividade(seboDono);
    }

    @Test
    @DisplayName("Deve propagar ResourceNotFoundException quando conta autenticada nao possui sebo")
    void shouldPropagateNotFoundWhenAccountHasNoSebo() {
        when(seboService.buscarEntidadePorAccount(100L))
                .thenThrow(new ResourceNotFoundException("Sebo nao encontrado para esta conta"));

        assertThatThrownBy(() -> produtoService.criar(contaDona, request))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(produtoRepository, never()).save(produtoCaptor.capture());
    }

    @Test
    @DisplayName("Deve atualizar campos do produto quando o sebo dono e o da conta autenticada")
    void shouldUpdateProductWhenOwnedByAuthenticatedAccount() {
        Categoria categoria = Categoria.builder().nome("Livros").slug("livros").build();
        setId(categoria, 7L);
        Produto produto = Produto.builder()
                .sebo(seboDono)
                .titulo("Antigo")
                .preco(new BigDecimal("10.00"))
                .condicao(CondicaoProduto.NOVO)
                .ativo(Boolean.TRUE)
                .build();
        setId(produto, 50L);
        ProdutoResponse esperado = new ProdutoResponse(50L, 1L, null, null, request.titulo(), request.autor(),
                request.descricao(), request.preco(), null, request.estoque(), request.condicao(), request.fotoUrl(), Boolean.TRUE);

        when(produtoRepository.findById(50L)).thenReturn(Optional.of(produto));
        when(categoriaService.buscarPermitida(7L)).thenReturn(categoria);
        when(precoService.precoPromocionalVigente(any())).thenReturn(Optional.empty());
        when(produtoMapper.toResponse(eq(produto), any())).thenReturn(esperado);

        ProdutoResponse resultado = produtoService.atualizar(contaDona, 50L, request);

        assertThat(resultado).isEqualTo(esperado);
        assertThat(produto.getTitulo()).isEqualTo("Dom Casmurro");
        assertThat(produto.getPreco()).isEqualByComparingTo("49.90");
        assertThat(produto.getEstoque()).isEqualTo(3);
        assertThat(produto.getCondicao()).isEqualTo(CondicaoProduto.USADO);
        assertThat(produto.getFotoUrl()).isEqualTo("https://cdn/dom.jpg");
        verify(seboService).registrarAtividade(seboDono);
    }

    @Test
    @DisplayName("Deve lancar BusinessException com status 403 ao atualizar produto de outro sebo")
    void shouldRejectUpdateWhenProductBelongsToAnotherSebo() {
        Produto produtoDeOutroSebo = Produto.builder().sebo(seboOutro).ativo(Boolean.TRUE).build();
        setId(produtoDeOutroSebo, 77L);

        when(produtoRepository.findById(77L)).thenReturn(Optional.of(produtoDeOutroSebo));

        assertThatThrownBy(() -> produtoService.atualizar(contaDona, 77L, request))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getStatus()).isEqualTo(HttpStatus.FORBIDDEN));

        assertThat(produtoDeOutroSebo.getTitulo()).isNull();
    }

    @Test
    @DisplayName("Deve lancar ResourceNotFoundException ao atualizar produto inexistente")
    void shouldThrowNotFoundWhenUpdatingMissingProduct() {
        when(produtoRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> produtoService.atualizar(contaDona, 999L, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Produto nao encontrado");
    }

    @Test
    @DisplayName("Deve marcar produto como inativo no soft delete preservando a entidade")
    void shouldSoftDeleteProductSettingAtivoFalse() {
        Produto produto = Produto.builder().sebo(seboDono).ativo(Boolean.TRUE).build();
        setId(produto, 30L);
        when(produtoRepository.findById(30L)).thenReturn(Optional.of(produto));

        produtoService.remover(contaDona, 30L);

        assertThat(produto.getAtivo()).isFalse();
        verify(produtoRepository, never()).delete(produto);
        verify(seboService).registrarAtividade(seboDono);
    }

    @Test
    @DisplayName("Deve lancar BusinessException com status 403 ao remover produto de outro sebo")
    void shouldRejectRemoveWhenProductBelongsToAnotherSebo() {
        Produto produtoDeOutroSebo = Produto.builder().sebo(seboOutro).ativo(Boolean.TRUE).build();
        setId(produtoDeOutroSebo, 80L);
        when(produtoRepository.findById(80L)).thenReturn(Optional.of(produtoDeOutroSebo));

        assertThatThrownBy(() -> produtoService.remover(contaDona, 80L))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getStatus()).isEqualTo(HttpStatus.FORBIDDEN));

        assertThat(produtoDeOutroSebo.getAtivo()).isTrue();
    }

    @Test
    @DisplayName("Deve retornar produto ativo na consulta publica por id")
    void shouldReturnActiveProductWhenSearchedById() {
        Produto produto = Produto.builder().sebo(seboDono).ativo(Boolean.TRUE).build();
        setId(produto, 5L);
        ProdutoResponse esperado = new ProdutoResponse(5L, 1L, null, null, "T", null, null,
                BigDecimal.TEN, null, 1, CondicaoProduto.NOVO, null, Boolean.TRUE);

        when(produtoRepository.findByIdAndAtivoTrue(5L)).thenReturn(Optional.of(produto));
        when(precoService.precoPromocionalVigente(any())).thenReturn(Optional.empty());
        when(produtoMapper.toResponse(eq(produto), any())).thenReturn(esperado);

        ProdutoResponse resultado = produtoService.buscarPorId(5L);

        assertThat(resultado).isEqualTo(esperado);
        verify(recomendacaoEventoService).registrarProduto(null,
                com.vitral.enumerations.TipoEventoRecomendacao.VISUALIZACAO, produto);
    }

    @Test
    @DisplayName("Deve esconder produto quando o sebo nao estiver verificado")
    void buscarPorId_seboNaoVerificado_lancaNotFound() {
        Sebo seboPendente = Sebo.builder().account(contaDona)
                .statusVerificacao(StatusVerificacaoSebo.PENDENTE).build();
        Produto produto = Produto.builder().sebo(seboPendente).ativo(Boolean.TRUE).build();
        setId(produto, 5L);
        when(produtoRepository.findByIdAndAtivoTrue(5L)).thenReturn(Optional.of(produto));

        assertThatThrownBy(() -> produtoService.buscarPorId(5L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Produto nao encontrado");
    }

    @Test
    @DisplayName("Deve lancar ResourceNotFoundException ao buscar produto inativo ou inexistente")
    void shouldThrowNotFoundWhenSearchingInactiveOrMissingProduct() {
        when(produtoRepository.findByIdAndAtivoTrue(123L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> produtoService.buscarPorId(123L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Produto nao encontrado");
    }

    @Test
    @DisplayName("Deve listar apenas o produto de referencia como vendedor")
    void shouldListOnlyReferenceProductAsSeller() {
        Produto referencia = Produto.builder().sebo(seboDono).titulo("Avulso")
                .preco(new BigDecimal("30.00")).estoque(2).condicao(CondicaoProduto.USADO)
                .ativo(true).build();
        setId(referencia, 10L);
        when(produtoRepository.findByIdAndAtivoTrue(10L)).thenReturn(Optional.of(referencia));
        when(precoService.precoPromocionalVigente(referencia)).thenReturn(Optional.empty());

        List<VendedorProdutoResponse> vendedores = produtoService.listarVendedores(10L);

        assertThat(vendedores).hasSize(1);
        assertThat(vendedores.getFirst().produtoId()).isEqualTo(10L);
        assertThat(vendedores.getFirst().precoEfetivo()).isEqualByComparingTo("30.00");
    }


    @Test
    @DisplayName("Deve listar produtos ativos de um sebo de forma paginada")
    void shouldListActiveProductsBySeboPaged() {
        Produto p1 = Produto.builder().sebo(seboDono).ativo(Boolean.TRUE).build();
        setId(p1, 1L);
        Produto p2 = Produto.builder().sebo(seboDono).ativo(Boolean.TRUE).build();
        setId(p2, 2L);
        Pageable pageable = PageRequest.of(0, 10);
        Page<Produto> pagina = new PageImpl<>(List.of(p1, p2), pageable, 2);
        ProdutoResponse r1 = new ProdutoResponse(1L, 1L, null, null, "A", null, null, BigDecimal.ONE, null, 1, CondicaoProduto.NOVO, null, true);
        ProdutoResponse r2 = new ProdutoResponse(2L, 1L, null, null, "B", null, null, BigDecimal.ONE, null, 1, CondicaoProduto.NOVO, null, true);

        when(produtoRepository.findAll(org.mockito.ArgumentMatchers.<Specification<Produto>>any(), eq(pageable)))
                .thenReturn(pagina);
        when(precoService.precosPromocionaisVigentes(any())).thenReturn(Map.of());
        when(produtoMapper.toResponse(eq(p1), any())).thenReturn(r1);
        when(produtoMapper.toResponse(eq(p2), any())).thenReturn(r2);

        Page<ProdutoResponse> resultado = produtoService.listarPorSebo(1L, pageable);

        assertThat(resultado.getTotalElements()).isEqualTo(2);
        assertThat(resultado.getContent()).containsExactly(r1, r2);
    }

    @Test
    @DisplayName("Deve listar produtos ativos por categoria")
    void listarPorCategoria_retornaProdutosAtivos() {
        Categoria categoria = Categoria.builder().nome("Livros").slug("livros").build();
        setId(categoria, 7L);
        Produto produto = Produto.builder().sebo(seboDono).categoria(categoria).ativo(true).build();
        setId(produto, 1L);
        Pageable pageable = PageRequest.of(0, 10);
        ProdutoResponse response = new ProdutoResponse(1L, 1L, 7L, "Romance", "A", null, null,
                BigDecimal.ONE, null, 1, CondicaoProduto.NOVO, null, true);
        when(categoriaService.buscarPermitida(7L)).thenReturn(categoria);
        when(produtoRepository.findAll(org.mockito.ArgumentMatchers.<Specification<Produto>>any(), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(produto), pageable, 1));
        when(precoService.precosPromocionaisVigentes(any())).thenReturn(Map.of());
        when(produtoMapper.toResponse(eq(produto), any())).thenReturn(response);

        Page<ProdutoResponse> resultado = produtoService.listarPorCategoria(7L, pageable);

        assertThat(resultado.getContent()).containsExactly(response);
        verify(categoriaService).buscarPermitida(7L);
    }

    @Test
    @DisplayName("Deve listar lancamentos usando produtos ativos na ordenacao recebida")
    void listarLancamentos_retornaProdutosAtivos() {
        Produto produto = Produto.builder().sebo(seboDono).ativo(true).build();
        setId(produto, 1L);
        Pageable pageable = PageRequest.of(0, 10);
        ProdutoResponse response = new ProdutoResponse(1L, 1L, null, null, "Lancamento", null, null,
                BigDecimal.TEN, null, 2, CondicaoProduto.NOVO, null, true);
        when(produtoRepository.findAll(org.mockito.ArgumentMatchers.<Specification<Produto>>any(), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(produto), pageable, 1));
        when(precoService.precosPromocionaisVigentes(any())).thenReturn(Map.of());
        when(produtoMapper.toResponse(eq(produto), any())).thenReturn(response);

        Page<ProdutoResponse> resultado = produtoService.listarLancamentos(pageable);

        assertThat(resultado.getContent()).containsExactly(response);
        verify(produtoRepository).findAll(org.mockito.ArgumentMatchers.<Specification<Produto>>any(), eq(pageable));
    }

    @Test
    @DisplayName("Deve listar classicos usando produtos ativos ordenados por data de criacao ascendente")
    void listarClassicos_retornaProdutosMaisAntigos() {
        Produto produto = Produto.builder().sebo(seboDono).ativo(true).build();
        setId(produto, 1L);
        Pageable pageable = PageRequest.of(0, 10);
        ProdutoResponse response = new ProdutoResponse(1L, 1L, null, null, "Classico", null, null,
                BigDecimal.TEN, null, 1, CondicaoProduto.USADO, null, true);
        when(produtoRepository.findAll(org.mockito.ArgumentMatchers.<Specification<Produto>>any(), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(produto), pageable, 1));
        when(precoService.precosPromocionaisVigentes(any())).thenReturn(Map.of());
        when(produtoMapper.toResponse(eq(produto), any())).thenReturn(response);

        Page<ProdutoResponse> resultado = produtoService.listarClassicos(pageable);

        assertThat(resultado.getContent()).containsExactly(response);
        verify(produtoRepository).findAll(org.mockito.ArgumentMatchers.<Specification<Produto>>any(), eq(pageable));
    }

    @Test
    @DisplayName("Deve retornar pagina vazia quando nao existem produtos classicos ativos")
    void listarClassicos_semProdutos_retornaPaginaVazia() {
        Pageable pageable = PageRequest.of(0, 10);
        when(produtoRepository.findAll(org.mockito.ArgumentMatchers.<Specification<Produto>>any(), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        Page<ProdutoResponse> resultado = produtoService.listarClassicos(pageable);

        assertThat(resultado.getContent()).isEmpty();
        assertThat(resultado.getTotalElements()).isZero();
        verify(produtoRepository).findAll(org.mockito.ArgumentMatchers.<Specification<Produto>>any(), eq(pageable));
    }

    @Test
    void criarLivroComGenero_persisteGenero() {
        Categoria livros = Categoria.builder().nome("Livros").slug("livros").build();
        setId(livros, 7L);
        when(seboService.buscarEntidadePorAccount(100L)).thenReturn(seboDono);
        when(categoriaService.buscarPermitida(7L)).thenReturn(livros);
        when(produtoRepository.save(produtoCaptor.capture())).thenAnswer(i -> i.getArgument(0));
        when(precoService.precoPromocionalVigente(any())).thenReturn(Optional.empty());

        produtoService.criar(contaDona, request);

        assertThat(produtoCaptor.getValue().getBookGenre())
                .isEqualTo(com.vitral.enumerations.BookGenre.ROMANCE);
    }

    @Test
    void criarProduto_persisteClassicoELancamentoExplicitamente() {
        Categoria livros = Categoria.builder().nome("Livros").slug("livros").build();
        setId(livros, 7L);
        ProdutoRequest classificado = new ProdutoRequest("Edicao especial", 7L, null, "Autor", null,
                BigDecimal.TEN, 1, CondicaoProduto.NOVO, null, true, true);
        when(seboService.buscarEntidadePorAccount(100L)).thenReturn(seboDono);
        when(categoriaService.buscarPermitida(7L)).thenReturn(livros);
        when(produtoRepository.save(produtoCaptor.capture())).thenAnswer(i -> i.getArgument(0));
        when(precoService.precoPromocionalVigente(any())).thenReturn(Optional.empty());

        produtoService.criar(contaDona, classificado);

        assertThat(produtoCaptor.getValue().getClassico()).isTrue();
        assertThat(produtoCaptor.getValue().getLancamento()).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"livros", "cds", "vinis", "hqs-mangas"})
    void criarProdutoNasQuatroCategoriasOficiais(String slug) {
        Categoria categoria = Categoria.builder().nome(slug).slug(slug).build();
        setId(categoria, 7L);
        ProdutoRequest produtoSemGenero = new ProdutoRequest( "Produto", 7L, null, null,
                BigDecimal.TEN, 1, CondicaoProduto.NOVO, null);
        when(seboService.buscarEntidadePorAccount(100L)).thenReturn(seboDono);
        when(categoriaService.buscarPermitida(7L)).thenReturn(categoria);
        when(produtoRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(precoService.precoPromocionalVigente(any())).thenReturn(Optional.empty());

        produtoService.criar(contaDona, produtoSemGenero);

        verify(produtoRepository).save(any());
    }

    @Test
    void criarCdComGenero_rejeitaPayloadInconsistente() {
        Categoria cds = Categoria.builder().nome("CDs").slug("cds").build();
        setId(cds, 8L);
        ProdutoRequest cd = new ProdutoRequest( "Album", 8L,
                com.vitral.enumerations.BookGenre.FANTASIA, null, null, BigDecimal.TEN, 1,
                CondicaoProduto.NOVO, null);
        when(seboService.buscarEntidadePorAccount(100L)).thenReturn(seboDono);
        when(categoriaService.buscarPermitida(8L)).thenReturn(cds);

        assertThatThrownBy(() -> produtoService.criar(contaDona, cd))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("somente pode ser informado");
        verify(produtoRepository, never()).save(any());
    }

    @Test
    void alterarLivroParaVinil_removeGeneroAnterior() {
        Categoria vinis = Categoria.builder().nome("Vinis").slug("vinis").build();
        setId(vinis, 9L);
        Produto produto = Produto.builder().sebo(seboDono).titulo("Livro").estoque(1)
                .bookGenre(com.vitral.enumerations.BookGenre.FANTASIA).preco(BigDecimal.TEN)
                .condicao(CondicaoProduto.USADO).ativo(true).build();
        setId(produto, 90L);
        ProdutoRequest vinil = new ProdutoRequest( "Disco", 9L, null, null,
                BigDecimal.TEN, 1, CondicaoProduto.USADO, null);
        when(produtoRepository.findById(90L)).thenReturn(Optional.of(produto));
        when(categoriaService.buscarPermitida(9L)).thenReturn(vinis);
        when(precoService.precoPromocionalVigente(any())).thenReturn(Optional.empty());

        produtoService.atualizar(contaDona, 90L, vinil);

        assertThat(produto.getBookGenre()).isNull();
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
