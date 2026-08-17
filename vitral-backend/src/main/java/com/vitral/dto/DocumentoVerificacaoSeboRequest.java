package com.vitral.dto;

import com.vitral.enumerations.TipoDocumentoSebo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record DocumentoVerificacaoSeboRequest(
        @NotNull TipoDocumentoSebo tipoDocumento,
        @NotBlank @Size(max = 500) String arquivoUrl) {
}
