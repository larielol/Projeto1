package com.vitral.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategoriaRequest(
        @NotBlank @Size(max = 120) String nome,
        @Size(max = 500) String descricao) {
}
