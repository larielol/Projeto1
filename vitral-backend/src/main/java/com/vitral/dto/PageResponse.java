package com.vitral.dto;

import java.util.List;

import org.springframework.data.domain.Page;

/**
 * DTO genérico de paginação que estabiliza o contrato JSON das respostas paginadas,
 * evitando a serialização direta de {@link org.springframework.data.domain.PageImpl}.
 *
 * <p>Campos alinhados com o tipo {@code Page<T>} esperado pelo frontend (common.ts).</p>
 */
public record PageResponse<T>(
        List<T> content,
        long totalElements,
        int totalPages,
        int number,
        int size,
        boolean first,
        boolean last
) {
    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.getNumber(),
                page.getSize(),
                page.isFirst(),
                page.isLast()
        );
    }
}
