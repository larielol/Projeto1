package com.vitral.controller;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.vitral.dto.OfertaRequest;
import com.vitral.dto.OfertaResponse;
import com.vitral.dto.PageResponse;
import com.vitral.security.AccountUserDetails;
import com.vitral.service.OfertaService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/ofertas")
@RequiredArgsConstructor
@Tag(name = "Ofertas", description = "Ofertas do catalogo")
public class OfertaController {

    private final OfertaService ofertaService;

    @GetMapping
    @Operation(summary = "Lista ofertas ativas e vigentes")
    public ResponseEntity<PageResponse<OfertaResponse>> listarAtivas(
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(ofertaService.listarAtivas(pageable)));
    }

    @GetMapping("/minhas")
    @PreAuthorize("hasRole('SEBO')")
    @Operation(summary = "Lista as ofertas do sebo autenticado (ativas e inativas)")
    public ResponseEntity<PageResponse<OfertaResponse>> listarMinhas(
            @AuthenticationPrincipal AccountUserDetails principal,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(ofertaService.listarDoSebo(principal.getAccount(), pageable)));
    }

    @PostMapping
    @PreAuthorize("hasRole('SEBO')")
    @Operation(summary = "Cria oferta para produto do sebo autenticado")
    public ResponseEntity<OfertaResponse> criar(
            @AuthenticationPrincipal AccountUserDetails principal,
            @Valid @RequestBody OfertaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ofertaService.criar(principal.getAccount(), request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SEBO')")
    @Operation(summary = "Atualiza oferta do sebo autenticado")
    public ResponseEntity<OfertaResponse> atualizar(
            @AuthenticationPrincipal AccountUserDetails principal,
            @PathVariable Long id,
            @Valid @RequestBody OfertaRequest request) {
        return ResponseEntity.ok(ofertaService.atualizar(principal.getAccount(), id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SEBO')")
    @Operation(summary = "Desativa oferta do sebo autenticado")
    public ResponseEntity<Void> remover(
            @AuthenticationPrincipal AccountUserDetails principal,
            @PathVariable Long id) {
        ofertaService.remover(principal.getAccount(), id);
        return ResponseEntity.noContent().build();
    }
}
