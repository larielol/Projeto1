package com.vitral.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vitral.dto.ProdutoRequest;
import com.vitral.dto.ProdutoResponse;
import com.vitral.dto.VendedorProdutoResponse;
import com.vitral.enumerations.StatusVerificacaoSebo;
import java.util.Comparator;
import org.springframework.data.jpa.domain.Specification;
import com.vitral.specification.ProdutoSpecification;
import com.vitral.enumerations.TipoMovimentacaoEstoque;
import com.vitral.enumerations.BookGenre;
import com.vitral.entity.Account;
import com.vitral.entity.Categoria;
import com.vitral.entity.Produto;
import com.vitral.entity.Sebo;
import com.vitral.exception.BusinessException;
import com.vitral.exception.ResourceNotFoundException;
import com.vitral.mapper.ProdutoMapper;
import com.vitral.repository.ProdutoRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProdutoService {

    private final ProdutoRepository produtoRepository;
    private final ProdutoMapper produtoMapper;
    private final SeboService seboService;
    private final CategoriaService categoriaService;
    private final PrecoService precoService;
    private final MovimentacaoEstoqueService movimentacaoEstoqueService;
    private final RecomendacaoEventoService recomendacaoEventoService;

    @Transactional
    public ProdutoResponse criar(Account account, ProdutoRequest request) {
        Sebo sebo = seboService.buscarEntidadePorAccount(account.getId());
        Categoria categoria = obterCategoria(request.categoriaId());
        validarGenero(categoria, request.bookGenre());
        Produto produto = Produto.builder()
                .sebo(sebo)
                .categoria(categoria)
                .bookGenre(request.bookGenre())
                .titulo(request.titulo())
                .autor(request.autor())
                .descricao(request.descricao())
                .ano(request.ano())
                .preco(request.preco())
                .estoque(normalizarEstoque(request.estoque()))
                .condicao(request.condicao())
                .fotoUrl(request.fotoUrl())
                .ativo(normalizarEstoque(request.estoque()) > 0)
                .classico(Boolean.TRUE.equals(request.classico()))
                .lancamento(Boolean.TRUE.equals(request.lancamento()))
                .build();
        Produto salvo = produtoRepository.save(produto);
        if (salvo.getEstoque() > 0) {
            movimentacaoEstoqueService.registrarAlteracao(salvo, account, TipoMovimentacaoEstoque.ENTRADA,
                    salvo.getEstoque(), 0, salvo.getEstoque(), salvo.getPreco(), "Estoque inicial");
        }
        seboService.registrarAtividade(sebo);
        return toResponse(salvo);
    }

    @Transactional
    public ProdutoResponse atualizar(Account account, Long produtoId, ProdutoRequest request) {
        Produto produto = obterProdutoDoDono(account, produtoId);
        Categoria categoria = obterCategoria(request.categoriaId());
        validarGenero(categoria, request.bookGenre());
        produto.setCategoria(categoria);
        produto.setBookGenre("livros".equals(categoria.getSlug()) ? request.bookGenre() : null);
        produto.setTitulo(request.titulo());
        produto.setAutor(request.autor());
        produto.setDescricao(request.descricao());
        produto.setAno(request.ano());
        produto.setPreco(request.preco());
        int estoqueAntes = produto.getEstoque();
        int estoqueDepois = normalizarEstoque(request.estoque());
        produto.setEstoque(estoqueDepois);
        produto.setCondicao(request.condicao());
        produto.setFotoUrl(request.fotoUrl());
        produto.setAtivo(produto.getEstoque() > 0);
        produto.setClassico(Boolean.TRUE.equals(request.classico()));
        produto.setLancamento(Boolean.TRUE.equals(request.lancamento()));
        if (estoqueAntes != estoqueDepois) {
            movimentacaoEstoqueService.registrarAlteracao(produto, account, TipoMovimentacaoEstoque.AJUSTE,
                    Math.abs(estoqueDepois - estoqueAntes), estoqueAntes, estoqueDepois, produto.getPreco(),
                    "Ajuste no cadastro do produto");
        }
        seboService.registrarAtividade(produto.getSebo());
        return toResponse(produto);
    }

    @Transactional
    public void remover(Account account, Long produtoId) {
        Produto produto = obterProdutoDoDono(account, produtoId);
        produto.setAtivo(Boolean.FALSE);
        seboService.registrarAtividade(produto.getSebo());
    }

    @Transactional(readOnly = true)
    public ProdutoResponse buscarPorId(Long id) {
        return buscarPorId(id, null);
    }

    @Transactional(readOnly = true)
    public ProdutoResponse buscarPorId(Long id, Account visitante) {
        Produto produto = produtoRepository.findByIdAndAtivoTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Produto nao encontrado"));
        validarSeboVerificado(produto);
        recomendacaoEventoService.registrarProduto(visitante,
                com.vitral.enumerations.TipoEventoRecomendacao.VISUALIZACAO, produto);
        return toResponse(produto);
    }

    @Transactional(readOnly = true)
    public List<VendedorProdutoResponse> listarVendedores(Long produtoId) {
        Produto referencia = produtoRepository.findByIdAndAtivoTrue(produtoId)
                .orElseThrow(() -> new ResourceNotFoundException("Produto nao encontrado"));
        return List.of(referencia).stream()
                .map(produto -> {
                    BigDecimal promocional = precoService.precoPromocionalVigente(produto).orElse(null);
                    BigDecimal efetivo = promocional == null ? produto.getPreco() : promocional;
                    return new VendedorProdutoResponse(produto.getId(), produto.getSebo().getId(),
                            produto.getSebo().getAccount().getName(), produto.getPreco(), promocional,
                            efetivo, produto.getEstoque(), produto.getCondicao());
                })
                .sorted(Comparator.comparing(VendedorProdutoResponse::precoEfetivo))
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<ProdutoResponse> listarPorSebo(Long seboId, Pageable pageable) {
        return toResponsePage(produtoRepository.findAll(
                ProdutoSpecification.ativo().and(ProdutoSpecification.doSebo(seboId)), pageable));
    }

    @Transactional(readOnly = true)
    public Page<ProdutoResponse> listarPorCategoria(Long categoriaId, BookGenre bookGenre, Pageable pageable) {
        Categoria categoria = categoriaService.buscarPermitida(categoriaId);
        validarFiltroGenero(categoria, bookGenre);
        Specification<Produto> spec = ProdutoSpecification.ativo()
                .and(ProdutoSpecification.daCategoria(categoriaId));
        if (bookGenre != null) spec = spec.and(ProdutoSpecification.comBookGenre(bookGenre));
        return toResponsePage(produtoRepository.findAll(spec, pageable));
    }

    public Page<ProdutoResponse> listarPorCategoria(Long categoriaId, Pageable pageable) {
        return listarPorCategoria(categoriaId, null, pageable);
    }

    @Transactional(readOnly = true)
    public Page<ProdutoResponse> listarLancamentos(Pageable pageable) {
        return toResponsePage(produtoRepository.findAll(ProdutoSpecification.disponivel()
                .and(ProdutoSpecification.naoClassico())
                .and(ProdutoSpecification.lancamentoOuRecente(java.time.OffsetDateTime.now().minusDays(30))), pageable));
    }

    @Transactional(readOnly = true)
    public Page<ProdutoResponse> listarClassicos(Pageable pageable) {
        return toResponsePage(produtoRepository.findAll(ProdutoSpecification.disponivel()
                .and(ProdutoSpecification.classico()), pageable));
    }

    private ProdutoResponse toResponse(Produto produto) {
        return produtoMapper.toResponse(produto, precoService.precoPromocionalVigente(produto).orElse(null));
    }

    private Page<ProdutoResponse> toResponsePage(Page<Produto> produtos) {
        List<Long> ids = produtos.getContent().stream().map(Produto::getId).toList();
        Map<Long, BigDecimal> promocionais = precoService.precosPromocionaisVigentes(ids);
        return produtos.map(p -> produtoMapper.toResponse(p, promocionais.get(p.getId())));
    }

    private Produto obterProdutoDoDono(Account account, Long produtoId) {
        Produto produto = produtoRepository.findById(produtoId)
                .orElseThrow(() -> new ResourceNotFoundException("Produto nao encontrado"));
        if (!produto.getSebo().getAccount().getId().equals(account.getId())) {
            throw new BusinessException("Produto nao pertence ao sebo da conta autenticada", HttpStatus.FORBIDDEN);
        }
        return produto;
    }

    private Categoria obterCategoria(Long categoriaId) {
        if (categoriaId == null) {
            throw new BusinessException("Categoria e obrigatoria", HttpStatus.BAD_REQUEST);
        }
        return categoriaService.buscarPermitida(categoriaId);
    }

    private void validarGenero(Categoria categoria, BookGenre bookGenre) {
        if (bookGenre != null && !"livros".equals(categoria.getSlug())) {
            throw new BusinessException("bookGenre somente pode ser informado para a categoria Livros", HttpStatus.BAD_REQUEST);
        }
    }

    private void validarFiltroGenero(Categoria categoria, BookGenre bookGenre) {
        validarGenero(categoria, bookGenre);
    }

    private int normalizarEstoque(Integer estoque) {
        return estoque == null ? 1 : estoque;
    }

    private void validarSeboVerificado(Produto produto) {
        if (produto.getSebo().getStatusVerificacao() != StatusVerificacaoSebo.VERIFICADO
                || !Boolean.TRUE.equals(produto.getSebo().getAccount().getAtivo())) {
            throw new ResourceNotFoundException("Produto nao encontrado");
        }
    }
}
