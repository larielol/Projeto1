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

import com.vitral.dto.PageResponse;
import com.vitral.dto.ProdutoRequest;
import com.vitral.dto.ProdutoResponse;
import com.vitral.dto.SugestaoProdutoResponse;
import com.vitral.dto.VendedorProdutoResponse;
import java.util.List;
import com.vitral.security.AccountUserDetails;
import com.vitral.service.MetadadosProdutoService;
import com.vitral.service.ProdutoService;
import com.vitral.enumerations.BookGenre;
import org.springframework.web.bind.annotation.RequestParam;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/produtos")
@RequiredArgsConstructor
@Tag(name = "Produtos", description = "CRUD do catalogo do sebo e consulta publica")
public class ProdutoController {

    private final ProdutoService produtoService;
    private final MetadadosProdutoService metadadosProdutoService;

    @GetMapping("/sugestoes")
    @PreAuthorize("hasRole('SEBO')")
    @Operation(summary = "Sugere metadados de um produto a partir do titulo, consultando catalogos publicos")
    public ResponseEntity<List<SugestaoProdutoResponse>> sugestoes(@RequestParam String termo) {
        return ResponseEntity.ok(metadadosProdutoService.sugerir(termo));
    }

    @PostMapping
    @PreAuthorize("hasRole('SEBO')")
    @Operation(summary = "Cadastra um produto no catalogo do sebo autenticado")
    public ResponseEntity<ProdutoResponse> criar(@AuthenticationPrincipal AccountUserDetails principal,
            @Valid @RequestBody ProdutoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(produtoService.criar(principal.getAccount(), request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SEBO')")
    @Operation(summary = "Atualiza um produto do sebo autenticado")
    public ResponseEntity<ProdutoResponse> atualizar(@AuthenticationPrincipal AccountUserDetails principal,
            @PathVariable Long id, @Valid @RequestBody ProdutoRequest request) {
        return ResponseEntity.ok(produtoService.atualizar(principal.getAccount(), id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SEBO')")
    @Operation(summary = "Remove logicamente um produto (soft delete) do sebo autenticado")
    public ResponseEntity<Void> remover(@AuthenticationPrincipal AccountUserDetails principal,
            @PathVariable Long id) {
        produtoService.remover(principal.getAccount(), id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consulta publica de um produto ativo pelo id")
    public ResponseEntity<ProdutoResponse> buscarPorId(@PathVariable Long id,
            @AuthenticationPrincipal AccountUserDetails principal) {
        return ResponseEntity.ok(produtoService.buscarPorId(id, principal == null ? null : principal.getAccount()));
    }

    ResponseEntity<ProdutoResponse> buscarPorId(Long id) {
        return ResponseEntity.ok(produtoService.buscarPorId(id));
    }

    @GetMapping("/{id}/vendedores")
    @Operation(summary = "Lista vendedores verificados com o mesmo produto ativo, do menor preco para o maior")
    public ResponseEntity<List<VendedorProdutoResponse>> listarVendedores(@PathVariable Long id) {
        return ResponseEntity.ok(produtoService.listarVendedores(id));
    }

    @GetMapping("/sebo/{seboId}")
    @Operation(summary = "Lista paginada do catalogo ativo de um sebo")
    public ResponseEntity<PageResponse<ProdutoResponse>> listarPorSebo(@PathVariable Long seboId, Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(produtoService.listarPorSebo(seboId, pageable)));
    }

    @GetMapping("/categoria/{categoriaId}")
    @Operation(summary = "Lista produtos ativos de uma categoria")
    public ResponseEntity<PageResponse<ProdutoResponse>> listarPorCategoria(@PathVariable Long categoriaId,
            @RequestParam(required = false) BookGenre bookGenre, Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(produtoService.listarPorCategoria(categoriaId, bookGenre, pageable)));
    }

    ResponseEntity<PageResponse<ProdutoResponse>> listarPorCategoria(Long categoriaId, Pageable pageable) {
        return listarPorCategoria(categoriaId, null, pageable);
    }

    @GetMapping("/lancamentos")
    @Operation(summary = "Lista produtos ativos mais recentes")
    public ResponseEntity<PageResponse<ProdutoResponse>> listarLancamentos(
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(produtoService.listarLancamentos(pageable)));
    }

    @GetMapping("/classicos")
    @Operation(summary = "Lista produtos ativos mais antigos (classicos do catalogo)")
    public ResponseEntity<PageResponse<ProdutoResponse>> listarClassicos(
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(produtoService.listarClassicos(pageable)));
    }

}
