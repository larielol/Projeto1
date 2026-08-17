package com.vitral.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import com.vitral.dto.MovimentacaoEstoqueResponse;
import com.vitral.entity.Account;
import com.vitral.entity.MovimentacaoEstoque;
import com.vitral.entity.Produto;
import com.vitral.entity.Sebo;
import com.vitral.enumerations.AccountType;
import com.vitral.enumerations.TipoMovimentacaoEstoque;
import com.vitral.repository.MovimentacaoEstoqueRepository;

@ExtendWith(MockitoExtension.class)
class MovimentacaoEstoqueServiceTest {

    @Mock MovimentacaoEstoqueRepository repository;
    @Mock SeboService seboService;
    @InjectMocks MovimentacaoEstoqueService service;

    private Account operador;
    private Produto produto;

    @BeforeEach
    void setup() {
        operador = Account.builder().name("Sebo").email("sebo@test.com").type(AccountType.SEBO).build();
        ReflectionTestUtils.setField(operador, "id", 1L);
        Sebo sebo = Sebo.builder().account(operador).build();
        ReflectionTestUtils.setField(sebo, "id", 2L);
        produto = Produto.builder().sebo(sebo).estoque(5).ativo(true).preco(new BigDecimal("30.00")).build();
        ReflectionTestUtils.setField(produto, "id", 3L);
        lenient().when(repository.save(org.mockito.ArgumentMatchers.any())).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    @DisplayName("Deve registrar a movimentacao com os saldos e o valor total calculado")
    void shouldRegisterMovementWithBalancesAndTotalValue() {
        ArgumentCaptor<MovimentacaoEstoque> captor = ArgumentCaptor.forClass(MovimentacaoEstoque.class);

        service.registrarAlteracao(produto, operador, TipoMovimentacaoEstoque.ENTRADA, 5, 0, 5,
                new BigDecimal("30.00"), "Estoque inicial");

        verify(repository).save(captor.capture());
        MovimentacaoEstoque salva = captor.getValue();
        assertThat(salva.getProduto()).isSameAs(produto);
        assertThat(salva.getSebo()).isSameAs(produto.getSebo());
        assertThat(salva.getOperador()).isSameAs(operador);
        assertThat(salva.getTipo()).isEqualTo(TipoMovimentacaoEstoque.ENTRADA);
        assertThat(salva.getEstoqueAntes()).isZero();
        assertThat(salva.getEstoqueDepois()).isEqualTo(5);
        assertThat(salva.getValorTotal()).isEqualByComparingTo("150.00");
        assertThat(salva.getMovimentacaoOrigem()).isNull();
    }

    @Test
    @DisplayName("Deve registrar a movimentacao sem valor total quando nao houver valor unitario")
    void shouldRegisterMovementWithoutTotalWhenUnitPriceIsMissing() {
        ArgumentCaptor<MovimentacaoEstoque> captor = ArgumentCaptor.forClass(MovimentacaoEstoque.class);

        service.registrarAlteracao(produto, operador, TipoMovimentacaoEstoque.AJUSTE, 2, 5, 3, null, "Ajuste");

        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getValorTotal()).isNull();
        assertThat(captor.getValue().getObservacao()).isEqualTo("Ajuste");
    }

    @Test
    @DisplayName("Deve listar as movimentacoes do sebo autenticado")
    void shouldListMovementsOfAuthenticatedSebo() {
        MovimentacaoEstoque movimentacao = MovimentacaoEstoque.builder()
                .produto(produto).sebo(produto.getSebo()).operador(operador)
                .tipo(TipoMovimentacaoEstoque.ENTRADA).quantidade(5).estoqueAntes(0).estoqueDepois(5)
                .valorUnitario(new BigDecimal("30.00")).valorTotal(new BigDecimal("150.00")).build();
        ReflectionTestUtils.setField(movimentacao, "id", 9L);
        PageRequest pageable = PageRequest.of(0, 20);
        when(repository.findBySeboAccountIdOrderByCreatedAtDesc(1L, pageable))
                .thenReturn(new PageImpl<>(List.of(movimentacao), pageable, 1));

        List<MovimentacaoEstoqueResponse> resultado = service.listar(operador, pageable).getContent();

        assertThat(resultado).singleElement().satisfies(item -> {
            assertThat(item.id()).isEqualTo(9L);
            assertThat(item.produtoId()).isEqualTo(3L);
            assertThat(item.tipo()).isEqualTo(TipoMovimentacaoEstoque.ENTRADA);
            assertThat(item.estoqueDepois()).isEqualTo(5);
        });
    }
}
