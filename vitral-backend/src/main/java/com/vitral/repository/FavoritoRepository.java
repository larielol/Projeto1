package com.vitral.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.vitral.entity.Favorito;

@Repository
public interface FavoritoRepository extends JpaRepository<Favorito, Long> {

    List<Favorito> findByAccountId(Long accountId);

    Optional<Favorito> findByAccountIdAndProdutoId(Long accountId, Long produtoId);

    boolean existsByAccountIdAndProdutoId(Long accountId, Long produtoId);

    @Query("select distinct f.produto.categoria.id from Favorito f where f.account.id = :accountId and f.produto.categoria is not null")
    List<Long> findCategoriaIdsByAccountId(@Param("accountId") Long accountId);
}
