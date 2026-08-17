package com.vitral.dto;

import java.time.OffsetDateTime;

public record MensagemChatResponse(
        Long id,
        Long remetenteId,
        String remetenteNome,
        Long destinatarioId,
        String destinatarioNome,
        String conteudo,
        Boolean lida,
        OffsetDateTime createdAt) {
}
