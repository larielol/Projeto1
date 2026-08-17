package com.vitral.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import com.vitral.dto.ConfirmarPedidoRequest;
import com.vitral.dto.FaturamentoMensalResponse;
import com.vitral.dto.PedidoResponse;
import com.vitral.entity.Account;
import com.vitral.entity.CestaItem;
import com.vitral.entity.Pedido;
import com.vitral.entity.PedidoItem;
import com.vitral.entity.Produto;
import com.vitral.entity.Sebo;
import com.vitral.enumerations.AccountType;
import com.vitral.enumerations.CondicaoProduto;
import com.vitral.enumerations.FormaPagamento;
import com.vitral.enumerations.StatusPagamento;
import com.vitral.enumerations.StatusPedido;
import com.vitral.exception.BusinessException;
import com.vitral.exception.ResourceNotFoundException;
import com.vitral.repository.CestaItemRepository;
import com.vitral.repository.PedidoRepository;
import com.vitral.repository.SeboRepository;

@ExtendWith(MockitoExtension.class)
class PedidoServiceTest {

    @Mock
    private PedidoRepository pedidoRepository;

    @Mock
    private CestaItemRepository cestaItemRepository;

    @Mock
    private SeboRepository seboRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private PrecoService precoService;

    @Mock
    private MovimentacaoEstoqueService movimentacaoEstoqueService;

    @Mock
    private PerfilCompraValidator perfilCompraValidator;

    @Mock
    private RecomendacaoEventoService recomendacaoEventoService;

    @InjectMocks
    private PedidoService pedidoService;

    private void setId(Object entity, Long id) {
        ReflectionTestUtils.setField(entity, "id", id);
    }

    private ConfirmarPedidoRequest pagamentoPix() {
        return new ConfirmarPedidoRequest(FormaPagamento.PIX, null);
    }

    private Account buildUsuario() {
        Account account = Account.builder()
                .name("Carlos")
                .email("carlos@email.com")
                .passwordHash("hash")
                .type(AccountType.USUARIO)
                .emailVerificado(true)
                .build();
        setId(account, 10L);
        return account;
    }

    private Account buildSeboAccount(Long id) {
        Account account = Account.builder()
                .name("Sebo Central")
                .email("sebo" + id + "@email.com")
                .passwordHash("hash")
                .type(AccountType.SEBO)
                .emailVerificado(true)
                .build();
        setId(account, id);
        return account;
    }

    private Sebo buildSebo(Account seboAccount, Long id) {
        Sebo sebo = Sebo.builder().account(seboAccount).build();
        setId(sebo, id);
        return sebo;
    }

    private Produto buildProduto(Sebo sebo) {
        return Produto.builder()
                .sebo(sebo)
                .titulo("O Cortiço")
                .autor("Aluísio Azevedo")
                .preco(new BigDecimal("25.00"))
                .condicao(CondicaoProduto.USADO)
                .ativo(true)
                .build();
    }

    @Test
    @DisplayName("Deve lançar BusinessException quando a cesta está vazia")
    void confirmarPedido_cestaVazia_lancaBusinessException() {
        Account account = buildUsuario();
        when(cestaItemRepository.findByAccountIdComProdutoESebo(10L)).thenReturn(List.of());

        assertThatThrownBy(() -> pedidoService.confirmarPedido(account, pagamentoPix()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("cesta esta vazia");

        verify(pedidoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lançar BusinessException quando a cesta contém produto inativo")
    void confirmarPedido_possuiProdutoInativo_lancaBusinessException() {
        Account account = buildUsuario();
        Sebo sebo = buildSebo(buildSeboAccount(1L), 1L);
        Produto produtoInativo = Produto.builder()
                .sebo(sebo)
                .titulo("Livro Esgotado")
                .preco(new BigDecimal("10.00"))
                .condicao(CondicaoProduto.USADO)
                .ativo(false)
                .build();
        CestaItem item = CestaItem.builder().account(account).produto(produtoInativo).build();
        when(cestaItemRepository.findByAccountIdComProdutoESebo(10L)).thenReturn(List.of(item));

        assertThatThrownBy(() -> pedidoService.confirmarPedido(account, pagamentoPix()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("nao estao mais disponiveis");

        verify(pedidoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lançar BusinessException quando produtos da cesta são de sebos distintos")
    void confirmarPedido_produtosDeSebosDistintos_lancaBusinessException() {
        Account account = buildUsuario();
        Sebo sebo1 = buildSebo(buildSeboAccount(1L), 1L);
        Sebo sebo2 = buildSebo(buildSeboAccount(2L), 2L);
        CestaItem item1 = CestaItem.builder().account(account).produto(buildProduto(sebo1)).build();
        CestaItem item2 = CestaItem.builder().account(account).produto(buildProduto(sebo2)).build();
        when(cestaItemRepository.findByAccountIdComProdutoESebo(10L)).thenReturn(List.of(item1, item2));

        assertThatThrownBy(() -> pedidoService.confirmarPedido(account, pagamentoPix()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("mesmo sebo");

        verify(pedidoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve criar pedido, limpar cesta e retornar response ao confirmar com sucesso")
    void confirmarPedido_sucesso_criaPedidoLimpaERetornaResponse() {
        Account account = buildUsuario();
        Sebo sebo = buildSebo(buildSeboAccount(1L), 1L);
        Produto produto = buildProduto(sebo);
        setId(produto, 50L);
        CestaItem item = CestaItem.builder().account(account).produto(produto).build();
        when(cestaItemRepository.findByAccountIdComProdutoESebo(10L)).thenReturn(List.of(item));
        when(pedidoRepository.sumQuantidadeReservadaEmPedidosPendentes(50L)).thenReturn(0L);
        when(seboRepository.findById(1L)).thenReturn(Optional.of(sebo));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(inv -> inv.getArgument(0));
        when(precoService.precoEfetivo(produto)).thenReturn(new BigDecimal("25.00"));

        PedidoResponse response = pedidoService.confirmarPedido(account, pagamentoPix());

        assertThat(response.status()).isEqualTo(StatusPedido.AGUARDANDO_CONFIRMACAO);
        assertThat(response.formaPagamento()).isEqualTo(FormaPagamento.PIX);
        assertThat(response.statusPagamento()).isEqualTo(StatusPagamento.APROVADO);
        assertThat(response.pagoEm()).isNotNull();
        assertThat(response.total()).isEqualByComparingTo("25.00");
        assertThat(response.itens()).hasSize(1);
        assertThat(response.itens().getFirst().tituloSnapshot()).isEqualTo("O Cortiço");
        verify(cestaItemRepository).deleteByAccountId(10L);
    }

    @Test
    @DisplayName("Deve recusar o pedido quando o pagamento no cartao for reprovado (mock)")
    void confirmarPedido_pagamentoRecusado_lancaBusinessException() {
        Account account = buildUsuario();
        Sebo sebo = buildSebo(buildSeboAccount(1L), 1L);
        Produto produto = buildProduto(sebo);
        setId(produto, 50L);
        CestaItem item = CestaItem.builder().account(account).produto(produto).build();
        when(cestaItemRepository.findByAccountIdComProdutoESebo(10L)).thenReturn(List.of(item));
        when(pedidoRepository.sumQuantidadeReservadaEmPedidosPendentes(50L)).thenReturn(0L);

        assertThatThrownBy(() -> pedidoService.confirmarPedido(account,
                new ConfirmarPedidoRequest(FormaPagamento.CARTAO, "4111 1111 1111 0000")))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getStatus())
                        .isEqualTo(HttpStatus.PAYMENT_REQUIRED));

        verify(pedidoRepository, never()).save(any());
        verify(cestaItemRepository, never()).deleteByAccountId(10L);
    }

    @Test
    @DisplayName("Deve notificar o sebo por e-mail ao criar o pedido")
    void confirmarPedido_notificaSeboPorEmail() {
        Account account = buildUsuario();
        Sebo sebo = buildSebo(buildSeboAccount(1L), 1L);
        Produto produto = buildProduto(sebo);
        setId(produto, 50L);
        CestaItem item = CestaItem.builder().account(account).produto(produto).build();
        when(cestaItemRepository.findByAccountIdComProdutoESebo(10L)).thenReturn(List.of(item));
        when(pedidoRepository.sumQuantidadeReservadaEmPedidosPendentes(50L)).thenReturn(0L);
        when(seboRepository.findById(1L)).thenReturn(Optional.of(sebo));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(inv -> inv.getArgument(0));
        when(precoService.precoEfetivo(produto)).thenReturn(new BigDecimal("25.00"));

        pedidoService.confirmarPedido(account, pagamentoPix());

        verify(emailService).enviarNotificacaoNovoPedido("sebo1@email.com", null, "Carlos", new BigDecimal("25.00"));
    }

    @Test
    @DisplayName("Deve usar o preco promocional da oferta no total e no snapshot do pedido")
    void confirmarPedido_comOferta_usaPrecoPromocional() {
        Account account = buildUsuario();
        Sebo sebo = buildSebo(buildSeboAccount(1L), 1L);
        Produto produto = buildProduto(sebo);
        setId(produto, 50L);
        CestaItem item = CestaItem.builder().account(account).produto(produto).build();
        when(cestaItemRepository.findByAccountIdComProdutoESebo(10L)).thenReturn(List.of(item));
        when(pedidoRepository.sumQuantidadeReservadaEmPedidosPendentes(50L)).thenReturn(0L);
        when(seboRepository.findById(1L)).thenReturn(Optional.of(sebo));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(inv -> inv.getArgument(0));
        when(precoService.precoEfetivo(produto)).thenReturn(new BigDecimal("15.00"));

        PedidoResponse response = pedidoService.confirmarPedido(account, pagamentoPix());

        assertThat(response.total()).isEqualByComparingTo("15.00");
        assertThat(response.itens().getFirst().precoSnapshot()).isEqualByComparingTo("15.00");
    }

    @Test
    @DisplayName("Deve lançar BusinessException quando produto da cesta ja esta reservado")
    void confirmarPedido_produtoReservado_lancaBusinessException() {
        Account account = buildUsuario();
        Sebo sebo = buildSebo(buildSeboAccount(1L), 1L);
        Produto produto = buildProduto(sebo);
        setId(produto, 50L);
        CestaItem item = CestaItem.builder().account(account).produto(produto).build();
        when(cestaItemRepository.findByAccountIdComProdutoESebo(10L)).thenReturn(List.of(item));
        when(pedidoRepository.sumQuantidadeReservadaEmPedidosPendentes(50L)).thenReturn(1L);

        assertThatThrownBy(() -> pedidoService.confirmarPedido(account, pagamentoPix()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("estoque suficiente");

        verify(pedidoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lançar BusinessException quando pedido não pertence ao sebo da conta")
    void confirmarCompra_pedidoNaoPertenceAoSebo_lancaBusinessException() {
        Account seboAccount = buildSeboAccount(1L);
        Account outroSeboAccount = buildSeboAccount(2L);
        Sebo outraSebo = buildSebo(outroSeboAccount, 2L);
        Pedido pedido = Pedido.builder()
                .account(buildUsuario())
                .sebo(outraSebo)
                .status(StatusPedido.AGUARDANDO_CONFIRMACAO)
                .total(new BigDecimal("25.00"))
                .build();
        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));

        assertThatThrownBy(() -> pedidoService.confirmarCompra(seboAccount, 1L, StatusPedido.CONFIRMADO))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("Deve lançar BusinessException ao tentar alterar pedido já confirmado")
    void confirmarCompra_pedidoJaConfirmado_lancaBusinessException() {
        Account seboAccount = buildSeboAccount(1L);
        Sebo sebo = buildSebo(seboAccount, 1L);
        Pedido pedido = Pedido.builder()
                .account(buildUsuario())
                .sebo(sebo)
                .status(StatusPedido.CONFIRMADO)
                .total(new BigDecimal("25.00"))
                .build();
        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));

        assertThatThrownBy(() -> pedidoService.confirmarCompra(seboAccount, 1L, StatusPedido.CANCELADO))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("AGUARDANDO_CONFIRMACAO");
    }

    @Test
    @DisplayName("Deve lançar BusinessException ao informar status inválido")
    void confirmarCompra_statusInvalido_lancaBusinessException() {
        Account seboAccount = buildSeboAccount(1L);
        Sebo sebo = buildSebo(seboAccount, 1L);
        Pedido pedido = Pedido.builder()
                .account(buildUsuario())
                .sebo(sebo)
                .status(StatusPedido.AGUARDANDO_CONFIRMACAO)
                .total(new BigDecimal("25.00"))
                .build();
        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));

        assertThatThrownBy(() -> pedidoService.confirmarCompra(seboAccount, 1L, StatusPedido.AGUARDANDO_CONFIRMACAO))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Status invalido");
    }

    @Test
    @DisplayName("Deve atualizar status e retornar response ao confirmar compra com sucesso")
    void confirmarCompra_sucesso_atualizaStatusERetornaResponse() {
        Account seboAccount = buildSeboAccount(1L);
        Sebo sebo = buildSebo(seboAccount, 1L);
        Produto produto = buildProduto(sebo);
        Pedido pedido = Pedido.builder()
                .account(buildUsuario())
                .sebo(sebo)
                .status(StatusPedido.AGUARDANDO_CONFIRMACAO)
                .total(new BigDecimal("25.00"))
                .build();
        pedido.getItens().add(PedidoItem.builder()
                .pedido(pedido)
                .produto(produto)
                .tituloSnapshot("O Cortiço")
                .precoSnapshot(new BigDecimal("25.00"))
                .build());
        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));

        PedidoResponse response = pedidoService.confirmarCompra(seboAccount, 1L, StatusPedido.CONFIRMADO);

        assertThat(response.status()).isEqualTo(StatusPedido.CONFIRMADO);
        assertThat(pedido.getStatus()).isEqualTo(StatusPedido.CONFIRMADO);
        assertThat(pedido.getConfirmadoEm()).isNotNull();
        assertThat(produto.getAtivo()).isFalse();
        verify(recomendacaoEventoService).registrarCompra(pedido.getAccount(), pedido, produto);
    }

    @Test
    @DisplayName("Deve descontar quantidade vendida e manter produto ativo quando ainda houver estoque")
    void confirmarCompra_estoqueParcial_descontaQuantidade() {
        Account seboAccount = buildSeboAccount(1L);
        Sebo sebo = buildSebo(seboAccount, 1L);
        Produto produto = buildProduto(sebo);
        produto.setEstoque(5);
        Pedido pedido = Pedido.builder()
                .account(buildUsuario())
                .sebo(sebo)
                .status(StatusPedido.AGUARDANDO_CONFIRMACAO)
                .total(new BigDecimal("50.00"))
                .build();
        pedido.getItens().add(PedidoItem.builder()
                .pedido(pedido)
                .produto(produto)
                .tituloSnapshot("O Cortiço")
                .precoSnapshot(new BigDecimal("25.00"))
                .quantidade(2)
                .build());
        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));

        pedidoService.confirmarCompra(seboAccount, 1L, StatusPedido.CONFIRMADO);

        assertThat(produto.getEstoque()).isEqualTo(3);
        assertThat(produto.getAtivo()).isTrue();
    }

    @Test
    @DisplayName("Usuario deve cancelar o proprio pedido aguardando confirmacao")
    void cancelarPedidoUsuario_sucesso_atualizaStatus() {
        Account account = buildUsuario();
        Sebo sebo = buildSebo(buildSeboAccount(1L), 1L);
        Pedido pedido = Pedido.builder()
                .account(account)
                .sebo(sebo)
                .status(StatusPedido.AGUARDANDO_CONFIRMACAO)
                .total(new BigDecimal("25.00"))
                .build();
        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));

        PedidoResponse response = pedidoService.cancelarPedidoUsuario(account, 1L);

        assertThat(response.status()).isEqualTo(StatusPedido.CANCELADO);
        assertThat(pedido.getStatus()).isEqualTo(StatusPedido.CANCELADO);
        assertThat(pedido.getCanceladoEm()).isNotNull();
    }

    @Test
    @DisplayName("Deve impedir usuario de cancelar pedido de outra conta")
    void cancelarPedidoUsuario_pedidoDeOutraConta_lancaForbidden() {
        Account usuario = buildUsuario();
        Account outroUsuario = buildUsuario();
        setId(outroUsuario, 11L);
        Pedido pedido = Pedido.builder()
                .account(outroUsuario)
                .sebo(buildSebo(buildSeboAccount(1L), 1L))
                .status(StatusPedido.AGUARDANDO_CONFIRMACAO)
                .total(BigDecimal.TEN)
                .build();
        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));

        assertThatThrownBy(() -> pedidoService.cancelarPedidoUsuario(usuario, 1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("nao pertence");
    }

    @Test
    @DisplayName("Deve impedir cancelamento pelo usuario depois da confirmacao")
    void cancelarPedidoUsuario_pedidoConfirmado_lancaBusinessException() {
        Account usuario = buildUsuario();
        Pedido pedido = Pedido.builder()
                .account(usuario)
                .sebo(buildSebo(buildSeboAccount(1L), 1L))
                .status(StatusPedido.CONFIRMADO)
                .total(BigDecimal.TEN)
                .build();
        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));

        assertThatThrownBy(() -> pedidoService.cancelarPedidoUsuario(usuario, 1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("aguardando confirmacao");
    }

    @Test
    @DisplayName("Deve retornar página de pedidos do usuário ordenados por data decrescente")
    void historicoPedidosUsuario_retornaPagedResponse() {
        Account account = buildUsuario();
        Pageable pageable = PageRequest.of(0, 10);
        Page<Pedido> paginaVazia = new PageImpl<>(List.of());
        when(pedidoRepository.findByAccountIdOrderByCreatedAtDesc(eq(10L), eq(pageable)))
                .thenReturn(paginaVazia);

        Page<PedidoResponse> resultado = pedidoService.historicoPedidosUsuario(account, null, pageable);

        assertThat(resultado.getContent()).isEmpty();
        verify(pedidoRepository).findByAccountIdOrderByCreatedAtDesc(10L, pageable);
    }

    @Test
    @DisplayName("Deve filtrar histórico do usuário por status")
    void historicoPedidosUsuario_comStatus_usaRepositorioFiltrado() {
        Account account = buildUsuario();
        Pageable pageable = PageRequest.of(0, 10);
        when(pedidoRepository.findByAccountIdAndStatusOrderByCreatedAtDesc(10L, StatusPedido.CONFIRMADO, pageable))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        Page<PedidoResponse> resultado = pedidoService.historicoPedidosUsuario(account, StatusPedido.CONFIRMADO, pageable);

        assertThat(resultado).isEmpty();
        verify(pedidoRepository).findByAccountIdAndStatusOrderByCreatedAtDesc(10L, StatusPedido.CONFIRMADO, pageable);
    }

    @Test
    @DisplayName("Deve lançar ResourceNotFoundException quando sebo não encontrado no histórico de vendas")
    void historicoVendasSebo_seboNaoEncontrado_lancaResourceNotFoundException() {
        Account seboAccount = buildSeboAccount(1L);
        Pageable pageable = PageRequest.of(0, 10);
        when(seboRepository.findByAccountId(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> pedidoService.historicoVendasSebo(seboAccount, null, pageable))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Deve retornar historico de vendas do sebo autenticado")
    void historicoVendasSebo_retornaPagina() {
        Account seboAccount = buildSeboAccount(1L);
        Sebo sebo = buildSebo(seboAccount, 5L);
        Pageable pageable = PageRequest.of(0, 10);
        when(seboRepository.findByAccountId(1L)).thenReturn(Optional.of(sebo));
        when(pedidoRepository.findBySeboIdOrderByCreatedAtDesc(5L, pageable))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        Page<PedidoResponse> resultado = pedidoService.historicoVendasSebo(seboAccount, null, pageable);

        assertThat(resultado).isEmpty();
        verify(pedidoRepository).findBySeboIdOrderByCreatedAtDesc(5L, pageable);
    }

    @Test
    @DisplayName("Deve filtrar histórico de vendas por status")
    void historicoVendasSebo_comStatus_usaRepositorioFiltrado() {
        Account seboAccount = buildSeboAccount(1L);
        Sebo sebo = buildSebo(seboAccount, 5L);
        Pageable pageable = PageRequest.of(0, 10);
        when(seboRepository.findByAccountId(1L)).thenReturn(Optional.of(sebo));
        when(pedidoRepository.findBySeboIdAndStatusOrderByCreatedAtDesc(5L, StatusPedido.CANCELADO, pageable))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        Page<PedidoResponse> resultado = pedidoService.historicoVendasSebo(seboAccount, StatusPedido.CANCELADO, pageable);

        assertThat(resultado).isEmpty();
        verify(pedidoRepository).findBySeboIdAndStatusOrderByCreatedAtDesc(5L, StatusPedido.CANCELADO, pageable);
    }

    @Test
    @DisplayName("Faturamento mensal deve rejeitar ano fora do intervalo")
    void faturamentoMensal_anoInvalido_lancaBadRequest() {
        Account seboAccount = buildSeboAccount(1L);

        assertThatThrownBy(() -> pedidoService.faturamentoMensal(seboAccount, 1999))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Ano invalido");
    }

    @Test
    @DisplayName("Faturamento mensal deve consolidar vendas online e reembolsos")
    void faturamentoMensal_consolidaOrigens() {
        Account seboAccount = buildSeboAccount(1L);
        Sebo sebo = buildSebo(seboAccount, 5L);
        when(seboRepository.findByAccountId(1L)).thenReturn(Optional.of(sebo));
        when(pedidoRepository.vendasOnlineMensais(5L, 2026))
                .thenReturn(List.<Object[]>of(new Object[] { 3, new BigDecimal("100.00") }));
        when(pedidoRepository.reembolsosMensais(5L, 2026))
                .thenReturn(List.<Object[]>of(new Object[] { 3, new BigDecimal("25.00") }));

        List<FaturamentoMensalResponse> resultado = pedidoService.faturamentoMensal(seboAccount, 2026);

        assertThat(resultado).hasSize(12);
        assertThat(resultado.get(2).vendasOnline()).isEqualByComparingTo("100.00");
        assertThat(resultado.get(2).reembolsos()).isEqualByComparingTo("25.00");
        assertThat(resultado.get(2).total()).isEqualByComparingTo("75.00");
        assertThat(resultado.get(2).totalLiquido()).isEqualByComparingTo("75.00");
        assertThat(resultado.get(3).total()).isEqualByComparingTo("0.00");
    }
}
