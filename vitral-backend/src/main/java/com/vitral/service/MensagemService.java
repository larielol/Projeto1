package com.vitral.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vitral.dto.MensagemConversaResponse;
import com.vitral.dto.MensagemRequest;
import com.vitral.entity.Account;
import com.vitral.entity.Mensagem;
import com.vitral.exception.BusinessException;
import com.vitral.exception.ResourceNotFoundException;
import com.vitral.repository.AccountRepository;
import com.vitral.repository.MensagemRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MensagemService {

    private final MensagemRepository mensagemRepository;
    private final AccountRepository accountRepository;

    @Transactional
    public MensagemConversaResponse enviar(Account remetente, MensagemRequest request) {
        if (remetente.getId().equals(request.destinatarioId())) {
            throw new BusinessException("Nao e possivel enviar mensagem para si mesmo", HttpStatus.BAD_REQUEST);
        }
        Account destinatario = accountRepository.findById(request.destinatarioId())
                .orElseThrow(() -> new ResourceNotFoundException("Destinatario nao encontrado"));
        Mensagem mensagem = Mensagem.builder()
                .remetente(remetente)
                .destinatario(destinatario)
                .conteudo(request.conteudo())
                .build();
        return toResponse(mensagemRepository.save(mensagem));
    }

    @Transactional(readOnly = true)
    public Page<MensagemConversaResponse> listarConversas(Account account, Pageable pageable) {
        return mensagemRepository.findConversasDaConta(account.getId(), pageable).map(this::toResponse);
    }

    @Transactional
    public Page<MensagemConversaResponse> listarConversaCom(Account account, Long outroAccountId, Pageable pageable) {
        Page<Mensagem> mensagens = mensagemRepository.findConversaComConta(account.getId(), outroAccountId, pageable);
        mensagens.stream()
                .filter(m -> m.getDestinatario().getId().equals(account.getId()))
                .forEach(m -> m.setLida(Boolean.TRUE));
        return mensagens.map(this::toResponse);
    }

    private MensagemConversaResponse toResponse(Mensagem mensagem) {
        return new MensagemConversaResponse(
                mensagem.getId(),
                mensagem.getRemetente().getId(),
                mensagem.getRemetente().getName(),
                mensagem.getDestinatario().getId(),
                mensagem.getDestinatario().getName(),
                mensagem.getConteudo(),
                mensagem.getLida(),
                mensagem.getCreatedAt());
    }
}
