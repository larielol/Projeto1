package com.vitral.service;

import java.text.Normalizer;
import java.util.Locale;

import org.springframework.stereotype.Component;

@Component
public class NormalizadorPesquisaService {
    public String normalizar(String termo) {
        if (termo == null || termo.isBlank()) return null;
        String semAcentos = Normalizer.normalize(termo.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        String normalizado = semAcentos.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim();
        return normalizado.isBlank() ? null : normalizado.substring(0, Math.min(160, normalizado.length()));
    }
}
