package com.vitral.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vitral.dto.MensagemResponse;
import com.vitral.entity.Account;
import com.vitral.repository.RecomendacaoEventoRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RecomendacaoHistoricoService {
    private final RecomendacaoEventoRepository repository;

    @Transactional
    public MensagemResponse limpar(Account account) {
        repository.deleteByAccountId(account.getId());
        return new MensagemResponse("Historico de recomendacoes removido com sucesso");
    }
}
