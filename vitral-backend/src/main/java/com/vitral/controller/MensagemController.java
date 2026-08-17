package com.vitral.controller;

import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.vitral.dto.MensagemConversaResponse;
import com.vitral.dto.MensagemRequest;
import com.vitral.dto.PageResponse;
import com.vitral.security.AccountUserDetails;
import com.vitral.service.MensagemService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/mensagens")
@RequiredArgsConstructor
@Tag(name = "Mensagens", description = "Conversas entre usuarios e sebos")
public class MensagemController {

    private final MensagemService mensagemService;

    @PostMapping
    @Operation(summary = "Envia mensagem para outra conta")
    public ResponseEntity<MensagemConversaResponse> enviar(
            @AuthenticationPrincipal AccountUserDetails principal,
            @Valid @RequestBody MensagemRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(mensagemService.enviar(principal.getAccount(), request));
    }

    @GetMapping
    @Operation(summary = "Lista mensagens da conta autenticada")
    public ResponseEntity<PageResponse<MensagemConversaResponse>> listarConversas(
            @AuthenticationPrincipal AccountUserDetails principal,
            Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(mensagemService.listarConversas(principal.getAccount(), pageable)));
    }

    @GetMapping("/conversa/{accountId}")
    @Operation(summary = "Lista conversa com uma conta especifica")
    public ResponseEntity<PageResponse<MensagemConversaResponse>> listarConversaCom(
            @AuthenticationPrincipal AccountUserDetails principal,
            @PathVariable Long accountId,
            Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(mensagemService.listarConversaCom(principal.getAccount(), accountId, pageable)));
    }
}
