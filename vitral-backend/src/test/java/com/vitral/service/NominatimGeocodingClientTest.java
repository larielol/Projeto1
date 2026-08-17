package com.vitral.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.vitral.config.GeocodingProperties;

class NominatimGeocodingClientTest {

    private static final String SEARCH_URL = "https://nominatim.openstreetmap.org/search?format=json&limit=1&countrycodes=br&q={q}";

    @Test
    @DisplayName("Deve retornar vazio quando a geocodificacao estiver desabilitada")
    void buscarCoordenadas_desabilitado_retornaVazio() {
        var client = new NominatimGeocodingClient(new GeocodingProperties(false, SEARCH_URL, "VitralApp/1.0"));

        var resultado = client.buscarCoordenadas("Campina Grande - PB, Brasil");

        assertThat(resultado).isEmpty();
    }

    @Test
    @DisplayName("Deve retornar vazio quando a consulta for nula ou em branco")
    void buscarCoordenadas_consultaEmBranco_retornaVazio() {
        var client = new NominatimGeocodingClient(new GeocodingProperties(true, SEARCH_URL, "VitralApp/1.0"));

        assertThat(client.buscarCoordenadas(null)).isEmpty();
        assertThat(client.buscarCoordenadas("  ")).isEmpty();
    }

    @Test
    @DisplayName("Deve retornar vazio quando o provedor estiver indisponivel")
    void buscarCoordenadas_providerFalha_retornaVazio() {
        var client = new NominatimGeocodingClient(
                new GeocodingProperties(true, "http://localhost:1/search?q={q}", "VitralApp/1.0"));

        var resultado = client.buscarCoordenadas("Campina Grande - PB, Brasil");

        assertThat(resultado).isEmpty();
    }
}
