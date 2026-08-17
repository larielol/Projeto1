package com.vitral.service;

import java.time.OffsetDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vitral.dto.OfertaRequest;
import com.vitral.dto.OfertaResponse;
import com.vitral.entity.Account;
import com.vitral.entity.Oferta;
import com.vitral.entity.Produto;
import com.vitral.exception.BusinessException;
import com.vitral.exception.ConflictException;
import com.vitral.exception.ResourceNotFoundException;
import com.vitral.repository.OfertaRepository;
import com.vitral.repository.ProdutoRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OfertaService {

    private final OfertaRepository ofertaRepository;
    private final ProdutoRepository produtoRepository;
    private final SeboService seboService;

    @Transactional
    public OfertaResponse criar(Account account, OfertaRequest request) {
        Produto produto = obterProdutoDoSebo(account, request.produtoId());
        if (ofertaRepository.findByProdutoIdAndAtivaTrue(produto.getId()).isPresent()) {
            throw new ConflictException("Produto ja possui oferta ativa");
        }
        validarPeriodo(request.inicioEm(), request.fimEm());
        Oferta oferta = Oferta.builder()
                .produto(produto)
                .precoPromocional(request.precoPromocional())
                .descricao(request.descricao())
                .inicioEm(request.inicioEm())
                .fimEm(request.fimEm())
                .ativa(request.ativa() == null || request.ativa())
                .build();
        Oferta salva = ofertaRepository.save(oferta);
        seboService.registrarAtividade(produto.getSebo());
        return toResponse(salva);
    }

    @Transactional
    public OfertaResponse atualizar(Account account, Long id, OfertaRequest request) {
        Oferta oferta = ofertaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Oferta nao encontrada"));
        Produto produto = obterProdutoDoSebo(account, request.produtoId());
        validarPeriodo(request.inicioEm(), request.fimEm());
        oferta.setProduto(produto);
        oferta.setPrecoPromocional(request.precoPromocional());
        oferta.setDescricao(request.descricao());
        oferta.setInicioEm(request.inicioEm());
        oferta.setFimEm(request.fimEm());
        oferta.setAtiva(request.ativa() == null || request.ativa());
        seboService.registrarAtividade(produto.getSebo());
        return toResponse(oferta);
    }

    @Transactional
    public void remover(Account account, Long id) {
        Oferta oferta = ofertaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Oferta nao encontrada"));
        if (!oferta.getProduto().getSebo().getAccount().getId().equals(account.getId())) {
            throw new BusinessException("Oferta nao pertence ao sebo da conta autenticada", HttpStatus.FORBIDDEN);
        }
        oferta.setAtiva(Boolean.FALSE);
        seboService.registrarAtividade(oferta.getProduto().getSebo());
    }

    @Transactional(readOnly = true)
    public Page<OfertaResponse> listarAtivas(Pageable pageable) {
        return ofertaRepository.findOfertasAtivas(OffsetDateTime.now(), pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<OfertaResponse> listarDoSebo(Account account, Pageable pageable) {
        return ofertaRepository.findDoSebo(account.getId(), pageable).map(this::toResponse);
    }

    private Produto obterProdutoDoSebo(Account account, Long produtoId) {
        Produto produto = produtoRepository.findById(produtoId)
                .orElseThrow(() -> new ResourceNotFoundException("Produto nao encontrado"));
        if (!produto.getSebo().getAccount().getId().equals(account.getId())) {
            throw new BusinessException("Produto nao pertence ao sebo da conta autenticada", HttpStatus.FORBIDDEN);
        }
        if (!Boolean.TRUE.equals(produto.getAtivo())) {
            throw new BusinessException("Produto nao esta disponivel para oferta", HttpStatus.BAD_REQUEST);
        }
        return produto;
    }

    private void validarPeriodo(OffsetDateTime inicioEm, OffsetDateTime fimEm) {
        if (inicioEm != null && fimEm != null && fimEm.isBefore(inicioEm)) {
            throw new BusinessException("Data final da oferta deve ser posterior ao inicio", HttpStatus.BAD_REQUEST);
        }
    }

    private OfertaResponse toResponse(Oferta oferta) {
        Produto produto = oferta.getProduto();
        return new OfertaResponse(
                oferta.getId(),
                produto.getId(),
                produto.getSebo().getId(),
                produto.getTitulo(),
                produto.getPreco(),
                oferta.getPrecoPromocional(),
                oferta.getDescricao(),
                oferta.getInicioEm(),
                oferta.getFimEm(),
                oferta.getAtiva());
    }
}
