package com.vitral.controller;

import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

import com.vitral.dto.ConfirmarPedidoRequest;
import com.vitral.dto.FaturamentoMensalResponse;
import java.util.List;
import java.time.Year;
import com.vitral.dto.PageResponse;
import com.vitral.dto.PedidoResponse;
import com.vitral.enumerations.StatusPedido;
import com.vitral.security.AccountUserDetails;
import com.vitral.service.PedidoService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/pedidos")
@RequiredArgsConstructor
@Tag(name = "Pedidos", description = "Gestao de pedidos entre usuario e sebo")
public class PedidoController {

    private final PedidoService pedidoService;

    @PostMapping
    @PreAuthorize("hasRole('USUARIO')")
    @Operation(summary = "Confirma o pedido e paga (mock) com os itens atuais da cesta (usuario)")
    public ResponseEntity<PedidoResponse> confirmarPedido(
            @AuthenticationPrincipal AccountUserDetails principal,
            @Valid @RequestBody ConfirmarPedidoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(pedidoService.confirmarPedido(principal.getAccount(), request));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('SEBO')")
    @Operation(summary = "Sebo confirma ou cancela um pedido (CONFIRMADO | CANCELADO)")
    public ResponseEntity<PedidoResponse> confirmarCompra(
            @AuthenticationPrincipal AccountUserDetails principal,
            @PathVariable Long id,
            @RequestParam StatusPedido status) {
        return ResponseEntity.ok(pedidoService.confirmarCompra(principal.getAccount(), id, status));
    }

    @PutMapping("/{id}/cancelar")
    @PreAuthorize("hasRole('USUARIO')")
    @Operation(summary = "Usuario cancela o proprio pedido enquanto aguarda confirmacao")
    public ResponseEntity<PedidoResponse> cancelarPedidoUsuario(
            @AuthenticationPrincipal AccountUserDetails principal,
            @PathVariable Long id) {
        return ResponseEntity.ok(pedidoService.cancelarPedidoUsuario(principal.getAccount(), id));
    }

    @PutMapping("/{id}/reembolsar")
    @PreAuthorize("hasRole('SEBO')")
    @Operation(summary = "Reembolsa integralmente um pedido confirmado e repoe o estoque")
    public ResponseEntity<PedidoResponse> reembolsarPedido(
            @AuthenticationPrincipal AccountUserDetails principal,
            @PathVariable Long id) {
        return ResponseEntity.ok(pedidoService.reembolsarPedido(principal.getAccount(), id));
    }

    @GetMapping("/meus-pedidos")
    @PreAuthorize("hasRole('USUARIO')")
    @Operation(summary = "Historico de pedidos do usuario autenticado")
    public ResponseEntity<PageResponse<PedidoResponse>> historicoPedidosUsuario(
            @AuthenticationPrincipal AccountUserDetails principal,
            @RequestParam(required = false) StatusPedido status,
            Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(
                pedidoService.historicoPedidosUsuario(principal.getAccount(), status, pageable)));
    }

    @GetMapping("/vendas")
    @PreAuthorize("hasRole('SEBO')")
    @Operation(summary = "Historico de vendas do sebo autenticado")
    public ResponseEntity<PageResponse<PedidoResponse>> historicoVendasSebo(
            @AuthenticationPrincipal AccountUserDetails principal,
            @RequestParam(required = false) StatusPedido status,
            Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(
                pedidoService.historicoVendasSebo(principal.getAccount(), status, pageable)));
    }

    @GetMapping("/faturamento-mensal")
    @PreAuthorize("hasRole('SEBO')")
    @Operation(summary = "Faturamento mensal de pedidos confirmados do sebo autenticado")
    public ResponseEntity<List<FaturamentoMensalResponse>> faturamentoMensal(
            @AuthenticationPrincipal AccountUserDetails principal,
            @RequestParam(required = false) Integer ano) {
        int anoConsulta = ano == null ? Year.now().getValue() : ano;
        return ResponseEntity.ok(pedidoService.faturamentoMensal(principal.getAccount(), anoConsulta));
    }
}
