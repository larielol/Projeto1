package com.vitral.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;

public record AtualizarPerfilRequest(
        @NotBlank @Size(min = 2, max = 255) String name,
        @Size(max = 500) String fotoUrl,
        @Size(max = 20) String cpf,
        @Size(max = 20) String cep,
        @Size(max = 255) String logradouro,
        @Size(max = 30) String numero,
        @Size(max = 255) String complemento,
        @Size(max = 120) String bairro,
        @Size(max = 120) String cidade,
        @Pattern(regexp = "(?i)[A-Z]{2}", message = "Estado deve ser uma UF com duas letras") String estado) {

    public AtualizarPerfilRequest(String name, String fotoUrl) {
        this(name, fotoUrl, null, null, null, null, null, null, null, null);
    }
}
