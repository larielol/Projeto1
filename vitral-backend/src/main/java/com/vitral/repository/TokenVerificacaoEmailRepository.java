package com.vitral.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.vitral.entity.TokenVerificacaoEmail;

public interface TokenVerificacaoEmailRepository extends JpaRepository<TokenVerificacaoEmail, Long> {

    Optional<TokenVerificacaoEmail> findByToken(String token);

    void deleteByAccountId(Long accountId);
}
