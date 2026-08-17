package com.vitral.dto;

import java.time.OffsetDateTime;
import com.vitral.enumerations.StatusConsultaCnpj;

public record ConsultaCnpjResponse(String cnpj, String razaoSocial, StatusConsultaCnpj status,
        OffsetDateTime consultadoEm, String mensagem) {
}
