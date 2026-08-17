package com.vitral.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import com.vitral.dto.SeboResponse;
import com.vitral.entity.Sebo;

@Mapper(componentModel = "spring")
public interface SeboMapper {

    // toResponse() e usado apenas pelo dono do sebo (criar/atualizar/"/me") ou por um ADMIN
    // revisando documentos: ambos precisam ver o CNPJ completo, sem mascara.
    @Mapping(target = "accountId", source = "account.id")
    @Mapping(target = "nome", source = "account.name")
    @Mapping(target = "email", source = "account.email")
    @Mapping(target = "distanciaKm", ignore = true)
    SeboResponse toResponse(Sebo sebo);

    @Mapping(target = "accountId", source = "sebo.account.id")
    @Mapping(target = "nome", source = "sebo.account.name")
    @Mapping(target = "email", source = "sebo.account.email")
    @Mapping(target = "distanciaKm", source = "distanciaKm")
    SeboResponse toResponse(Sebo sebo, Double distanciaKm);

    @Mapping(target = "accountId", source = "account.id")
    @Mapping(target = "nome", source = "account.name")
    @Mapping(target = "email", ignore = true)
    @Mapping(target = "telefone", ignore = true)
    @Mapping(target = "cnpj", source = "cnpj", qualifiedByName = "maskCnpj")
    @Mapping(target = "cep", ignore = true)
    @Mapping(target = "logradouro", ignore = true)
    @Mapping(target = "cidade", ignore = true)
    @Mapping(target = "uf", ignore = true)
    @Mapping(target = "horarioFuncionamento", ignore = true)
    @Mapping(target = "motivoRejeicao", ignore = true)
    @Mapping(target = "razaoSocialReceita", ignore = true)
    @Mapping(target = "mensagemConsultaCnpj", ignore = true)
    @Mapping(target = "distanciaKm", ignore = true)
    SeboResponse toPublicResponse(Sebo sebo);

    @Mapping(target = "accountId", source = "sebo.account.id")
    @Mapping(target = "nome", source = "sebo.account.name")
    @Mapping(target = "email", ignore = true)
    @Mapping(target = "telefone", ignore = true)
    @Mapping(target = "cnpj", source = "sebo.cnpj", qualifiedByName = "maskCnpj")
    @Mapping(target = "cep", ignore = true)
    @Mapping(target = "logradouro", ignore = true)
    @Mapping(target = "cidade", ignore = true)
    @Mapping(target = "uf", ignore = true)
    @Mapping(target = "horarioFuncionamento", ignore = true)
    @Mapping(target = "motivoRejeicao", ignore = true)
    @Mapping(target = "razaoSocialReceita", ignore = true)
    @Mapping(target = "mensagemConsultaCnpj", ignore = true)
    @Mapping(target = "distanciaKm", source = "distanciaKm")
    SeboResponse toPublicResponse(Sebo sebo, Double distanciaKm);

    @Named("maskCnpj")
    default String maskCnpj(String cnpj) {
        return com.vitral.util.DocumentoFiscalUtils.mascararCnpj(cnpj);
    }
}
