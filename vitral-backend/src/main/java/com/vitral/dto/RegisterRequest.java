package com.vitral.dto;

import com.vitral.enumerations.AccountType;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Size(min = 2, max = 255) String name,
        @NotBlank @Pattern(
                regexp = "^[a-z0-9._-]{3,30}$",
                message = "Usuario deve ter de 3 a 30 caracteres, apenas letras minusculas, numeros, ponto, hifen ou underline, sem espacos")
        String username,
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8, max = 100) String password,
        @NotNull AccountType type) {
}
