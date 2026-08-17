package com.vitral.repository;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.vitral.entity.Oferta;

public interface OfertaRepository extends JpaRepository<Oferta, Long> {

    @Query(value = """
            select o from Oferta o
            join fetch o.produto p
            join fetch p.sebo s
            where o.ativa = true
              and p.ativo = true
              and (o.inicioEm is null or o.inicioEm <= :agora)
              and (o.fimEm is null or o.fimEm >= :agora)
            """,
            countQuery = """
            select count(o) from Oferta o
            join o.produto p
            where o.ativa = true
              and p.ativo = true
              and (o.inicioEm is null or o.inicioEm <= :agora)
              and (o.fimEm is null or o.fimEm >= :agora)
            """)
    Page<Oferta> findOfertasAtivas(@Param("agora") OffsetDateTime agora, Pageable pageable);

    Optional<Oferta> findByProdutoIdAndAtivaTrue(Long produtoId);

    @Query("""
            select o from Oferta o
            where o.produto.id = :produtoId
              and o.ativa = true
              and (o.inicioEm is null or o.inicioEm <= :agora)
              and (o.fimEm is null or o.fimEm >= :agora)
            """)
    Optional<Oferta> findVigenteByProdutoId(@Param("produtoId") Long produtoId, @Param("agora") OffsetDateTime agora);

    @Query(value = """
            select o from Oferta o
            join fetch o.produto p
            join fetch p.sebo s
            where s.account.id = :accountId
            order by o.createdAt desc
            """,
            countQuery = """
            select count(o) from Oferta o
            where o.produto.sebo.account.id = :accountId
            """)
    Page<Oferta> findDoSebo(@Param("accountId") Long accountId, Pageable pageable);

    @Query("""
            select o from Oferta o
            join fetch o.produto p
            where p.id in :produtoIds
              and o.ativa = true
              and (o.inicioEm is null or o.inicioEm <= :agora)
              and (o.fimEm is null or o.fimEm >= :agora)
            """)
    List<Oferta> findVigentesByProdutoIds(@Param("produtoIds") Collection<Long> produtoIds,
            @Param("agora") OffsetDateTime agora);
}
