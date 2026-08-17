package com.vitral.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

import com.vitral.dto.ProdutoResponse;
import com.vitral.dto.SeboResponse;
import com.vitral.entity.Account;
import com.vitral.entity.Produto;
import com.vitral.entity.Sebo;
import com.vitral.enumerations.AccountType;
import com.vitral.enumerations.CondicaoProduto;
import com.vitral.enumerations.StatusVerificacaoSebo;
import com.vitral.mapper.ProdutoMapper;
import com.vitral.mapper.SeboMapper;
import com.vitral.repository.ProdutoRepository;
import com.vitral.repository.SeboRepository;

@ExtendWith(MockitoExtension.class)
class BuscaServiceTest {

    @Mock RecomendacaoEventoService recomendacaoEventoService;

    @Mock
    private SeboRepository seboRepository;

    @Mock
    private ProdutoRepository produtoRepository;

    @Mock
    private SeboMapper seboMapper;

    @Mock
    private ProdutoMapper produtoMapper;

    @Mock
    private PrecoService precoService;

    @InjectMocks
    private BuscaService buscaService;

    private Pageable pageable;
    private Sebo sebo;
    private SeboResponse seboResponse;
    private Produto produto;
    private ProdutoResponse produtoResponse;

    @BeforeEach
    void setUp() {
        pageable = PageRequest.of(0, 10);

        Account conta = Account.builder()
                .name("Livraria Leitura")
                .email("livraria@email.com")
                .passwordHash("hash")
                .type(AccountType.SEBO)
                .build();

        sebo = Sebo.builder()
                .account(conta)
                .build();

        seboResponse = new SeboResponse(1L, 1L, "Livraria Leitura", "livraria@email.com",
                null, null, "12345678000199", StatusVerificacaoSebo.VERIFICADO, null,
                null, null, null, null, null, null);

        produto = Produto.builder()
                .sebo(sebo)
                .titulo("Dom Casmurro")
                .preco(BigDecimal.valueOf(25.00))
                .condicao(CondicaoProduto.USADO)
                .ativo(true)
                .build();
        ReflectionTestUtils.setField(produto, "id", 1L);

        produtoResponse = new ProdutoResponse(1L, 1L, null, null, "Dom Casmurro", null, null,
                BigDecimal.valueOf(25.00), null, 1, CondicaoProduto.USADO, null, true);
    }

    @Test
    @DisplayName("Deve retornar sebos filtrados quando parametros sao fornecidos")
    void shouldReturnFilteredSebosWhenParametersProvided() {
        Page<Sebo> pageResult = new PageImpl<>(List.of(sebo));
        when(seboRepository.findAll(anySeboSpecification(), eq(pageable))).thenReturn(pageResult);
        when(seboMapper.toPublicResponse(sebo)).thenReturn(seboResponse);

        Page<SeboResponse> resultado = buscaService.buscarSebos("Livraria", pageable);

        assertThat(resultado.getContent()).hasSize(1);
        assertThat(resultado.getContent().getFirst()).isEqualTo(seboResponse);
        verify(seboRepository).findAll(anySeboSpecification(), eq(pageable));
    }

    @Test
    @DisplayName("Deve listar todos os sebos quando nenhum filtro for informado")
    void shouldListAllSebosWhenNoFiltersProvided() {
        Page<Sebo> pageResult = new PageImpl<>(List.of(sebo));
        when(seboRepository.findAll(anySeboSpecification(), eq(pageable))).thenReturn(pageResult);
        when(seboMapper.toPublicResponse(sebo)).thenReturn(seboResponse);

        Page<SeboResponse> resultado = buscaService.buscarSebos(null, pageable);

        assertThat(resultado.getContent()).hasSize(1);
        verify(seboRepository).findAll(anySeboSpecification(), eq(pageable));
    }

    @Test
    @DisplayName("Deve filtrar sebos por tipo de operacao")
    void shouldFilterSebosByOperationType() {
        Page<Sebo> pageResult = new PageImpl<>(List.of(sebo));
        when(seboRepository.findAll(anySeboSpecification(), eq(pageable))).thenReturn(pageResult);
        when(seboMapper.toPublicResponse(sebo)).thenReturn(seboResponse);

        Page<SeboResponse> resultado = buscaService.buscarSebos(null, pageable);

        assertThat(resultado.getContent()).hasSize(1);
        verify(seboRepository).findAll(anySeboSpecification(), eq(pageable));
    }

    @Test
    @DisplayName("Deve filtrar sebos por cidade")
    void shouldFilterSebosByCity() {
        Page<Sebo> pageResult = new PageImpl<>(List.of(sebo));
        when(seboRepository.findAll(anySeboSpecification(), eq(pageable))).thenReturn(pageResult);
        when(seboMapper.toPublicResponse(sebo)).thenReturn(seboResponse);

        Page<SeboResponse> resultado = buscaService.buscarSebos(null, "Joao Pessoa", null, pageable);

        assertThat(resultado.getContent()).hasSize(1);
        verify(seboRepository).findAll(anySeboSpecification(), eq(pageable));
    }

    @Test
    @DisplayName("Deve filtrar sebos por UF")
    void shouldFilterSebosByState() {
        Page<Sebo> pageResult = new PageImpl<>(List.of(sebo));
        when(seboRepository.findAll(anySeboSpecification(), eq(pageable))).thenReturn(pageResult);
        when(seboMapper.toPublicResponse(sebo)).thenReturn(seboResponse);

        Page<SeboResponse> resultado = buscaService.buscarSebos(null, null, "pb", pageable);

        assertThat(resultado.getContent()).hasSize(1);
        verify(seboRepository).findAll(anySeboSpecification(), eq(pageable));
    }

    @Test
    @DisplayName("Deve listar apenas vitrines de sebos ativos")
    void shouldListOnlyActiveSebos() {
        Page<Sebo> pageResult = new PageImpl<>(List.of(sebo));
        when(seboRepository.findAll(anySeboSpecification(), eq(pageable))).thenReturn(pageResult);
        when(seboMapper.toPublicResponse(sebo)).thenReturn(seboResponse);

        Page<SeboResponse> resultado = buscaService.buscarSebos(null, pageable);

        assertThat(resultado.getContent()).hasSize(1);
        verify(seboRepository).findAll(anySeboSpecification(), eq(pageable));
    }

    @Test
    @DisplayName("Deve buscar produtos ativos por titulo quando o termo for informado")
    void shouldSearchActiveProductsByTitleWhenTermoProvided() {
        Page<Produto> pageResult = new PageImpl<>(List.of(produto));
        when(produtoRepository.findAll(anyProdutoSpecification(), eq(pageable))).thenReturn(pageResult);
        when(precoService.precosPromocionaisVigentes(any())).thenReturn(Map.of());
        when(produtoMapper.toResponse(eq(produto), any())).thenReturn(produtoResponse);

        Page<ProdutoResponse> resultado = buscaService.buscarProdutos("Dom", null, null, null, null, pageable);

        assertThat(resultado.getContent()).hasSize(1);
        assertThat(resultado.getContent().getFirst().titulo()).isEqualTo("Dom Casmurro");
        verify(produtoRepository).findAll(anyProdutoSpecification(), eq(pageable));
    }

    @Test
    @DisplayName("Deve filtrar produtos por sebo quando seboId for informado")
    void shouldFilterProductsBySeboWhenSeboIdProvided() {
        Page<Produto> pageResult = new PageImpl<>(List.of(produto));
        when(produtoRepository.findAll(anyProdutoSpecification(), eq(pageable))).thenReturn(pageResult);
        when(precoService.precosPromocionaisVigentes(any())).thenReturn(Map.of());
        when(produtoMapper.toResponse(eq(produto), any())).thenReturn(produtoResponse);

        Page<ProdutoResponse> resultado = buscaService.buscarProdutos(null, 1L, null, null, null, pageable);

        assertThat(resultado.getContent()).hasSize(1);
        verify(produtoRepository).findAll(anyProdutoSpecification(), eq(pageable));
    }

    @Test
    @DisplayName("Deve filtrar produtos por condicao quando condicao for informada")
    void shouldFilterProductsByCondicaoWhenCondicaoProvided() {
        Page<Produto> pageResult = new PageImpl<>(List.of(produto));
        when(produtoRepository.findAll(anyProdutoSpecification(), eq(pageable))).thenReturn(pageResult);
        when(precoService.precosPromocionaisVigentes(any())).thenReturn(Map.of());
        when(produtoMapper.toResponse(eq(produto), any())).thenReturn(produtoResponse);

        Page<ProdutoResponse> resultado = buscaService.buscarProdutos(null, null, CondicaoProduto.USADO, null, null, pageable);

        assertThat(resultado.getContent()).hasSize(1);
        verify(produtoRepository).findAll(anyProdutoSpecification(), eq(pageable));
    }

    @Test
    @DisplayName("Deve filtrar produtos por faixa de preco quando precoMin e precoMax forem informados")
    void shouldFilterProductsByPriceRangeWhenBothLimitsProvided() {
        Page<Produto> pageResult = new PageImpl<>(List.of(produto));
        when(produtoRepository.findAll(anyProdutoSpecification(), eq(pageable))).thenReturn(pageResult);
        when(precoService.precosPromocionaisVigentes(any())).thenReturn(Map.of());
        when(produtoMapper.toResponse(eq(produto), any())).thenReturn(produtoResponse);

        Page<ProdutoResponse> resultado = buscaService.buscarProdutos(
                null, null, null, BigDecimal.valueOf(10), BigDecimal.valueOf(50), pageable);

        assertThat(resultado.getContent()).hasSize(1);
        verify(produtoRepository).findAll(anyProdutoSpecification(), eq(pageable));
    }

    @Test
    @DisplayName("Deve listar todos os produtos ativos quando nenhum filtro for informado")
    void shouldListAllActiveProductsWhenNoFiltersProvided() {
        Page<Produto> pageResult = new PageImpl<>(List.of(produto));
        when(produtoRepository.findAll(anyProdutoSpecification(), eq(pageable))).thenReturn(pageResult);
        when(precoService.precosPromocionaisVigentes(any())).thenReturn(Map.of());
        when(produtoMapper.toResponse(eq(produto), any())).thenReturn(produtoResponse);

        Page<ProdutoResponse> resultado = buscaService.buscarProdutos(null, null, null, null, null, pageable);

        assertThat(resultado.getContent()).hasSize(1);
        verify(produtoRepository).findAll(anyProdutoSpecification(), eq(pageable));
    }

    @Test
    @DisplayName("Deve retornar pagina vazia quando nenhum resultado for encontrado")
    void shouldReturnEmptyPageWhenNoResultsFound() {
        when(seboRepository.findAll(anySeboSpecification(), eq(pageable))).thenReturn(new PageImpl<>(List.of()));

        Page<SeboResponse> resultado = buscaService.buscarSebos("Inexistente", pageable);

        assertThat(resultado.getContent()).isEmpty();
        assertThat(resultado.getTotalElements()).isZero();
    }

    @Test
    @DisplayName("Deve ordenar sebos por proximidade quando lat e lng forem informados, deixando sem coordenadas por ultimo")
    void shouldOrderSebosByProximityWhenLatAndLngProvided() {
        Account conta = sebo.getAccount();
        Sebo seboDistante = Sebo.builder().account(conta).latitude(-23.5505).longitude(-46.6333).build(); // Sao Paulo
        Sebo seboProximo = Sebo.builder().account(conta).latitude(-7.1219).longitude(-34.8850).build(); // Joao Pessoa
        Sebo seboSemCoordenadas = Sebo.builder().account(conta).build();

        when(seboRepository.findAll(anySeboSpecification()))
                .thenReturn(List.of(seboDistante, seboSemCoordenadas, seboProximo));
        when(seboMapper.toPublicResponse(eq(seboProximo), any()))
                .thenAnswer(inv -> responseComDistancia(1L, "Proximo", inv.getArgument(1)));
        when(seboMapper.toPublicResponse(eq(seboDistante), any()))
                .thenAnswer(inv -> responseComDistancia(2L, "Distante", inv.getArgument(1)));
        when(seboMapper.toPublicResponse(eq(seboSemCoordenadas), any()))
                .thenAnswer(inv -> responseComDistancia(3L, "SemCoordenadas", inv.getArgument(1)));

        // Referencia proxima a Joao Pessoa/PB
        Page<SeboResponse> resultado = buscaService.buscarSebos(null, null, null, -7.1150, -34.8631, pageable);

        assertThat(resultado.getContent()).extracting(SeboResponse::nome)
                .containsExactly("Proximo", "Distante", "SemCoordenadas");
        assertThat(resultado.getContent().get(0).distanciaKm()).isNotNull();
        assertThat(resultado.getContent().get(1).distanciaKm()).isNotNull();
        assertThat(resultado.getContent().get(2).distanciaKm()).isNull();
        assertThat(resultado.getTotalElements()).isEqualTo(3);
    }

    @Test
    @DisplayName("Deve manter comportamento padrao quando apenas lat ou apenas lng forem informados")
    void shouldIgnoreProximitySortingWhenOnlyOneCoordinateProvided() {
        Page<Sebo> pageResult = new PageImpl<>(List.of(sebo));
        when(seboRepository.findAll(anySeboSpecification(), eq(pageable))).thenReturn(pageResult);
        when(seboMapper.toPublicResponse(sebo)).thenReturn(seboResponse);

        Page<SeboResponse> resultado = buscaService.buscarSebos(null, null, null, -7.1150, null, pageable);

        assertThat(resultado.getContent()).containsExactly(seboResponse);
        verify(seboRepository).findAll(anySeboSpecification(), eq(pageable));
    }

    private SeboResponse responseComDistancia(Long id, String nome, Double distanciaKm) {
        return new SeboResponse(id, 1L, nome, nome + "@email.com", null, null, String.valueOf(id),
                StatusVerificacaoSebo.VERIFICADO, null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, distanciaKm);
    }

    private static Specification<Sebo> anySeboSpecification() {
        return ArgumentMatchers.any();
    }

    private static Specification<Produto> anyProdutoSpecification() {
        return ArgumentMatchers.any();
    }
}
