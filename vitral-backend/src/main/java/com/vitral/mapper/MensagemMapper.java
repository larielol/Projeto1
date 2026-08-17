package com.vitral.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.vitral.dto.MensagemChatResponse;
import com.vitral.entity.Mensagem;

@Mapper(componentModel = "spring")
public interface MensagemMapper {

    @Mapping(target = "remetenteId", source = "remetente.id")
    @Mapping(target = "remetenteNome", source = "remetente.name")
    @Mapping(target = "destinatarioId", source = "destinatario.id")
    @Mapping(target = "destinatarioNome", source = "destinatario.name")
    MensagemChatResponse toResponse(Mensagem mensagem);
}
