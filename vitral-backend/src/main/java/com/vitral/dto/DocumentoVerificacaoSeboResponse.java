package com.vitral.dto;

import java.time.OffsetDateTime;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.vitral.enumerations.StatusDocumentoSebo;
import com.vitral.enumerations.TipoDocumentoSebo;

public record DocumentoVerificacaoSeboResponse(
        Long id,
        @JsonProperty("tipo") TipoDocumentoSebo tipoDocumento,
        String arquivoUrl,
        String nomeArquivo,
        String contentType,
        Long tamanhoBytes,
        StatusDocumentoSebo status,
        OffsetDateTime enviadoEm,
        OffsetDateTime analisadoEm, Long analisadoPorId, String motivoRejeicao) {

    public DocumentoVerificacaoSeboResponse(Long id, TipoDocumentoSebo tipoDocumento,
            String arquivoUrl, StatusDocumentoSebo status, OffsetDateTime enviadoEm,
            OffsetDateTime analisadoEm, Long analisadoPorId, String motivoRejeicao) {
        this(id, tipoDocumento, arquivoUrl, null, null, null, status, enviadoEm, analisadoEm, analisadoPorId,
                motivoRejeicao);
    }
}
