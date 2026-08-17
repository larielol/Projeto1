package com.vitral.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.vitral.entity.TokenRecuperacaoSenha;

public interface TokenRecuperacaoSenhaRepository extends JpaRepository<TokenRecuperacaoSenha, Long> {
    Optional<TokenRecuperacaoSenha> findByToken(String token);
    void deleteByAccountId(Long accountId);
}
