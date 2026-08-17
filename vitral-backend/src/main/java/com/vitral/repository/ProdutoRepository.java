package com.vitral.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.repository.query.Param;
import java.util.List;

import com.vitral.entity.Produto;

public interface ProdutoRepository extends JpaRepository<Produto, Long>, JpaSpecificationExecutor<Produto> {

    @Override
    @EntityGraph(attributePaths = {"categoria", "sebo", "sebo.account"})
    Page<Produto> findAll(Specification<Produto> spec, Pageable pageable);

    Optional<Produto> findByIdAndAtivoTrue(Long id);

    Page<Produto> findBySeboIdAndAtivoTrue(Long seboId, Pageable pageable);

    Page<Produto> findByCategoriaIdAndAtivoTrue(Long categoriaId, Pageable pageable);

    Page<Produto> findByAtivoTrue(Pageable pageable);

    Page<Produto> findByAtivoTrueOrderByCreatedAtAsc(Pageable pageable);

    Page<Produto> findByAtivoTrueAndTituloContainingIgnoreCase(String termo, Pageable pageable);

    @Modifying
    @Query("update Produto p set p.ativo = false where p.sebo.id = :seboId")
    int desativarCatalogoDoSebo(@Param("seboId") Long seboId);

}
