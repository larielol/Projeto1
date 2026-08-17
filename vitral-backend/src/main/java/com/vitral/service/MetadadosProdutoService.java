package com.vitral.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vitral.config.MetadadosProdutoProperties;
import com.vitral.dto.SugestaoProdutoResponse;
import com.vitral.entity.Categoria;
import com.vitral.exception.BusinessException;
import com.vitral.repository.CategoriaRepository;
import com.vitral.service.CatalogoExternoClient.MetadadoProduto;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MetadadosProdutoService {

    private static final int TAMANHO_MINIMO_TERMO = 3;
    private static final int LIMITE_TITULO = 255;
    private static final int LIMITE_AUTOR = 255;
    private static final int LIMITE_RESUMO = 320;
    private static final int TAMANHO_MINIMO_FRASE = 40;
    private static final String RETICENCIAS = "...";
    private static final int LIMITE_FOTO_URL = 500;

    private final OpenLibraryClient openLibraryClient;
    private final MusicBrainzClient musicBrainzClient;
    private final WikipediaClient wikipediaClient;
    private final CategoriaRepository categoriaRepository;
    private final MetadadosProdutoProperties properties;

    @Transactional(readOnly = true)
    public List<SugestaoProdutoResponse> sugerir(String termo) {
        String normalizado = termo == null ? "" : termo.trim();
        if (normalizado.length() < TAMANHO_MINIMO_TERMO) {
            throw new BusinessException("Informe ao menos 3 caracteres para buscar sugestoes",
                    HttpStatus.BAD_REQUEST);
        }
        if (!properties.enabled()) {
            return List.of();
        }

        List<MetadadoProduto> encontrados = new ArrayList<>();
        encontrados.addAll(openLibraryClient.buscar(normalizado));
        encontrados.addAll(musicBrainzClient.buscar(normalizado));

        Map<String, Categoria> categoriasPorSlug = carregarCategorias();
        Map<String, String> resumosBuscados = new HashMap<>();
        return encontrados.stream()
                .sorted(Comparator.comparingInt(this::completude).reversed())
                .map(metadado -> toResponse(metadado, categoriasPorSlug, resumosBuscados))
                .toList();
    }

    private String resumo(MetadadoProduto metadado, Map<String, String> resumosBuscados) {
        if (CatalogoExternoClient.FONTE_OPEN_LIBRARY.equals(metadado.fonte()) && metadado.descricao() != null) {
            return encurtar(metadado.descricao());
        }
        String chave = metadado.titulo() + "|" + metadado.categoriaSlug();
        String daWikipedia = resumosBuscados.computeIfAbsent(chave, ignorado -> wikipediaClient
                .buscarResumo(metadado.titulo(), metadado.categoriaSlug())
                .orElse(""));
        return daWikipedia.isBlank() ? encurtar(metadado.descricao()) : encurtar(daWikipedia);
    }

    private String encurtar(String texto) {
        if (texto == null) {
            return null;
        }
        String limpo = texto.replace("**", "").replaceAll("\\s+", " ").trim();
        if (limpo.length() <= LIMITE_RESUMO) {
            return limpo;
        }
        int fimDaFrase = limpo.substring(0, LIMITE_RESUMO).lastIndexOf(". ");
        if (fimDaFrase >= TAMANHO_MINIMO_FRASE) {
            return limpo.substring(0, fimDaFrase + 1);
        }
        return limpo.substring(0, LIMITE_RESUMO - RETICENCIAS.length()).trim() + RETICENCIAS;
    }

    private Map<String, Categoria> carregarCategorias() {
        return categoriaRepository.findAll().stream()
                .filter(categoria -> CategoriaService.SLUGS_PERMITIDOS.contains(categoria.getSlug()))
                .collect(java.util.stream.Collectors.toMap(Categoria::getSlug, categoria -> categoria,
                        (primeira, segunda) -> primeira));
    }

    private SugestaoProdutoResponse toResponse(MetadadoProduto metadado, Map<String, Categoria> categoriasPorSlug,
            Map<String, String> resumosBuscados) {
        Categoria categoria = metadado.categoriaSlug() == null
                ? null
                : categoriasPorSlug.get(metadado.categoriaSlug());
        return new SugestaoProdutoResponse(
                truncar(metadado.titulo(), LIMITE_TITULO),
                truncar(metadado.autor(), LIMITE_AUTOR),
                metadado.ano(),
                resumo(metadado, resumosBuscados),
                categoria == null ? null : categoria.getId(),
                categoria == null ? null : categoria.getSlug(),
                categoria == null ? null : categoria.getNome(),
                truncar(metadado.fotoUrl(), LIMITE_FOTO_URL),
                metadado.fonte());
    }

    private int completude(MetadadoProduto metadado) {
        return (int) Stream.of(metadado.autor(), metadado.ano(), metadado.descricao(),
                metadado.categoriaSlug(), metadado.fotoUrl())
                .filter(Objects::nonNull)
                .count();
    }

    private String truncar(String valor, int limite) {
        if (valor == null) {
            return null;
        }
        return valor.length() <= limite ? valor : valor.substring(0, limite);
    }
}
