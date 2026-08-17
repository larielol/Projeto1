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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.vitral.dto.FavoritoResponse;
import com.vitral.dto.MensagemResponse;
import com.vitral.security.AccountUserDetails;
import com.vitral.service.FavoritoService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/favoritos")
@RequiredArgsConstructor
@Tag(name = "Favoritos", description = "Gerenciamento de produtos favoritados pelo usuario")
public class FavoritoController {

    private final FavoritoService favoritoService;

    @PostMapping("/{produtoId}")
    @PreAuthorize("hasRole('USUARIO')")
    @Operation(summary = "Favorita um produto")
    public ResponseEntity<MensagemResponse> favoritar(
            @AuthenticationPrincipal AccountUserDetails principal,
            @PathVariable Long produtoId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(favoritoService.favoritar(principal.getAccount(), produtoId));
    }

    @GetMapping
    @PreAuthorize("hasRole('USUARIO')")
    @Operation(summary = "Lista os produtos favoritados pelo usuario autenticado")
    public ResponseEntity<List<FavoritoResponse>> listarFavoritos(
            @AuthenticationPrincipal AccountUserDetails principal) {
        return ResponseEntity.ok(favoritoService.listarFavoritos(principal.getAccount()));
    }

    @DeleteMapping("/{produtoId}")
    @PreAuthorize("hasRole('USUARIO')")
    @Operation(summary = "Remove um produto dos favoritos")
    public ResponseEntity<Void> removerFavorito(
            @AuthenticationPrincipal AccountUserDetails principal,
            @PathVariable Long produtoId) {
        favoritoService.removerFavorito(principal.getAccount(), produtoId);
        return ResponseEntity.noContent().build();
    }
}
