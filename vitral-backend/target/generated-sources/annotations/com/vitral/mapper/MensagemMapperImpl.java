package com.vitral.mapper;

import com.vitral.dto.MensagemChatResponse;
import com.vitral.entity.Account;
import com.vitral.entity.Mensagem;
import java.time.OffsetDateTime;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-08-13T08:51:42-0300",
    comments = "version: 1.6.3, compiler: Eclipse JDT (IDE) 3.46.100.v20260624-0231, environment: Java 21.0.11 (Eclipse Adoptium)"
)
@Component
public class MensagemMapperImpl implements MensagemMapper {

    @Override
    public MensagemChatResponse toResponse(Mensagem mensagem) {
        if ( mensagem == null ) {
            return null;
        }

        Long remetenteId = null;
        String remetenteNome = null;
        Long destinatarioId = null;
        String destinatarioNome = null;
        Long id = null;
        String conteudo = null;
        Boolean lida = null;
        OffsetDateTime createdAt = null;

        remetenteId = mensagemRemetenteId( mensagem );
        remetenteNome = mensagemRemetenteName( mensagem );
        destinatarioId = mensagemDestinatarioId( mensagem );
        destinatarioNome = mensagemDestinatarioName( mensagem );
        id = mensagem.getId();
        conteudo = mensagem.getConteudo();
        lida = mensagem.getLida();
        createdAt = mensagem.getCreatedAt();

        MensagemChatResponse mensagemChatResponse = new MensagemChatResponse( id, remetenteId, remetenteNome, destinatarioId, destinatarioNome, conteudo, lida, createdAt );

        return mensagemChatResponse;
    }

    private Long mensagemRemetenteId(Mensagem mensagem) {
        Account remetente = mensagem.getRemetente();
        if ( remetente == null ) {
            return null;
        }
        return remetente.getId();
    }

    private String mensagemRemetenteName(Mensagem mensagem) {
        Account remetente = mensagem.getRemetente();
        if ( remetente == null ) {
            return null;
        }
        return remetente.getName();
    }

    private Long mensagemDestinatarioId(Mensagem mensagem) {
        Account destinatario = mensagem.getDestinatario();
        if ( destinatario == null ) {
            return null;
        }
        return destinatario.getId();
    }

    private String mensagemDestinatarioName(Mensagem mensagem) {
        Account destinatario = mensagem.getDestinatario();
        if ( destinatario == null ) {
            return null;
        }
        return destinatario.getName();
    }
}
