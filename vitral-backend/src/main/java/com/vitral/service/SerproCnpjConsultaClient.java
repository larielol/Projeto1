package com.vitral.service;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.vitral.config.CnpjConsultaProperties;
import com.vitral.enumerations.StatusConsultaCnpj;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SerproCnpjConsultaClient implements CnpjConsultaClient {
    private final CnpjConsultaProperties properties;

    @Override
    public Resultado consultar(String cnpj) {
        if (!properties.enabled()) return semIntegracaoConfigurada("Consulta automatica nao configurada");
        if (vazio(properties.clientId()) || vazio(properties.clientSecret()) || vazio(properties.tokenUrl()) || vazio(properties.consultaUrl())) {
            return semIntegracaoConfigurada("Credenciais da consulta CNPJ incompletas");
        }
        try {
            var form = new LinkedMultiValueMap<String, String>();
            form.add("grant_type", "client_credentials");
            JsonNode token = RestClient.create().post().uri(properties.tokenUrl())
                    .headers(h -> h.setBasicAuth(properties.clientId(), properties.clientSecret()))
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED).body(form).retrieve().body(JsonNode.class);
            String accessToken = token == null ? null : token.path("access_token").asText(null);
            if (vazio(accessToken)) return indisponivel("Provedor nao retornou token de acesso");
            String url = properties.consultaUrl().replace("{cnpj}", cnpj);
            JsonNode body = RestClient.create().get().uri(url)
                    .headers(h -> h.setBearerAuth(accessToken)).retrieve().body(JsonNode.class);
            if (body == null) return indisponivel("Provedor retornou resposta vazia");
            String razaoSocial = primeiroTexto(body, "nomeEmpresarial", "razaoSocial", "nome");
            String codigo = body.path("situacaoCadastral").path("codigo").asText("");
            String descricao = body.path("situacaoCadastral").path("descricao").asText("");
            boolean ativa = "2".equals(codigo) || "02".equals(codigo) || "ATIVA".equalsIgnoreCase(descricao);
            return new Resultado(razaoSocial, ativa ? StatusConsultaCnpj.ATIVA : StatusConsultaCnpj.INATIVA,
                    ativa ? "CNPJ ativo na Receita Federal" : "CNPJ com situacao cadastral nao ativa");
        } catch (RuntimeException e) {
            return indisponivel("Consulta CNPJ temporariamente indisponivel");
        }
    }

    private String primeiroTexto(JsonNode body, String... campos) {
        for (String campo : campos) if (body.hasNonNull(campo) && !body.path(campo).asText().isBlank()) return body.path(campo).asText();
        return null;
    }

    /**
     * MOCK TEMPORARIO (ainda nao existe integracao SERPRO configurada em producao):
     * quando a consulta automatica nao esta disponivel (desabilitada ou sem credenciais),
     * considera o CNPJ ativo em vez de travar a aprovacao do sebo em INDISPONIVEL para
     * sempre. Defina app.cnpj-consulta.mock-auto-aprovar=false quando a integracao real
     * (SERPRO ou equivalente) estiver configurada, para que a consulta oficial volte a valer.
     */
    private Resultado semIntegracaoConfigurada(String motivo) {
        if (!properties.mockAutoAprovar()) {
            return indisponivel(motivo);
        }
        return new Resultado(null, StatusConsultaCnpj.ATIVA,
                "Mock: " + motivo + ". CNPJ considerado ativo ate a integracao oficial ser configurada.");
    }

    private Resultado indisponivel(String mensagem) { return new Resultado(null, StatusConsultaCnpj.INDISPONIVEL, mensagem); }
    private boolean vazio(String valor) { return valor == null || valor.isBlank(); }
}
