package com.vitral.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import com.vitral.dto.OfertaRequest;
import com.vitral.entity.Account;
import com.vitral.entity.Oferta;
import com.vitral.entity.Produto;
import com.vitral.entity.Sebo;
import com.vitral.enumerations.AccountType;
import com.vitral.enumerations.CondicaoProduto;
import com.vitral.exception.BusinessException;
import com.vitral.exception.ConflictException;
import com.vitral.repository.OfertaRepository;
import com.vitral.repository.ProdutoRepository;

@ExtendWith(MockitoExtension.class)
class OfertaServiceTest {

    @Mock
    private OfertaRepository ofertaRepository;

    @Mock
    private ProdutoRepository produtoRepository;

    @Mock
    private SeboService seboService;

    @InjectMocks
    private OfertaService ofertaService;

    private Account contaSebo;
    private Produto produto;

    @BeforeEach
    void setUp() {
        contaSebo = account(1L, "Sebo", AccountType.SEBO);
        Sebo sebo = Sebo.builder().account(contaSebo).build();
        ReflectionTestUtils.setField(sebo, "id", 5L);
        produto = Produto.builder()
                .sebo(sebo)
                .titulo("Dom Casmurro")
                .preco(new BigDecimal("50.00"))
                .estoque(4)
                .condicao(CondicaoProduto.USADO)
                .ativo(true)
                .build();
        ReflectionTestUtils.setField(produto, "id", 10L);
    }

    @Test
    @DisplayName("Deve criar oferta para produto do proprio sebo")
    void criar_sucesso_retornaOferta() {
        OfertaRequest request = request(new BigDecimal("39.90"), null, null, true);
        when(produtoRepository.findById(10L)).thenReturn(Optional.of(produto));
        when(ofertaRepository.findByProdutoIdAndAtivaTrue(10L)).thenReturn(Optional.empty());
        when(ofertaRepository.save(any(Oferta.class))).thenAnswer(invocation -> {
            Oferta oferta = invocation.getArgument(0);
            ReflectionTestUtils.setField(oferta, "id", 20L);
            return oferta;
        });

        var response = ofertaService.criar(contaSebo, request);

        assertThat(response.id()).isEqualTo(20L);
        assertThat(response.produtoId()).isEqualTo(10L);
        assertThat(response.precoPromocional()).isEqualByComparingTo("39.90");
        verify(seboService).registrarAtividade(produto.getSebo());
    }

    @Test
    @DisplayName("Deve rejeitar segunda oferta ativa para o mesmo produto")
    void criar_ofertaAtivaDuplicada_lancaConflict() {
        when(produtoRepository.findById(10L)).thenReturn(Optional.of(produto));
        when(ofertaRepository.findByProdutoIdAndAtivaTrue(10L))
                .thenReturn(Optional.of(Oferta.builder().produto(produto).ativa(true).build()));

        assertThatThrownBy(() -> ofertaService.criar(contaSebo, request(new BigDecimal("40.00"), null, null, true)))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    @DisplayName("Deve impedir oferta para produto de outro sebo")
    void criar_produtoDeOutroSebo_lancaForbidden() {
        Account outraConta = account(2L, "Outro", AccountType.SEBO);
        when(produtoRepository.findById(10L)).thenReturn(Optional.of(produto));

        assertThatThrownBy(() -> ofertaService.criar(outraConta, request(new BigDecimal("40.00"), null, null, true)))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getStatus()).isEqualTo(HttpStatus.FORBIDDEN));

        verify(ofertaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve rejeitar oferta para produto inativo")
    void criar_produtoInativo_lancaBadRequest() {
        produto.setAtivo(false);
        when(produtoRepository.findById(10L)).thenReturn(Optional.of(produto));

        assertThatThrownBy(() -> ofertaService.criar(contaSebo, request(new BigDecimal("40.00"), null, null, true)))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    @DisplayName("Deve rejeitar oferta cuja data final antecede o inicio")
    void criar_periodoInvalido_lancaBadRequest() {
        OffsetDateTime inicio = OffsetDateTime.now().plusDays(2);
        OffsetDateTime fim = inicio.minusDays(1);
        when(produtoRepository.findById(10L)).thenReturn(Optional.of(produto));
        when(ofertaRepository.findByProdutoIdAndAtivaTrue(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ofertaService.criar(contaSebo, request(new BigDecimal("40.00"), inicio, fim, true)))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    @DisplayName("Deve listar apenas ofertas ativas retornadas pelo repositorio")
    void listarAtivas_retornaPagina() {
        Oferta oferta = Oferta.builder()
                .produto(produto)
                .precoPromocional(new BigDecimal("39.90"))
                .ativa(true)
                .build();
        var pageable = PageRequest.of(0, 10);
        when(ofertaRepository.findOfertasAtivas(any(OffsetDateTime.class), any()))
                .thenReturn(new PageImpl<>(List.of(oferta), pageable, 1));

        var resultado = ofertaService.listarAtivas(pageable);

        assertThat(resultado.getContent()).singleElement()
                .satisfies(item -> assertThat(item.precoPromocional()).isEqualByComparingTo("39.90"));
    }

    @Test
    @DisplayName("Deve listar as ofertas do sebo autenticado")
    void listarDoSebo_retornaPagina() {
        Oferta oferta = Oferta.builder()
                .produto(produto)
                .precoPromocional(new BigDecimal("29.90"))
                .ativa(false)
                .build();
        var pageable = PageRequest.of(0, 20);
        when(ofertaRepository.findDoSebo(1L, pageable))
                .thenReturn(new PageImpl<>(List.of(oferta), pageable, 1));

        var resultado = ofertaService.listarDoSebo(contaSebo, pageable);

        assertThat(resultado.getContent()).singleElement()
                .satisfies(item -> {
                    assertThat(item.produtoId()).isEqualTo(10L);
                    assertThat(item.ativa()).isFalse();
                });
        verify(ofertaRepository).findDoSebo(1L, pageable);
    }

    @Test
    @DisplayName("Deve desativar oferta do proprio sebo")
    void remover_sucesso_desativaOferta() {
        Oferta oferta = Oferta.builder().produto(produto).ativa(true).build();
        when(ofertaRepository.findById(20L)).thenReturn(Optional.of(oferta));

        ofertaService.remover(contaSebo, 20L);

        assertThat(oferta.getAtiva()).isFalse();
        verify(seboService).registrarAtividade(produto.getSebo());
    }

    @Test
    @DisplayName("Deve atualizar oferta e respeitar ativa false")
    void atualizar_sucesso_comAtivaFalse() {
        Oferta oferta = Oferta.builder().produto(produto).ativa(true).build();
        ReflectionTestUtils.setField(oferta, "id", 20L);
        when(ofertaRepository.findById(20L)).thenReturn(Optional.of(oferta));
        when(produtoRepository.findById(10L)).thenReturn(Optional.of(produto));

        var response = ofertaService.atualizar(contaSebo, 20L,
                request(new BigDecimal("35.00"), OffsetDateTime.now(), OffsetDateTime.now().plusDays(2), false));

        assertThat(response.precoPromocional()).isEqualByComparingTo("35.00");
        assertThat(response.ativa()).isFalse();
        assertThat(oferta.getAtiva()).isFalse();
        verify(seboService).registrarAtividade(produto.getSebo());
    }

    @Test
    @DisplayName("Deve impedir remocao de oferta de outro sebo")
    void remover_ofertaDeOutroSebo_lancaForbidden() {
        Account outraConta = account(2L, "Outro", AccountType.SEBO);
        Oferta oferta = Oferta.builder().produto(produto).ativa(true).build();
        when(ofertaRepository.findById(20L)).thenReturn(Optional.of(oferta));

        assertThatThrownBy(() -> ofertaService.remover(outraConta, 20L))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getStatus()).isEqualTo(HttpStatus.FORBIDDEN));
        assertThat(oferta.getAtiva()).isTrue();
    }

    private OfertaRequest request(BigDecimal preco, OffsetDateTime inicio, OffsetDateTime fim, Boolean ativa) {
        return new OfertaRequest(10L, preco, "Oferta especial", inicio, fim, ativa);
    }

    private Account account(Long id, String nome, AccountType type) {
        Account account = Account.builder()
                .name(nome)
                .email(nome.toLowerCase() + "@email.com")
                .passwordHash("hash")
                .type(type)
                .emailVerificado(true)
                .build();
        ReflectionTestUtils.setField(account, "id", id);
        return account;
    }
}
