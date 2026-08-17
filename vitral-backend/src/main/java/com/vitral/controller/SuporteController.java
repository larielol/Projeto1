package com.vitral.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.vitral.dto.SuporteRequest;
import com.vitral.dto.SuporteResponse;
import com.vitral.security.AccountUserDetails;
import com.vitral.service.SuporteService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/suporte")
@RequiredArgsConstructor
@Tag(name = "Suporte", description = "Envio de mensagens de suporte ao Vitral")
public class SuporteController {

    private final SuporteService suporteService;

    @PostMapping
    @Operation(summary = "Envia mensagem de suporte ao Vitral")
    public ResponseEntity<SuporteResponse> enviar(
            @AuthenticationPrincipal AccountUserDetails principal,
            @Valid @RequestBody SuporteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(suporteService.enviar(principal.getAccount(), request));
    }
}
