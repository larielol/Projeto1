package com.vitral.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.vitral.dto.HomeResponse;
import com.vitral.security.AccountUserDetails;
import com.vitral.service.HomeService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/home")
@RequiredArgsConstructor
@Tag(name = "Home", description = "Secoes dinamicas da pagina inicial")
public class HomeController {

    private final HomeService homeService;

    @GetMapping
    @Operation(summary = "Retorna lancamentos, classicos, recomendacoes e categorias disponiveis")
    public ResponseEntity<HomeResponse> carregar(@AuthenticationPrincipal AccountUserDetails principal) {
        return ResponseEntity.ok(homeService.carregar(principal == null ? null : principal.getAccount()));
    }
}
