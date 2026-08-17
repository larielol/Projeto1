package com.vitral.dto;

import java.time.OffsetDateTime;

public record MensagemConversaResponse(
        Long id,
        Long remetenteId,
        String remetenteNome,
        Long destinatarioId,
        String destinatarioNome,
        String conteudo,
        Boolean lida,
        OffsetDateTime createdAt) {
}
