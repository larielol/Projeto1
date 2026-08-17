package com.vitral.service;

import com.vitral.enumerations.StatusConsultaCnpj;

public interface CnpjConsultaClient {
    Resultado consultar(String cnpj);

    record Resultado(String razaoSocial, StatusConsultaCnpj status, String mensagem) {}
}
