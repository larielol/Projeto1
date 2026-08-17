package com.vitral.controller;

import java.math.BigDecimal;

import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.vitral.dto.PageResponse;
import com.vitral.dto.ProdutoResponse;
import com.vitral.dto.SeboResponse;
import com.vitral.enumerations.CondicaoProduto;
import com.vitral.enumerations.BookGenre;
import com.vitral.service.BuscaService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.vitral.security.AccountUserDetails;

@RestController
@RequestMapping("/api/v1/busca")
@RequiredArgsConstructor
@Tag(name = "Busca", description = "Busca publica de sebos e produtos sem necessidade de login")
public class BuscaController {

    private final BuscaService buscaService;

    @GetMapping("/sebos")
    @Operation(summary = "Busca vitrines virtuais de sebos ativos pelo nome opcional (q); "
            + "ordena por proximidade quando lat/lng sao informados")
    public ResponseEntity<PageResponse<SeboResponse>> buscarSebos(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String cidade,
            @RequestParam(required = false) String uf,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng,
            Pageable pageable) {
        return ResponseEntity.ok(
                PageResponse.from(buscaService.buscarSebos(q, cidade, uf, lat, lng, pageable)));
    }

    @GetMapping("/produtos")
    @Operation(summary = "Busca produtos ativos com filtros opcionais: titulo (q), seboId, condicao, precoMin e precoMax")
    public ResponseEntity<PageResponse<ProdutoResponse>> buscarProdutos(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long seboId,
            @RequestParam(required = false) CondicaoProduto condicao,
            @RequestParam(required = false) BigDecimal precoMin,
            @RequestParam(required = false) BigDecimal precoMax,
            @RequestParam(required = false) Long categoriaId,
            @RequestParam(required = false) BookGenre bookGenre,
            @AuthenticationPrincipal AccountUserDetails principal,
            Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(buscaService.buscarProdutos(
                principal == null ? null : principal.getAccount(), q, seboId, condicao, precoMin, precoMax,
                categoriaId, bookGenre, pageable)));
    }
}
