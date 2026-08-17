package com.vitral.dto;

import java.math.BigDecimal;

import com.vitral.enumerations.CondicaoProduto;
import com.vitral.enumerations.BookGenre;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ProdutoRequest(
        @NotBlank @Size(max = 255) String titulo,
        @NotNull(message = "Categoria e obrigatoria") Long categoriaId,
        BookGenre bookGenre,
        @Size(max = 255) String autor,
        @Size(max = 2000) String descricao,
        @Min(0) @Max(2200) Integer ano,
        @NotNull @DecimalMin(value = "0.0", inclusive = true) @Digits(integer = 10, fraction = 2) BigDecimal preco,
        @Min(0) Integer estoque,
        @NotNull CondicaoProduto condicao,
        @Size(max = 500) String fotoUrl,
        Boolean classico,
        Boolean lancamento) {

    public ProdutoRequest(String titulo, Long categoriaId, String autor, String descricao,
            BigDecimal preco, Integer estoque, CondicaoProduto condicao, String fotoUrl) {
        this(titulo, categoriaId, null, autor, descricao, null, preco, estoque, condicao, fotoUrl, false, false);
    }

    public ProdutoRequest(String titulo, Long categoriaId, BookGenre bookGenre, String autor,
            String descricao, BigDecimal preco, Integer estoque, CondicaoProduto condicao, String fotoUrl) {
        this(titulo, categoriaId, bookGenre, autor, descricao, null, preco, estoque, condicao, fotoUrl, false, false);
    }

    public ProdutoRequest(String titulo, Long categoriaId, BookGenre bookGenre, String autor,
            String descricao, BigDecimal preco, Integer estoque, CondicaoProduto condicao, String fotoUrl,
            Boolean classico, Boolean lancamento) {
        this(titulo, categoriaId, bookGenre, autor, descricao, null, preco, estoque, condicao, fotoUrl, classico,
                lancamento);
    }
}
