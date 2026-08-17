package com.vitral.service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.vitral.config.RecomendacaoProperties;
import com.vitral.dto.HomeSectionResponse;
import com.vitral.dto.ProdutoResponse;
import com.vitral.entity.Account;
import com.vitral.entity.Produto;
import com.vitral.entity.RecomendacaoEvento;
import com.vitral.enumerations.FaixaPrecoRecomendacao;
import com.vitral.enumerations.TipoEventoRecomendacao;
import com.vitral.exception.BusinessException;
import com.vitral.mapper.ProdutoMapper;
import com.vitral.repository.ProdutoEventoContagemProjection;
import com.vitral.repository.ProdutoRepository;
import com.vitral.repository.RecomendacaoEventoRepository;
import com.vitral.specification.ProdutoSpecification;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RecomendacaoService {
    private final RecomendacaoEventoRepository eventoRepository;
    private final ProdutoRepository produtoRepository;
    private final ProdutoMapper produtoMapper;
    private final PrecoService precoService;
    private final RecenciaRecomendacaoService recenciaService;
    private static final int CATEGORIAS_DE_INTERESSE = 3;

    private final RecomendacaoProperties properties;

    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public HomeSectionResponse paraHome(Account account, Set<Long> excluidos) {
        Page<ProdutoResponse> page = recomendarInterno(account, PageRequest.of(0, properties.tamanhoHome()), excluidos,
                false);
        return new HomeSectionResponse("Recomendados para voce", page.getContent(), page.getTotalElements());
    }

    @Transactional(readOnly = true)
    public Page<ProdutoResponse> recomendar(Account account, Pageable pageable, Set<Long> excluidos) {
        return recomendarInterno(account, pageable, excluidos, true);
    }

    private Page<ProdutoResponse> recomendarInterno(Account account, Pageable pageable, Set<Long> excluidos,
            boolean validarLimitePublico) {
        if (validarLimitePublico && pageable.getPageSize() > properties.tamanhoPaginaMaximo()) {
            throw new BusinessException("size deve ser no maximo " + properties.tamanhoPaginaMaximo(),
                    org.springframework.http.HttpStatus.BAD_REQUEST);
        }
        OffsetDateTime agora = OffsetDateTime.now();
        List<RecomendacaoEvento> eventos = eventoRepository.findByAccountIdAndCreatedAtAfterOrderByCreatedAtDesc(
                account.getId(), agora.minusDays(properties.retencaoEventoDias()),
                PageRequest.of(0, properties.historicoMaximo()));
        Perfil perfil = agregar(eventos, agora);
        Set<Long> bloqueados = new HashSet<>(excluidos == null ? Set.of() : excluidos);
        eventos.stream().filter(e -> e.getTipo() == TipoEventoRecomendacao.COMPRA_CONCLUIDA)
                .filter(e -> e.getCreatedAt().isAfter(agora.minusDays(properties.compraRecenteDias())))
                .filter(e -> e.getProduto() != null).map(e -> e.getProduto().getId()).forEach(bloqueados::add);

        Map<Long, Double> popularidade = popularidade(agora);
        List<ProdutoPontuado> ordenados = candidatos(perfil, bloqueados).stream()
                .map(p -> new ProdutoPontuado(p, pontuarInteresse(p, perfil), pontuarDescoberta(p, popularidade)))
                .sorted(Comparator.comparingDouble(ProdutoPontuado::total).reversed()
                        .thenComparing(pp -> pp.produto().getId()))
                .toList();
        List<Produto> diversos = diversificar(ordenados, Math.min(properties.candidatosMaximo(), ordenados.size()));
        int inicio = (int) Math.min(pageable.getOffset(), diversos.size());
        int fim = Math.min(inicio + pageable.getPageSize(), diversos.size());
        List<Produto> pagina = diversos.subList(inicio, fim);
        List<Long> ids = pagina.stream().map(Produto::getId).toList();
        Map<Long, BigDecimal> promocionais = precoService.precosPromocionaisVigentes(ids);
        List<ProdutoResponse> responses = pagina.stream()
                .map(p -> produtoMapper.toResponse(p, promocionais.get(p.getId()))).toList();
        return new PageImpl<>(responses, pageable, diversos.size());
    }

    private Collection<Produto> candidatos(Perfil perfil, Set<Long> bloqueados) {
        Specification<Produto> base = ProdutoSpecification.disponivel()
                .and(ProdutoSpecification.idsFora(bloqueados));
        PageRequest maisRecentes = PageRequest.of(0, properties.candidatosMaximo(),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        Map<Long, Produto> porId = new LinkedHashMap<>();
        produtoRepository.findAll(base, maisRecentes).forEach(produto -> porId.put(produto.getId(), produto));

        List<Long> categoriasDeInteresse = perfil.categorias.entrySet().stream()
                .filter(entrada -> entrada.getValue() > 0)
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(CATEGORIAS_DE_INTERESSE)
                .map(Map.Entry::getKey)
                .toList();
        if (!categoriasDeInteresse.isEmpty()) {
            produtoRepository.findAll(base.and(ProdutoSpecification.dasCategorias(categoriasDeInteresse)),
                    maisRecentes).forEach(produto -> porId.putIfAbsent(produto.getId(), produto));
        }
        return porId.values();
    }

    private Perfil agregar(List<RecomendacaoEvento> eventos, OffsetDateTime agora) {
        Perfil perfil = new Perfil();
        for (RecomendacaoEvento evento : eventos) {
            double pontos = evento.getTipo().pesoBase() * recenciaService.fator(evento.getCreatedAt(), agora);
            perfil.somar(perfil.tipos, evento.getTipoProduto(), pontos);
            perfil.somar(perfil.categorias, id(evento), pontos);
            perfil.somar(perfil.generos, evento.getGenero(), pontos);
            perfil.somar(perfil.autores, evento.getAutorArtista(), pontos);
            perfil.somar(perfil.faixas, evento.getFaixaPreco(), pontos);
            perfil.somar(perfil.sebos, evento.getSebo() == null ? null : evento.getSebo().getId(), pontos * 0.2);
        }
        return perfil;
    }

    private double pontuarInteresse(Produto produto, Perfil perfil) {
        double pontos = 0;
        pontos += pontuarCategoria(produto, perfil) * 1.5;
        pontos += perfil.valor(perfil.generos, produto.getBookGenre() == null ? null : produto.getBookGenre().name());
        pontos += perfil.valor(perfil.autores, normalizar(produto.getAutor())) * 1.2;
        pontos += perfil.valor(perfil.faixas, FaixaPrecoRecomendacao.de(produto.getPreco()).name()) * 0.5;
        pontos += perfil.valor(perfil.sebos, produto.getSebo().getId());
        return pontos;
    }

    private double pontuarCategoria(Produto produto, Perfil perfil) {
        if (produto.getCategoria() == null) {
            return 0;
        }
        double porId = perfil.valor(perfil.categorias, produto.getCategoria().getId());
        return porId > 0 ? porId : perfil.valor(perfil.tipos, produto.getCategoria().getSlug());
    }

    private double pontuarDescoberta(Produto produto, Map<Long, Double> popularidade) {
        return Math.max(0, popularidade.getOrDefault(produto.getId(), 0.0)) * 0.15
                + (Boolean.TRUE.equals(produto.getLancamento()) ? 1.0 : 0.0);
    }

    private List<Produto> diversificar(List<ProdutoPontuado> ordenados, int limite) {
        int principais = (int) Math.ceil(limite * 0.60);
        int secundarios = (int) Math.ceil(limite * 0.20);
        int descoberta = Math.max(0, limite - principais - secundarios);
        List<ProdutoPontuado> composicao = new ArrayList<>();
        Set<Long> compostos = new HashSet<>();
        adicionarFaixa(composicao, compostos, ordenados.stream()
                .sorted(Comparator.comparingDouble(ProdutoPontuado::interesse).reversed()).toList(), principais);
        adicionarFaixa(composicao, compostos, ordenados, secundarios);
        adicionarFaixa(composicao, compostos, ordenados.stream()
                .sorted(Comparator.comparingDouble(ProdutoPontuado::descoberta).reversed()).toList(), descoberta);
        adicionarFaixa(composicao, compostos, ordenados, limite);
        List<Produto> resultado = new ArrayList<>();
        Map<Long, Integer> porSebo = new HashMap<>();
        Map<Long, Integer> porCategoria = new HashMap<>();
        Set<Long> usados = new LinkedHashSet<>();
        for (int passe = 0; passe < 2 && resultado.size() < limite; passe++) {
            for (ProdutoPontuado pp : composicao) {
                Produto p = pp.produto();
                if (usados.contains(p.getId())) continue;
                Long sebo = p.getSebo().getId();
                Long categoria = p.getCategoria() == null ? -1L : p.getCategoria().getId();
                if (passe == 0 && (porSebo.getOrDefault(sebo, 0) >= 2 || porCategoria.getOrDefault(categoria, 0) >= 3)) continue;
                resultado.add(p);
                usados.add(p.getId());
                porSebo.merge(sebo, 1, Integer::sum);
                porCategoria.merge(categoria, 1, Integer::sum);
                if (resultado.size() == limite) break;
            }
        }
        return resultado;
    }

    private void adicionarFaixa(List<ProdutoPontuado> destino, Set<Long> usados,
            List<ProdutoPontuado> candidatos, int quantidade) {
        int adicionados = 0;
        for (ProdutoPontuado candidato : candidatos) {
            if (adicionados >= quantidade) return;
            if (usados.add(candidato.produto().getId())) {
                destino.add(candidato);
                adicionados++;
            }
        }
    }

    private Map<Long, Double> popularidade(OffsetDateTime agora) {
        Map<Long, Double> resultado = new HashMap<>();
        for (ProdutoEventoContagemProjection item : eventoRepository
                .contagemPorProdutoETipo(agora.minusDays(properties.popularidadeDias()))) {
            resultado.merge(item.getProdutoId(), item.getTipo().pesoBase() * (double) item.getQuantidade(),
                    Double::sum);
        }
        return resultado;
    }

    private Long id(RecomendacaoEvento evento) {
        return evento.getCategoria() == null ? null : evento.getCategoria().getId();
    }

    private String normalizar(String valor) { return valor == null ? null : valor.trim().toLowerCase(); }
    private record ProdutoPontuado(Produto produto, double interesse, double descoberta) {
        double total() { return interesse + descoberta; }
    }

    private static class Perfil {
        final Map<String, Double> tipos = new HashMap<>();
        final Map<Long, Double> categorias = new HashMap<>();
        final Map<String, Double> generos = new HashMap<>();
        final Map<String, Double> autores = new HashMap<>();
        final Map<String, Double> faixas = new HashMap<>();
        final Map<Long, Double> sebos = new HashMap<>();
        <T> void somar(Map<T, Double> mapa, T chave, double valor) {
            if (chave != null) mapa.merge(chave, valor, Double::sum);
        }
        <T> double valor(Map<T, Double> mapa, T chave) {
            return chave == null ? 0 : Math.max(0, mapa.getOrDefault(chave, 0.0));
        }
    }
}
