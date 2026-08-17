package com.vitral.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Year;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;

import com.vitral.dto.ConfirmarPedidoRequest;
import com.vitral.dto.FaturamentoMensalResponse;
import com.vitral.dto.PedidoResponse;
import com.vitral.entity.Account;
import com.vitral.enumerations.AccountType;
import com.vitral.enumerations.FormaPagamento;
import com.vitral.enumerations.StatusPagamento;
import com.vitral.enumerations.StatusPedido;
import com.vitral.security.AccountUserDetails;
import com.vitral.service.PedidoService;

@ExtendWith(MockitoExtension.class)
class PedidoControllerTest {

    @Mock
    private PedidoService pedidoService;

    @InjectMocks
    private PedidoController controller;

    @Test
    void confirmarPedidoRetornaCreated() {
        Account account = account(AccountType.USUARIO);
        AccountUserDetails principal = new AccountUserDetails(account);
        ConfirmarPedidoRequest request = new ConfirmarPedidoRequest(FormaPagamento.CARTAO, "4111111111111111");
        PedidoResponse response = pedido(StatusPedido.AGUARDANDO_CONFIRMACAO);
        when(pedidoService.confirmarPedido(account, request)).thenReturn(response);

        var result = controller.confirmarPedido(principal, request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(result.getBody()).isSameAs(response);
        verify(pedidoService).confirmarPedido(account, request);
    }

    @Test
    void confirmarCompraCancelarEReembolsarDelegamStatusCorreto() {
        Account account = account(AccountType.SEBO);
        AccountUserDetails principal = new AccountUserDetails(account);
        PedidoResponse confirmado = pedido(StatusPedido.CONFIRMADO);
        PedidoResponse reembolsado = pedido(StatusPedido.REEMBOLSADO);
        when(pedidoService.confirmarCompra(account, 20L, StatusPedido.CONFIRMADO)).thenReturn(confirmado);
        when(pedidoService.reembolsarPedido(account, 20L)).thenReturn(reembolsado);

        assertThat(controller.confirmarCompra(principal, 20L, StatusPedido.CONFIRMADO).getBody()).isSameAs(confirmado);
        assertThat(controller.reembolsarPedido(principal, 20L).getBody()).isSameAs(reembolsado);
        verify(pedidoService).confirmarCompra(account, 20L, StatusPedido.CONFIRMADO);
        verify(pedidoService).reembolsarPedido(account, 20L);
    }

    @Test
    void historicosConvertemPageParaContratoEstavel() {
        Account usuario = account(AccountType.USUARIO);
        Account sebo = account(AccountType.SEBO);
        var pageable = PageRequest.of(0, 10);
        PedidoResponse pedido = pedido(StatusPedido.CONFIRMADO);
        when(pedidoService.historicoPedidosUsuario(usuario, StatusPedido.CONFIRMADO, pageable))
                .thenReturn(new PageImpl<>(List.of(pedido), pageable, 1));
        when(pedidoService.historicoVendasSebo(sebo, null, pageable))
                .thenReturn(new PageImpl<>(List.of(pedido), pageable, 1));

        var historicoUsuario = controller.historicoPedidosUsuario(new AccountUserDetails(usuario),
                StatusPedido.CONFIRMADO, pageable);
        var vendasSebo = controller.historicoVendasSebo(new AccountUserDetails(sebo), null, pageable);

        assertThat(historicoUsuario.getBody().content()).containsExactly(pedido);
        assertThat(vendasSebo.getBody().content()).containsExactly(pedido);
    }

    @Test
    void faturamentoMensalUsaAnoAtualQuandoParametroNaoVem() {
        Account account = account(AccountType.SEBO);
        AccountUserDetails principal = new AccountUserDetails(account);
        List<FaturamentoMensalResponse> response = List.of(faturamento(Year.now().getValue(), 7));
        when(pedidoService.faturamentoMensal(account, Year.now().getValue())).thenReturn(response);

        var result = controller.faturamentoMensal(principal, null);

        assertThat(result.getBody()).isSameAs(response);
        verify(pedidoService).faturamentoMensal(account, Year.now().getValue());
    }

    @Test
    void faturamentoMensalUsaAnoSolicitado() {
        Account account = account(AccountType.SEBO);
        AccountUserDetails principal = new AccountUserDetails(account);
        List<FaturamentoMensalResponse> response = List.of(faturamento(2026, 6));
        when(pedidoService.faturamentoMensal(account, 2026)).thenReturn(response);

        var result = controller.faturamentoMensal(principal, 2026);

        assertThat(result.getBody()).isSameAs(response);
        verify(pedidoService).faturamentoMensal(account, 2026);
    }

    private Account account(AccountType type) {
        return Account.builder().type(type).email("user@vitral.com").passwordHash("hash").ativo(true).build();
    }

    private PedidoResponse pedido(StatusPedido status) {
        return new PedidoResponse(20L, 1L, 10L, status, FormaPagamento.CARTAO, StatusPagamento.APROVADO,
                BigDecimal.valueOf(100), null, null, null, null, null, List.of());
    }

    private FaturamentoMensalResponse faturamento(int ano, int mes) {
        return new FaturamentoMensalResponse(ano, mes, BigDecimal.valueOf(100), BigDecimal.TEN,
                BigDecimal.valueOf(90), BigDecimal.valueOf(90));
    }
}
