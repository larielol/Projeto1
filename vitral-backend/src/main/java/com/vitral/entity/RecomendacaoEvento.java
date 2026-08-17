package com.vitral.entity;

import com.vitral.enumerations.TipoEventoRecomendacao;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "recomendacao_evento")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecomendacaoEvento extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TipoEventoRecomendacao tipo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "produto_id")
    private Produto produto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categoria_id")
    private Categoria categoria;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sebo_id")
    private Sebo sebo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pedido_id")
    private Pedido pedido;

    @Column(name = "tipo_produto", length = 40)
    private String tipoProduto;

    @Column(length = 40)
    private String genero;

    @Column(name = "autor_artista", length = 255)
    private String autorArtista;

    @Column(name = "faixa_preco", length = 30)
    private String faixaPreco;

    @Column(name = "termo_pesquisa", length = 160)
    private String termoPesquisa;
}
