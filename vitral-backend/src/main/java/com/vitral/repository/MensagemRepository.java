package com.vitral.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.vitral.entity.Mensagem;

public interface MensagemRepository extends JpaRepository<Mensagem, Long> {

    @Query(value = """
            select m from Mensagem m
            join fetch m.remetente r
            join fetch m.destinatario d
            where m.remetente.id = :accountId or m.destinatario.id = :accountId
            order by m.createdAt desc
            """,
            countQuery = """
            select count(m) from Mensagem m
            where m.remetente.id = :accountId or m.destinatario.id = :accountId
            """)
    Page<Mensagem> findConversasDaConta(@Param("accountId") Long accountId, Pageable pageable);

    @Query(value = """
            select m from Mensagem m
            join fetch m.remetente r
            join fetch m.destinatario d
            where (m.remetente.id = :accountId and m.destinatario.id = :outroAccountId)
               or (m.remetente.id = :outroAccountId and m.destinatario.id = :accountId)
            order by m.createdAt asc, m.id asc
            """,
            countQuery = """
            select count(m) from Mensagem m
            where (m.remetente.id = :accountId and m.destinatario.id = :outroAccountId)
               or (m.remetente.id = :outroAccountId and m.destinatario.id = :accountId)
            """)
    Page<Mensagem> findConversaComConta(
            @Param("accountId") Long accountId,
            @Param("outroAccountId") Long outroAccountId,
            Pageable pageable);
}
