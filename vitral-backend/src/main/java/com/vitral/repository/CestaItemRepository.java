package com.vitral.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.vitral.entity.CestaItem;

@Repository
public interface CestaItemRepository extends JpaRepository<CestaItem, Long> {

    @Query("SELECT c FROM CestaItem c JOIN FETCH c.produto p JOIN FETCH p.sebo WHERE c.account.id = :accountId")
    List<CestaItem> findByAccountIdComProdutoESebo(@Param("accountId") Long accountId);

    Optional<CestaItem> findByAccountIdAndProdutoId(Long accountId, Long produtoId);

    boolean existsByAccountIdAndProdutoId(Long accountId, Long produtoId);

    void deleteByAccountId(Long accountId);
}
