package com.vitral.service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vitral.dto.HomeCategoriaResponse;
import com.vitral.dto.HomeResponse;
import com.vitral.dto.HomeSectionResponse;
import com.vitral.dto.ProdutoResponse;
import com.vitral.entity.Account;
import com.vitral.entity.Categoria;
import com.vitral.entity.Produto;
import com.vitral.enumerations.AccountType;
import com.vitral.mapper.ProdutoMapper;
import com.vitral.repository.CategoriaRepository;
import com.vitral.repository.ProdutoRepository;
import com.vitral.specification.ProdutoSpecification;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.vitral.config.RecomendacaoProperties;

@Service
@RequiredArgsConstructor
public class HomeService {
    private static final Logger log = LoggerFactory.getLogger(HomeService.class);

    static final int DIAS_PRODUTO_RECENTE = 30;

    private final ProdutoRepository produtoRepository;
    private final CategoriaRepository categoriaRepository;
    private final ProdutoMapper produtoMapper;
    private final PrecoService precoService;
    private final SeboService seboService;
    private final RecomendacaoService recomendacaoService;
    private final RecomendacaoProperties recomendacaoProperties;

    @Transactional(readOnly = true)
    public HomeResponse carregar(Account account) {
        Long seboId = resolverEscopoSebo(account);
        Specification<Produto> base = ProdutoSpecification.disponivel();
        if (seboId != null) base = base.and(ProdutoSpecification.doSebo(seboId));

        Page<Produto> classicos = produtoRepository.findAll(base.and(ProdutoSpecification.classico()),
                Pageable.unpaged(Sort.by(Sort.Direction.DESC, "updatedAt")));
        Set<Long> destacados = ids(classicos);

        Specification<Produto> specLancamentos = base.and(ProdutoSpecification.naoClassico())
                .and(ProdutoSpecification.lancamentoOuRecente(OffsetDateTime.now().minusDays(DIAS_PRODUTO_RECENTE)));
        Page<Produto> lancamentos = produtoRepository.findAll(specLancamentos,
                Pageable.unpaged(
                        Sort.by(Sort.Order.desc("lancamento"), Sort.Order.desc("createdAt"))));
        destacados.addAll(ids(lancamentos));

        HomeSectionResponse recomendados = montarRecomendados(account, base, destacados);
        if (recomendados != null) {
            recomendados.produtos().stream().map(ProdutoResponse::id).forEach(destacados::add);
        }
        List<HomeCategoriaResponse> categorias = montarCategorias(base, seboId, destacados);

        return new HomeResponse(
                secao("Lancamentos", lancamentos),
                secao("Classicos", classicos),
                recomendados,
                categorias);
    }

    private Long resolverEscopoSebo(Account account) {
        if (account == null || account.getType() != AccountType.SEBO) return null;
        return seboService.buscarEntidadePorAccount(account.getId()).getId();
    }

    private HomeSectionResponse montarRecomendados(Account account, Specification<Produto> base, Set<Long> destacados) {
        if (account == null || account.getType() != AccountType.USUARIO) return null;
        try {
            return recomendacaoService.paraHome(account, destacados);
        } catch (RuntimeException exception) {
            log.warn("Falha ao personalizar home; usando recomendacoes genericas");
            Page<Produto> genericos = produtoRepository.findAll(base.and(ProdutoSpecification.idsFora(destacados)),
                    org.springframework.data.domain.PageRequest.of(0, recomendacaoProperties.candidatosMaximo(),
                            Sort.by(Sort.Direction.DESC, "createdAt")));
            return secao("Recomendados para voce", genericos);
        }
    }

    private List<HomeCategoriaResponse> montarCategorias(Specification<Produto> base, Long seboId,
            Set<Long> destacados) {
        List<HomeCategoriaResponse> resultado = new ArrayList<>();
        for (Categoria categoria : categoriaRepository.findComProdutosDisponiveis(seboId,
                CategoriaService.SLUGS_PERMITIDOS)) {
            Specification<Produto> categoriaBase = base.and(ProdutoSpecification.daCategoria(categoria.getId()));
            long total = produtoRepository.count(categoriaBase);
            Page<Produto> page = produtoRepository.findAll(
                    categoriaBase.and(ProdutoSpecification.idsFora(destacados)),
                    Pageable.unpaged(Sort.by(Sort.Direction.DESC, "createdAt")));
            if (page.isEmpty() && total > 0) {
                page = produtoRepository.findAll(categoriaBase,
                        Pageable.unpaged(Sort.by(Sort.Direction.DESC, "createdAt")));
            }
            resultado.add(new HomeCategoriaResponse(categoria.getId(), categoria.getNome(), categoria.getSlug(),
                    mapear(page.getContent()), total));
        }
        return resultado;
    }

    private HomeSectionResponse secao(String titulo, Page<Produto> page) {
        return new HomeSectionResponse(titulo, mapear(page.getContent()), page.getTotalElements());
    }

    private List<ProdutoResponse> mapear(List<Produto> produtos) {
        List<Long> ids = produtos.stream().map(Produto::getId).toList();
        Map<Long, BigDecimal> promocionais = precoService.precosPromocionaisVigentes(ids);
        return produtos.stream().map(p -> produtoMapper.toResponse(p, promocionais.get(p.getId()))).toList();
    }

    private Set<Long> ids(Page<Produto> produtos) {
        return produtos.stream().map(Produto::getId).collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
    }
}
