package com.vitral.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import com.vitral.enumerations.CondicaoProduto;
import com.vitral.enumerations.BookGenre;

public record ProdutoResponse(
        Long id,
        Long seboId,
        String seboNome,
        Long categoriaId,
        String categoriaNome,
        BookGenre bookGenre,
        String titulo,
        String autor,
        String descricao,
        Integer ano,
        BigDecimal preco,
        BigDecimal precoPromocional,
        Integer estoque,
        CondicaoProduto condicao,
        String fotoUrl,
        Boolean ativo,
        Boolean disponivel,
        Boolean classico,
        Boolean lancamento,
        OffsetDateTime dataPublicacao) {

    public ProdutoResponse(
            Long id,
            Long seboId,
            Long categoriaId,
            String categoriaNome,
            String titulo,
            String autor,
            String descricao,
            BigDecimal preco,
            BigDecimal precoPromocional,
            Integer estoque,
            CondicaoProduto condicao,
            String fotoUrl,
            Boolean ativo) {
        this(id, seboId, null, categoriaId, categoriaNome, null, titulo, autor, descricao, null,
                preco, precoPromocional, estoque, condicao, fotoUrl, ativo,
                Boolean.TRUE.equals(ativo) && estoque != null && estoque > 0,
                false, false, null);
    }
}
