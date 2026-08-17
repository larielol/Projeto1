package com.vitral.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.Collection;
import org.springframework.data.jpa.repository.Query;

import com.vitral.entity.Categoria;
import java.util.List;
import org.springframework.data.repository.query.Param;

public interface CategoriaRepository extends JpaRepository<Categoria, Long> {

    Optional<Categoria> findBySlug(String slug);

    boolean existsBySlug(String slug);

    @Query("""
            select c from Categoria c where c.slug in :slugs
            order by case c.slug when 'livros' then 1 when 'cds' then 2 when 'vinis' then 3 when 'hqs-mangas' then 4 else 5 end
            """)
    Page<Categoria> findPermitidas(Collection<String> slugs, Pageable pageable);

    @Query("""
            select distinct c from Produto p join p.categoria c
            where p.ativo = true and p.estoque > 0
              and c.slug in :slugs
              and p.sebo.account.ativo = true
              and p.sebo.statusVerificacao = com.vitral.enumerations.StatusVerificacaoSebo.VERIFICADO
              and (:seboId is null or p.sebo.id = :seboId)
            order by c.nome
            """)
    List<Categoria> findComProdutosDisponiveis(@Param("seboId") Long seboId,
            @Param("slugs") Collection<String> slugs);
}
