package com.vitral.service;

import java.time.OffsetDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.vitral.dto.ConsultaCnpjResponse;
import com.vitral.entity.Account;
import com.vitral.entity.Sebo;
import com.vitral.exception.ResourceNotFoundException;
import com.vitral.repository.SeboRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CnpjConsultaService {
    private final CnpjConsultaClient client;
    private final SeboRepository seboRepository;

    @Transactional
    public ConsultaCnpjResponse consultarMeuSebo(Account account) {
        Sebo sebo = seboRepository.findByAccountId(account.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Sebo nao encontrado para esta conta"));
        return consultar(sebo);
    }

    @Transactional
    public ConsultaCnpjResponse consultar(Sebo sebo) {
        var resultado = client.consultar(sebo.getCnpj());
        OffsetDateTime agora = OffsetDateTime.now();
        sebo.setRazaoSocialReceita(resultado.razaoSocial());
        sebo.setStatusConsultaCnpj(resultado.status());
        sebo.setCnpjConsultadoEm(agora);
        sebo.setMensagemConsultaCnpj(resultado.mensagem());
        return new ConsultaCnpjResponse(sebo.getCnpj(), resultado.razaoSocial(), resultado.status(), agora, resultado.mensagem());
    }
}
