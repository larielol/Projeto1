package com.vitral.entity;

import java.math.BigDecimal;

import com.vitral.enumerations.CondicaoProduto;
import com.vitral.enumerations.BookGenre;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "produto")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Produto extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sebo_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Sebo sebo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categoria_id")
    private Categoria categoria;

    @Enumerated(EnumType.STRING)
    @Column(name = "book_genre", length = 20)
    private BookGenre bookGenre;

    @Column(nullable = false, length = 255)
    private String titulo;

    @Column(length = 255)
    private String autor;

    @Column(length = 2000)
    private String descricao;

    @Column(name = "ano")
    private Integer ano;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal preco;

    @Column(nullable = false)
    @Builder.Default
    private Integer estoque = 1;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CondicaoProduto condicao;

    @Column(name = "foto_url", length = 500)
    private String fotoUrl;

    @Column(nullable = false)
    @Builder.Default
    private Boolean ativo = Boolean.TRUE;

    @Column(nullable = false)
    @Builder.Default
    private Boolean classico = Boolean.FALSE;

    @Column(nullable = false)
    @Builder.Default
    private Boolean lancamento = Boolean.FALSE;
}
