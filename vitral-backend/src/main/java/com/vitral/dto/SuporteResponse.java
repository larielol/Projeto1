package com.vitral.dto;

import java.time.OffsetDateTime;

public record SuporteResponse(Long id, String assunto, String mensagem, String status, OffsetDateTime createdAt) {
}
