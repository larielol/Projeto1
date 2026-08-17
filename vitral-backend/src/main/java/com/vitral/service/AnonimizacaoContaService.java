package com.vitral.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.vitral.entity.Account;
import com.vitral.enumerations.StatusVerificacaoSebo;
import com.vitral.enumerations.StatusConsultaCnpj;
import com.vitral.repository.ProdutoRepository;
import com.vitral.repository.SeboRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AnonimizacaoContaService {
    private final SeboRepository seboRepository;
    private final ProdutoRepository produtoRepository;
    private final com.vitral.repository.RecomendacaoEventoRepository recomendacaoEventoRepository;

    @Transactional
    public void anonimizarUsuario(Account account) {
        recomendacaoEventoRepository.deleteByAccountId(account.getId());
        anonimizarConta(account);
    }

    @Transactional
    public void anonimizarSebo(Account account) {
        seboRepository.findByAccountId(account.getId()).ifPresent(sebo -> {
            produtoRepository.desativarCatalogoDoSebo(sebo.getId());
            sebo.setDescricao("Sebo desativado");
            sebo.setTelefone(null);
            sebo.setCnpj(null);
            sebo.setFotoUrl(null);
            sebo.setVerificadoEm(null);
            sebo.setMotivoRejeicao(null);
            sebo.setRazaoSocialReceita(null);
            sebo.setCnpjConsultadoEm(null);
            sebo.setMensagemConsultaCnpj(null);
            sebo.setStatusConsultaCnpj(StatusConsultaCnpj.NAO_CONSULTADO);
            sebo.setStatusVerificacao(StatusVerificacaoSebo.REJEITADO);
            sebo.setConfirmado(false);
        });
        anonimizarConta(account);
    }

    private void anonimizarConta(Account account) {
        account.setAtivo(false);
        account.setName("Conta excluida");
        account.setUsername("conta-excluida-" + account.getId());
        account.setEmail("excluida-" + account.getId() + "@invalid.local");
        account.setPasswordHash("CONTA_DESATIVADA");
        account.setFotoUrl(null);
        account.setAuthVersion((account.getAuthVersion() == null ? 0 : account.getAuthVersion()) + 1);
    }
}
