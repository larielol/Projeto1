package com.vitral.dto;

import java.time.OffsetDateTime;
import com.vitral.enumerations.StatusVerificacaoSebo;

public record AuditoriaVerificacaoSeboResponse(Long id, Long analisadoPorId,
        StatusVerificacaoSebo statusAnterior, StatusVerificacaoSebo novoStatus,
        String motivo, OffsetDateTime criadoEm) {
}
