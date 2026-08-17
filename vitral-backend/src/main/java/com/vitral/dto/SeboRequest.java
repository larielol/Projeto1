package com.vitral.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SeboRequest(
        @Size(max = 2000) String descricao,
        @Size(max = 20) String telefone,
        @NotBlank @Size(max = 20) String cnpj,
        @Size(max = 500) String fotoUrl,
        @NotBlank(message = "Informe o CEP do sebo") @Size(max = 20) String cep,
        @NotBlank(message = "Informe o logradouro do sebo") @Size(max = 255) String logradouro,
        @NotBlank(message = "Informe a cidade do sebo") @Size(max = 120) String cidade,
        @NotBlank(message = "Informe a UF do sebo")
        @Pattern(regexp = "^[A-Za-z]{2}$", message = "UF deve ter duas letras") String uf,
        @Size(max = 255) String horarioFuncionamento) {

    public SeboRequest(String descricao, String telefone, String cnpj, String fotoUrl,
            String cep, String logradouro, String cidade, String uf) {
        this(descricao, telefone, cnpj, fotoUrl, cep, logradouro, cidade, uf, null);
    }
}
