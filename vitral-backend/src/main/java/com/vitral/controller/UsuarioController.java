package com.vitral.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.vitral.dto.AccountResponse;
import com.vitral.dto.AtualizarPerfilRequest;
import com.vitral.dto.MensagemResponse;
import com.vitral.security.AccountUserDetails;
import com.vitral.service.UsuarioService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/usuarios")
@RequiredArgsConstructor
@Tag(name = "Usuarios", description = "Gerenciamento do perfil do usuario autenticado")
public class UsuarioController {

    private final UsuarioService usuarioService;

    @PutMapping("/me")
    @Operation(summary = "Atualiza o nome do perfil do usuario autenticado")
    public ResponseEntity<AccountResponse> atualizarPerfil(
            @AuthenticationPrincipal AccountUserDetails principal,
            @Valid @RequestBody AtualizarPerfilRequest request) {
        return ResponseEntity.ok(usuarioService.atualizarPerfil(principal.getAccount(), request));
    }

    @DeleteMapping("/me")
    @Operation(summary = "Exclui permanentemente a conta do usuario autenticado")
    public ResponseEntity<MensagemResponse> excluirConta(
            @AuthenticationPrincipal AccountUserDetails principal) {
        return ResponseEntity.ok(usuarioService.excluirConta(principal.getAccount()));
    }
}
