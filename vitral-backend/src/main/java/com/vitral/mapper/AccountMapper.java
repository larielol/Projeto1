package com.vitral.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import com.vitral.dto.AccountResponse;
import com.vitral.entity.Account;

@Mapper(componentModel = "spring")
public interface AccountMapper {

    @Mapping(target = "cpf", source = "cpf", qualifiedByName = "maskCpf")
    AccountResponse toResponse(Account account);

    @Named("maskCpf")
    default String maskCpf(String cpf) {
        return com.vitral.util.DocumentoFiscalUtils.mascararCpf(cpf);
    }
}
