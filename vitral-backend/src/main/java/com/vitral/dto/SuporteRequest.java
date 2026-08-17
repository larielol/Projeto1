package com.vitral.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SuporteRequest(
        @NotBlank @Size(max = 255) String assunto,
        @NotBlank @Size(max = 2000) String mensagem) {
}
