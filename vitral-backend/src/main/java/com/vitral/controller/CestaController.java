package com.vitral.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.vitral.dto.CestaItemResponse;
import com.vitral.dto.MensagemResponse;
import com.vitral.security.AccountUserDetails;
import com.vitral.service.CestaService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/cesta")
@RequiredArgsConstructor
@Tag(name = "Cesta", description = "Gerenciamento da cesta de compras do usuario")
public class CestaController {

    private final CestaService cestaService;

    @PostMapping("/{produtoId}")
    @PreAuthorize("hasRole('USUARIO')")
    @Operation(summary = "Adiciona um produto a cesta de compras")
    public ResponseEntity<MensagemResponse> adicionarItem(
            @AuthenticationPrincipal AccountUserDetails principal,
            @PathVariable Long produtoId,
            @RequestParam(required = false) Integer quantidade) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(cestaService.adicionarItem(principal.getAccount(), produtoId, quantidade));
    }

    @PutMapping("/{produtoId}")
    @PreAuthorize("hasRole('USUARIO')")
    @Operation(summary = "Atualiza a quantidade de um produto na cesta")
    public ResponseEntity<MensagemResponse> atualizarQuantidade(
            @AuthenticationPrincipal AccountUserDetails principal,
            @PathVariable Long produtoId,
            @RequestParam Integer quantidade) {
        return ResponseEntity.ok(cestaService.atualizarQuantidade(principal.getAccount(), produtoId, quantidade));
    }

    @GetMapping
    @PreAuthorize("hasRole('USUARIO')")
    @Operation(summary = "Lista os itens da cesta de compras do usuario autenticado")
    public ResponseEntity<List<CestaItemResponse>> listarCesta(
            @AuthenticationPrincipal AccountUserDetails principal) {
        return ResponseEntity.ok(cestaService.listarCesta(principal.getAccount()));
    }

    @DeleteMapping("/{produtoId}")
    @PreAuthorize("hasRole('USUARIO')")
    @Operation(summary = "Remove um produto da cesta de compras")
    public ResponseEntity<Void> removerItem(
            @AuthenticationPrincipal AccountUserDetails principal,
            @PathVariable Long produtoId) {
        cestaService.removerItem(principal.getAccount(), produtoId);
        return ResponseEntity.noContent().build();
    }
}
