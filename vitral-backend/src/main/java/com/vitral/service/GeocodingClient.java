package com.vitral.service;

import java.util.Optional;

public interface GeocodingClient {

    Optional<Coordenadas> buscarCoordenadas(String query);

    record Coordenadas(double latitude, double longitude) {
    }
}
