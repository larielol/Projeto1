package com.vitral.service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.databind.JsonNode;
import com.vitral.config.GeocodingProperties;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class NominatimGeocodingClient implements GeocodingClient {

    private final GeocodingProperties properties;

    @Override
    public Optional<Coordenadas> buscarCoordenadas(String query) {
        if (!properties.enabled() || vazio(query)) {
            return Optional.empty();
        }
        try {
            String urlCodificada = properties.searchUrl()
                    .replace("{q}", URLEncoder.encode(query, StandardCharsets.UTF_8));
            JsonNode body = RestClient.create().get().uri(urlCodificada)
                    .header("User-Agent", properties.userAgent())
                    .retrieve().body(JsonNode.class);
            if (body == null || !body.isArray() || body.isEmpty()) {
                return Optional.empty();
            }
            JsonNode primeiro = body.get(0);
            String latitudeTexto = primeiro.path("lat").asText(null);
            String longitudeTexto = primeiro.path("lon").asText(null);
            if (vazio(latitudeTexto) || vazio(longitudeTexto)) {
                return Optional.empty();
            }
            return Optional.of(new Coordenadas(
                    Double.parseDouble(latitudeTexto),
                    Double.parseDouble(longitudeTexto)));
        } catch (RuntimeException exception) {
            return Optional.empty();
        }
    }

    private boolean vazio(String texto) {
        return texto == null || texto.isBlank();
    }
}
