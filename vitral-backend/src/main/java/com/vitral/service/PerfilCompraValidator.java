package com.vitral.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.vitral.entity.Account;
import com.vitral.exception.BusinessException;
import com.vitral.util.DocumentoFiscalUtils;

@Component
public class PerfilCompraValidator {
    private static final Set<String> UFS = Set.of("AC", "AL", "AP", "AM", "BA", "CE", "DF", "ES", "GO",
            "MA", "MT", "MS", "MG", "PA", "PB", "PR", "PE", "PI", "RJ", "RN", "RS", "RO", "RR", "SC",
            "SP", "SE", "TO");

    public void validar(Account account) {
        List<String> fields = new ArrayList<>();
        if (!DocumentoFiscalUtils.cpfValido(account.getCpf())) fields.add("cpf");
        if (account.getCep() == null || !account.getCep().matches("\\d{8}")) fields.add("cep");
        validarTexto(account.getLogradouro(), "logradouro", fields);
        validarTexto(account.getNumero(), "numero", fields);
        validarTexto(account.getBairro(), "bairro", fields);
        validarTexto(account.getCidade(), "cidade", fields);
        if (account.getEstado() == null || !UFS.contains(account.getEstado().toUpperCase())) fields.add("estado");
        if (!fields.isEmpty()) {
            throw new BusinessException("Complete CPF e endereco antes de finalizar a compra",
                    HttpStatus.UNPROCESSABLE_ENTITY, "PROFILE_INCOMPLETE_FOR_PURCHASE", fields);
        }
    }

    private void validarTexto(String valor, String campo, List<String> fields) {
        if (valor == null || valor.isBlank()) fields.add(campo);
    }
}
