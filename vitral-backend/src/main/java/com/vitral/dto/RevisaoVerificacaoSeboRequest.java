package com.vitral.dto;

import jakarta.validation.constraints.Size;

public record RevisaoVerificacaoSeboRequest(
        @Size(max = 500) String motivo) {
}
