package com.vitral.service;

import java.text.Normalizer;
import java.util.Locale;
import java.util.List;
import java.util.Set;
import org.springframework.http.HttpStatus;
import com.vitral.exception.BusinessException;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vitral.dto.CategoriaRequest;
import com.vitral.dto.CategoriaResponse;
import com.vitral.entity.Categoria;
import com.vitral.exception.ConflictException;
import com.vitral.exception.ResourceNotFoundException;
import com.vitral.repository.CategoriaRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CategoriaService {

    public static final List<String> SLUGS_PERMITIDOS = List.of("livros", "cds", "vinis", "hqs-mangas");
    private static final Set<String> SLUGS_PERMITIDOS_SET = Set.copyOf(SLUGS_PERMITIDOS);

    private final CategoriaRepository categoriaRepository;

    @Transactional
    public CategoriaResponse criar(CategoriaRequest request) {
        String slug = gerarSlug(request.nome());
        if (!SLUGS_PERMITIDOS_SET.contains(slug)) {
            throw new BusinessException("Categoria invalida. Use Livros, CDs, Vinis ou HQs / Mangas", HttpStatus.BAD_REQUEST);
        }
        if (categoriaRepository.existsBySlug(slug)) {
            throw new ConflictException("Categoria ja cadastrada");
        }
        Categoria categoria = Categoria.builder()
                .nome(request.nome().trim())
                .slug(slug)
                .descricao(request.descricao())
                .build();
        return toResponse(categoriaRepository.save(categoria));
    }

    @Transactional(readOnly = true)
    public Page<CategoriaResponse> listar(Pageable pageable) {
        return categoriaRepository.findPermitidas(SLUGS_PERMITIDOS, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Categoria buscarEntidade(Long id) {
        return categoriaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Categoria nao encontrada"));
    }

    @Transactional(readOnly = true)
    public Categoria buscarPermitida(Long id) {
        Categoria categoria = categoriaRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Categoria invalida", HttpStatus.BAD_REQUEST));
        if (!SLUGS_PERMITIDOS_SET.contains(categoria.getSlug())) {
            throw new BusinessException("Categoria invalida. Use Livros, CDs, Vinis ou HQs / Mangas", HttpStatus.BAD_REQUEST);
        }
        return categoria;
    }

    private CategoriaResponse toResponse(Categoria categoria) {
        return new CategoriaResponse(
                categoria.getId(),
                categoria.getNome(),
                categoria.getSlug(),
                categoria.getDescricao());
    }

    private String gerarSlug(String nome) {
        String semAcentos = Normalizer.normalize(nome.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return semAcentos.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
    }
}
