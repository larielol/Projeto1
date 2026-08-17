package com.vitral.mapper;

import com.vitral.dto.AccountResponse;
import com.vitral.entity.Account;
import com.vitral.enumerations.AccountType;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-08-13T08:51:42-0300",
    comments = "version: 1.6.3, compiler: Eclipse JDT (IDE) 3.46.100.v20260624-0231, environment: Java 21.0.11 (Eclipse Adoptium)"
)
@Component
public class AccountMapperImpl implements AccountMapper {

    @Override
    public AccountResponse toResponse(Account account) {
        if ( account == null ) {
            return null;
        }

        String cpf = null;
        Long id = null;
        String name = null;
        String username = null;
        String email = null;
        AccountType type = null;
        String fotoUrl = null;
        boolean emailVerificado = false;
        String cep = null;
        String logradouro = null;
        String numero = null;
        String complemento = null;
        String bairro = null;
        String cidade = null;
        String estado = null;

        cpf = maskCpf( account.getCpf() );
        id = account.getId();
        name = account.getName();
        username = account.getUsername();
        email = account.getEmail();
        type = account.getType();
        fotoUrl = account.getFotoUrl();
        emailVerificado = account.isEmailVerificado();
        cep = account.getCep();
        logradouro = account.getLogradouro();
        numero = account.getNumero();
        complemento = account.getComplemento();
        bairro = account.getBairro();
        cidade = account.getCidade();
        estado = account.getEstado();

        AccountResponse accountResponse = new AccountResponse( id, name, username, email, type, fotoUrl, emailVerificado, cpf, cep, logradouro, numero, complemento, bairro, cidade, estado );

        return accountResponse;
    }
}
