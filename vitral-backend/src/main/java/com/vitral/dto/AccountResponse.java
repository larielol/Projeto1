package com.vitral.dto;

import com.vitral.enumerations.AccountType;

public record AccountResponse(
        Long id,
        String name,
        String username,
        String email,
        AccountType type,
        String fotoUrl,
        boolean emailVerificado,
        String cpf,
        String cep,
        String logradouro,
        String numero,
        String complemento,
        String bairro,
        String cidade,
        String estado) {

    public AccountResponse(Long id, String name, String username, String email, AccountType type, String fotoUrl) {
        this(id, name, username, email, type, fotoUrl, false, null, null, null, null, null, null, null, null);
    }
}
