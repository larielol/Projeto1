package com.vitral.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vitral.dto.CestaItemResponse;
import com.vitral.dto.MensagemResponse;
import com.vitral.entity.Account;
import com.vitral.entity.CestaItem;
import com.vitral.entity.Produto;
import com.vitral.exception.BusinessException;
import com.vitral.exception.ResourceNotFoundException;
import com.vitral.repository.CestaItemRepository;
import com.vitral.repository.ProdutoRepository;

import lombok.RequiredArgsConstructor;
import com.vitral.enumerations.TipoEventoRecomendacao;

@Service
@RequiredArgsConstructor
public class CestaService {

    private final CestaItemRepository cestaItemRepository;
    private final ProdutoRepository produtoRepository;
    private final PrecoService precoService;
    private final RecomendacaoEventoService recomendacaoEventoService;

    @Transactional
    public MensagemResponse adicionarItem(Account account, Long produtoId) {
        return adicionarItem(account, produtoId, 1);
    }

    @Transactional
    public MensagemResponse adicionarItem(Account account, Long produtoId, Integer quantidade) {
        int quantidadeNormalizada = normalizarQuantidade(quantidade);
        Produto produto = produtoRepository.findByIdAndAtivoTrue(produtoId)
                .orElseThrow(() -> new ResourceNotFoundException("Produto nao encontrado"));
        CestaItem item = cestaItemRepository
                .findByAccountIdAndProdutoId(account.getId(), produtoId)
                .orElseGet(() -> CestaItem.builder()
                        .account(account)
                        .produto(produto)
                        .quantidade(0)
                        .build());

        int novaQuantidade = item.getQuantidade() + quantidadeNormalizada;
        validarEstoque(produto, novaQuantidade);
        item.setQuantidade(novaQuantidade);
        cestaItemRepository.save(item);
        recomendacaoEventoService.registrarProduto(account, TipoEventoRecomendacao.CESTA_ADICIONADO, produto);
        return new MensagemResponse("Produto adicionado a cesta");
    }

    @Transactional
    public MensagemResponse atualizarQuantidade(Account account, Long produtoId, Integer quantidade) {
        int quantidadeNormalizada = normalizarQuantidade(quantidade);
        CestaItem item = cestaItemRepository
                .findByAccountIdAndProdutoId(account.getId(), produtoId)
                .orElseThrow(() -> new ResourceNotFoundException("Item nao encontrado na cesta"));
        validarEstoque(item.getProduto(), quantidadeNormalizada);
        item.setQuantidade(quantidadeNormalizada);
        return new MensagemResponse("Quantidade atualizada");
    }

    @Transactional(readOnly = true)
    public List<CestaItemResponse> listarCesta(Account account) {
        return cestaItemRepository.findByAccountIdComProdutoESebo(account.getId()).stream()
                .map(i -> {
                    BigDecimal preco = precoService.precoEfetivo(i.getProduto());
                    return new CestaItemResponse(
                            i.getId(),
                            i.getProduto().getId(),
                            i.getProduto().getTitulo(),
                            i.getProduto().getAutor(),
                            preco,
                            i.getProduto().getPreco(),
                            i.getQuantidade(),
                            preco.multiply(BigDecimal.valueOf(i.getQuantidade())),
                            i.getProduto().getEstoque(),
                            i.getProduto().getCondicao(),
                            i.getProduto().getFotoUrl());
                })
                .toList();
    }

    @Transactional
    public MensagemResponse removerItem(Account account, Long produtoId) {
        CestaItem item = cestaItemRepository
                .findByAccountIdAndProdutoId(account.getId(), produtoId)
                .orElseThrow(() -> new ResourceNotFoundException("Item nao encontrado na cesta"));
        cestaItemRepository.delete(item);
        recomendacaoEventoService.registrarProduto(account, TipoEventoRecomendacao.CESTA_REMOVIDO, item.getProduto());
        return new MensagemResponse("Produto removido da cesta");
    }

    private int normalizarQuantidade(Integer quantidade) {
        if (quantidade == null) {
            return 1;
        }
        if (quantidade < 1) {
            throw new BusinessException("Quantidade deve ser maior que zero", HttpStatus.BAD_REQUEST);
        }
        return quantidade;
    }

    private void validarEstoque(Produto produto, int quantidade) {
        if (produto.getEstoque() < quantidade) {
            throw new BusinessException("Quantidade solicitada maior que o estoque disponivel", HttpStatus.BAD_REQUEST);
        }
    }
}
