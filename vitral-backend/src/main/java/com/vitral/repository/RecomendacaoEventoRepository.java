package com.vitral.repository;

import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.vitral.entity.RecomendacaoEvento;
import com.vitral.enumerations.TipoEventoRecomendacao;

public interface RecomendacaoEventoRepository extends JpaRepository<RecomendacaoEvento, Long> {
    List<RecomendacaoEvento> findByAccountIdAndCreatedAtAfterOrderByCreatedAtDesc(
            Long accountId, OffsetDateTime inicio, Pageable pageable);

    boolean existsByAccountIdAndProdutoIdAndTipoAndCreatedAtAfter(
            Long accountId, Long produtoId, TipoEventoRecomendacao tipo, OffsetDateTime inicio);

    long countByAccountIdAndProdutoIdAndTipoAndCreatedAtAfter(
            Long accountId, Long produtoId, TipoEventoRecomendacao tipo, OffsetDateTime inicio);

    boolean existsByAccountIdAndTipoAndTermoPesquisaAndCreatedAtAfter(
            Long accountId, TipoEventoRecomendacao tipo, String termoPesquisa, OffsetDateTime inicio);

    long countByAccountIdAndTipoAndTermoPesquisaAndCreatedAtAfter(
            Long accountId, TipoEventoRecomendacao tipo, String termoPesquisa, OffsetDateTime inicio);

    boolean existsByPedidoIdAndProdutoIdAndTipo(Long pedidoId, Long produtoId, TipoEventoRecomendacao tipo);

    void deleteByAccountId(Long accountId);

    @Modifying
    @Query("update RecomendacaoEvento e set e.termoPesquisa = null where e.termoPesquisa is not null and e.createdAt < :limite")
    int anonimizarTermosAntigos(@Param("limite") OffsetDateTime limite);

    @Modifying
    @Query("delete from RecomendacaoEvento e where e.createdAt < :limite")
    int excluirEventosAntigos(@Param("limite") OffsetDateTime limite);

    @Query("""
            select e.produto.id as produtoId, e.tipo as tipo, count(e) as quantidade
            from RecomendacaoEvento e
            where e.produto is not null and e.createdAt >= :inicio
            group by e.produto.id, e.tipo
            """)
    List<ProdutoEventoContagemProjection> contagemPorProdutoETipo(@Param("inicio") OffsetDateTime inicio);
}
