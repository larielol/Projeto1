package com.vitral.controller;

import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.vitral.dto.MensagemResponse;
import com.vitral.dto.PageResponse;
import com.vitral.dto.ProdutoResponse;
import com.vitral.security.AccountUserDetails;
import com.vitral.service.RecomendacaoHistoricoService;
import com.vitral.service.RecomendacaoService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/recomendacoes")
@RequiredArgsConstructor
@PreAuthorize("hasRole('USUARIO')")
@Tag(name = "Recomendacoes", description = "Personalizacao privada do usuario autenticado")
public class RecomendacaoController {
    private final RecomendacaoService recomendacaoService;
    private final RecomendacaoHistoricoService historicoService;

    @GetMapping
    @Operation(summary = "Lista recomendacoes personalizadas e paginadas")
    public ResponseEntity<PageResponse<ProdutoResponse>> listar(
            @AuthenticationPrincipal AccountUserDetails principal, Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(
                recomendacaoService.recomendar(principal.getAccount(), pageable, java.util.Set.of())));
    }

    @DeleteMapping("/historico")
    @Operation(summary = "Apaga os dados usados para personalizar recomendacoes")
    public ResponseEntity<MensagemResponse> limparHistorico(
            @AuthenticationPrincipal AccountUserDetails principal) {
        return ResponseEntity.ok(historicoService.limpar(principal.getAccount()));
    }
}
