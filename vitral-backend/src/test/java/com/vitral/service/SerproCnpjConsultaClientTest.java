package com.vitral.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.vitral.config.CnpjConsultaProperties;
import com.vitral.enumerations.StatusConsultaCnpj;

class SerproCnpjConsultaClientTest {

    @Test
    @DisplayName("Deve retornar indisponivel quando a consulta estiver desabilitada e o mock tambem estiver desligado")
    void retornaIndisponivelQuandoConsultaDesabilitadaSemMock() {
        var client = new SerproCnpjConsultaClient(new CnpjConsultaProperties(false, null, null, null, null, false));

        var result = client.consultar("12345678000195");

        assertThat(result.status()).isEqualTo(StatusConsultaCnpj.INDISPONIVEL);
        assertThat(result.mensagem()).contains("nao configurada");
    }

    @Test
    @DisplayName("Deve retornar indisponivel quando as credenciais estiverem incompletas e o mock estiver desligado")
    void retornaIndisponivelQuandoCredenciaisIncompletasSemMock() {
        var client = new SerproCnpjConsultaClient(
                new CnpjConsultaProperties(true, " ", "http://consulta", "id", "secret", false));

        var result = client.consultar("12345678000195");

        assertThat(result.status()).isEqualTo(StatusConsultaCnpj.INDISPONIVEL);
        assertThat(result.mensagem()).contains("incompletas");
    }

    @Test
    @DisplayName("Deve retornar indisponivel quando o provedor falhar, independente do mock")
    void retornaIndisponivelQuandoProviderFalha() {
        var client = new SerproCnpjConsultaClient(new CnpjConsultaProperties(true,
                "http://localhost:1/token", "http://localhost:1/cnpj/{cnpj}", "id", "secret", true));

        var result = client.consultar("12345678000195");

        assertThat(result.status()).isEqualTo(StatusConsultaCnpj.INDISPONIVEL);
        assertThat(result.mensagem()).contains("temporariamente");
    }

    @Test
    @DisplayName("Deve considerar o CNPJ ativo quando a consulta estiver desabilitada e o mock estiver ligado")
    void consideraCnpjAtivoQuandoConsultaDesabilitadaComMock() {
        var client = new SerproCnpjConsultaClient(new CnpjConsultaProperties(false, null, null, null, null, true));

        var result = client.consultar("12345678000195");

        assertThat(result.status()).isEqualTo(StatusConsultaCnpj.ATIVA);
        assertThat(result.razaoSocial()).isNull();
        assertThat(result.mensagem()).contains("Mock").contains("nao configurada");
    }

    @Test
    @DisplayName("Deve considerar o CNPJ ativo quando faltarem credenciais e o mock estiver ligado")
    void consideraCnpjAtivoQuandoCredenciaisIncompletasComMock() {
        var client = new SerproCnpjConsultaClient(
                new CnpjConsultaProperties(true, " ", "http://consulta", "id", "secret", true));

        var result = client.consultar("12345678000195");

        assertThat(result.status()).isEqualTo(StatusConsultaCnpj.ATIVA);
        assertThat(result.mensagem()).contains("Mock").contains("incompletas");
    }
}
