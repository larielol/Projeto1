package com.vitral.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vitral.dto.SuporteRequest;
import com.vitral.dto.SuporteResponse;
import com.vitral.entity.Account;
import com.vitral.entity.Suporte;
import com.vitral.repository.SuporteRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SuporteService {

    private static final Logger log = LoggerFactory.getLogger(SuporteService.class);

    private final SuporteRepository suporteRepository;
    private final EmailService emailService;

    @Transactional
    public SuporteResponse enviar(Account remetente, SuporteRequest request) {
        Suporte suporte = Suporte.builder()
                .assunto(request.assunto())
                .mensagem(request.mensagem())
                .remetente(remetente)
                .build();
        Suporte salvo = suporteRepository.save(suporte);
        notificarEquipe(salvo, remetente);
        return toResponse(salvo);
    }

    private void notificarEquipe(Suporte suporte, Account remetente) {
        try {
            emailService.enviarMensagemSuporte(
                    remetente.getName(),
                    remetente.getEmail(),
                    suporte.getAssunto(),
                    suporte.getMensagem());
        } catch (RuntimeException exception) {
            log.warn("Falha ao notificar a equipe sobre o chamado de suporte {}", suporte.getId(), exception);
        }
    }

    private SuporteResponse toResponse(Suporte suporte) {
        return new SuporteResponse(
                suporte.getId(),
                suporte.getAssunto(),
                suporte.getMensagem(),
                suporte.getStatus().name(),
                suporte.getCreatedAt());
    }
}
