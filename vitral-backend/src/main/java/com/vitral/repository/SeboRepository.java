package com.vitral.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.vitral.entity.Sebo;

public interface SeboRepository extends JpaRepository<Sebo, Long>, JpaSpecificationExecutor<Sebo> {

    Optional<Sebo> findByAccountId(Long accountId);

    Optional<Sebo> findByIdAndAccountAtivoTrueAndStatusVerificacao(
            Long id, com.vitral.enumerations.StatusVerificacaoSebo status);

    boolean existsByAccountId(Long accountId);

    boolean existsByCnpj(String cnpj);

    boolean existsByCnpjAndIdNot(String cnpj, Long id);

    Page<Sebo> findByStatusVerificacao(com.vitral.enumerations.StatusVerificacaoSebo status, Pageable pageable);

    Page<Sebo> findByAccountNameContainingIgnoreCase(String termo, Pageable pageable);

}
