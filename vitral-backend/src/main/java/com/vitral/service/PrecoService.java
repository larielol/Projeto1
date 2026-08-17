package com.vitral.service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vitral.entity.Oferta;
import com.vitral.entity.Produto;
import com.vitral.repository.OfertaRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PrecoService {

    private final OfertaRepository ofertaRepository;

    @Transactional(readOnly = true)
    public Optional<BigDecimal> precoPromocionalVigente(Produto produto) {
        return ofertaRepository.findVigenteByProdutoId(produto.getId(), OffsetDateTime.now())
                .map(Oferta::getPrecoPromocional);
    }

    @Transactional(readOnly = true)
    public BigDecimal precoEfetivo(Produto produto) {
        return precoPromocionalVigente(produto).orElse(produto.getPreco());
    }

    @Transactional(readOnly = true)
    public Map<Long, BigDecimal> precosPromocionaisVigentes(Collection<Long> produtoIds) {
        if (produtoIds.isEmpty()) {
            return Map.of();
        }
        return ofertaRepository.findVigentesByProdutoIds(produtoIds, OffsetDateTime.now()).stream()
                .collect(Collectors.toMap(o -> o.getProduto().getId(), Oferta::getPrecoPromocional, (a, b) -> a));
    }
}
