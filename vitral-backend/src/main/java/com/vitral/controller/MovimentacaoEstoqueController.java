package com.vitral.controller;

import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.vitral.dto.MovimentacaoEstoqueResponse;
import com.vitral.dto.PageResponse;
import com.vitral.security.AccountUserDetails;
import com.vitral.service.MovimentacaoEstoqueService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/movimentacoes-estoque")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SEBO')")
public class MovimentacaoEstoqueController {

    private final MovimentacaoEstoqueService service;

    @GetMapping
    public ResponseEntity<PageResponse<MovimentacaoEstoqueResponse>> listar(
            @AuthenticationPrincipal AccountUserDetails principal, Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(service.listar(principal.getAccount(), pageable)));
    }
}
